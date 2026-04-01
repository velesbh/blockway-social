package space.blockway.social.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import space.blockway.social.velocity.api.RestApiServer;
import space.blockway.social.velocity.commands.AdminCommand;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.*;
import space.blockway.social.velocity.listeners.PlayerConnectionListener;
import space.blockway.social.velocity.managers.*;
import space.blockway.social.velocity.messaging.MessageSender;
import space.blockway.social.velocity.messaging.VelocityMessageHandler;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for the Blockway Social Velocity proxy plugin.
 *
 * <p>Initialisation order:
 * <ol>
 *   <li>Load config from data directory</li>
 *   <li>Initialise database (HikariCP + schema migrations)</li>
 *   <li>Construct repositories</li>
 *   <li>Construct managers</li>
 *   <li>Register plugin messaging channel + event listeners</li>
 *   <li>Register admin command</li>
 *   <li>Start REST API server (if enabled in config)</li>
 *   <li>Schedule periodic maintenance tasks</li>
 * </ol>
 *
 * @author Enzonic LLC — blockway.space
 */
@Plugin(
        id = "blockway-social",
        name = "Blockway Social",
        version = "1.0.0",
        description = "Friends, parties, cross-server chat, account linking, and REST API for the Blockway network.",
        url = "https://blockway.space",
        authors = {"Enzonic"}
)
public class BlockwaySocialVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    // Core components
    private VelocityConfig config;
    private DatabaseManager databaseManager;

    // Repositories
    private PlayerRepository playerRepository;
    private FriendsRepository friendsRepository;
    private PartyRepository partyRepository;
    private ChatRepository chatRepository;
    private LinkRepository linkRepository;
    private ApiKeyRepository apiKeyRepository;
    private PlayerSettingsRepository playerSettingsRepository;

    // Managers
    private PresenceManager presenceManager;
    private FriendsManager friendsManager;
    private PartyManager partyManager;
    private LinkManager linkManager;

    // Infrastructure
    private MessageSender messageSender;
    private RestApiServer restApiServer;

    @Inject
    public BlockwaySocialVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Blockway Social starting up...");

        // 1. Load configuration
        config = new VelocityConfig(dataDirectory, logger);
        config.load();

        // 2. Database
        databaseManager = new DatabaseManager(config, logger);
        databaseManager.initialize();

        // 3. Repositories
        playerRepository = new PlayerRepository(databaseManager, logger);
        friendsRepository = new FriendsRepository(databaseManager, logger);
        partyRepository = new PartyRepository(databaseManager, logger);
        chatRepository = new ChatRepository(databaseManager, logger);
        linkRepository = new LinkRepository(databaseManager, logger);
        apiKeyRepository = new ApiKeyRepository(databaseManager, logger);
        playerSettingsRepository = new PlayerSettingsRepository(databaseManager, logger);

        // 4. Managers
        presenceManager = new PresenceManager();
        messageSender = new MessageSender(proxy, logger);

        friendsManager = new FriendsManager(
                friendsRepository, playerRepository, playerSettingsRepository,
                presenceManager, messageSender, proxy, config, logger);

        partyManager = new PartyManager(
                partyRepository, playerRepository, chatRepository,
                presenceManager, messageSender, proxy, config, logger);

        linkManager = new LinkManager(linkRepository, config, logger);

        // 5. Plugin messaging channel
        proxy.getChannelRegistrar().register(MessageSender.CHANNEL);
        VelocityMessageHandler messageHandler = new VelocityMessageHandler(
                friendsManager, partyManager, presenceManager, linkManager, messageSender, logger);
        proxy.getEventManager().register(this, messageHandler);

        // 6. Connection lifecycle listener
        proxy.getEventManager().register(this, new PlayerConnectionListener(
                presenceManager, friendsManager, playerRepository, logger));

        // 7. Admin command
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("bwsocial")
                        .aliases("bws")
                        .plugin(this)
                        .build(),
                new AdminCommand(config, databaseManager, logger)
        );

        // 8. REST API
        if (config.isApiEnabled()) {
            restApiServer = new RestApiServer(config, databaseManager, friendsManager,
                    partyManager, presenceManager, linkManager, messageSender, logger);
            restApiServer.start();
        } else {
            logger.info("REST API is disabled in config.");
        }

        // 9. Maintenance tasks (clean expired link codes every 5 minutes)
        proxy.getScheduler()
                .buildTask(this, linkManager::cleanExpiredCodes)
                .delay(5, TimeUnit.MINUTES)
                .repeat(5, TimeUnit.MINUTES)
                .schedule();

        logger.info("Blockway Social is ready! Enzonic LLC — blockway.space");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (restApiServer != null) restApiServer.stop();
        if (databaseManager != null) databaseManager.close();
        logger.info("Blockway Social shut down cleanly.");
    }

    // Getters for cross-component access if needed
    public ProxyServer getProxy() { return proxy; }
    public VelocityConfig getPluginConfig() { return config; }
    public FriendsManager getFriendsManager() { return friendsManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public PresenceManager getPresenceManager() { return presenceManager; }
    public LinkManager getLinkManager() { return linkManager; }
}
