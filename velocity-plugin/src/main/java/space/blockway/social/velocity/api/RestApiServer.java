package space.blockway.social.velocity.api;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import org.slf4j.Logger;
import space.blockway.social.velocity.api.handlers.*;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.*;
import space.blockway.social.velocity.managers.*;
import space.blockway.social.velocity.messaging.MessageSender;

/**
 * Embedded Javalin HTTP server that exposes the Blockway Social REST API.
 *
 * <p><strong>Javalin class-loader note:</strong> Velocity uses an isolated class loader per plugin.
 * Javalin initialises Jetty via {@code ServiceLoader}, which reads from
 * {@code Thread.currentThread().getContextClassLoader()}. Without switching the context class
 * loader to the plugin's own class loader before calling {@code start()}, Jetty cannot find its
 * own classes and throws {@code NoClassDefFoundError}. The class loader is restored immediately
 * after {@code start()} completes.
 *
 * @author Enzonic LLC — blockway.space
 */
public class RestApiServer {

    private Javalin app;
    private final VelocityConfig config;

    // Handlers
    private final FriendsApiHandler friendsHandler;
    private final PartyApiHandler partyHandler;
    private final PlayerApiHandler playerHandler;
    private final ChatApiHandler chatHandler;
    private final LinkApiHandler linkHandler;
    private final AdminApiHandler adminHandler;
    private final ApiKeyAuthFilter authFilter;

    private final Logger logger;

    public RestApiServer(VelocityConfig config,
                          DatabaseManager databaseManager,
                          FriendsManager friendsManager,
                          PartyManager partyManager,
                          PresenceManager presenceManager,
                          LinkManager linkManager,
                          MessageSender messageSender,
                          Logger logger) {
        this.config = config;
        this.logger = logger;

        ApiKeyRepository apiKeyRepo = new ApiKeyRepository(databaseManager, logger);
        PlayerRepository playerRepo = new PlayerRepository(databaseManager, logger);
        ChatRepository chatRepo = new ChatRepository(databaseManager, logger);
        LinkRepository linkRepo = new LinkRepository(databaseManager, logger);

        this.authFilter = new ApiKeyAuthFilter(apiKeyRepo, config.getMasterKey());
        this.friendsHandler = new FriendsApiHandler(friendsManager, playerRepo, presenceManager);
        this.partyHandler = new PartyApiHandler(partyManager, playerRepo, presenceManager);
        this.playerHandler = new PlayerApiHandler(playerRepo, presenceManager, linkRepo);
        this.chatHandler = new ChatApiHandler(chatRepo, partyManager, playerRepo, presenceManager, messageSender, config);
        this.linkHandler = new LinkApiHandler(linkManager, linkRepo);
        this.adminHandler = new AdminApiHandler(apiKeyRepo, config.getMasterKey());
    }

    /** Start the Javalin HTTP server. */
    public void start() {
        // ── Class loader swap (required for Velocity + Javalin) ──────────────
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            app = Javalin.create(cfg -> {
                cfg.showJavalinBanner = false;
                cfg.router.apiBuilder(this::configureRoutes);
            }).start(config.getApiBind(), config.getApiPort());

            logger.info("Blockway Social REST API listening on {}:{}", config.getApiBind(), config.getApiPort());
        } catch (Exception e) {
            logger.error("Failed to start REST API server", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    private void configureRoutes() {
        ApiBuilder.path("/api/v1", () -> {
            // Apply auth filter to everything under /api/v1
            ApiBuilder.before(authFilter);

            // ── Player ────────────────────────────────────────────────────────
            ApiBuilder.path("/player/{username}", () -> {
                ApiBuilder.get("/friends", friendsHandler::getFriends);
                ApiBuilder.get("/party",   partyHandler::getPlayerParty);
                ApiBuilder.get("/status",  playerHandler::getPlayerStatus);
            });

            // ── Party ─────────────────────────────────────────────────────────
            ApiBuilder.path("/party", () -> {
                ApiBuilder.post("/chat",               chatHandler::sendPartyChat);
                ApiBuilder.get("/{partyId}",           partyHandler::getPartyById);
                ApiBuilder.get("/{partyId}/messages",  chatHandler::getPartyMessages);
            });

            // ── Friend chat ───────────────────────────────────────────────────
            ApiBuilder.path("/friend", () -> {
                ApiBuilder.post("/chat",               chatHandler::sendFriendChat);
                ApiBuilder.get("/{uuid}/messages",     chatHandler::getFriendMessages);
            });

            // ── Link ──────────────────────────────────────────────────────────
            ApiBuilder.path("/link", () -> {
                ApiBuilder.post("/verify",   linkHandler::verifyCode);
                ApiBuilder.get("/{uuid}",    linkHandler::getLinkStatus);
                ApiBuilder.delete("/{uuid}", linkHandler::unlink);
            });

            // ── Admin (master key required) ───────────────────────────────────
            ApiBuilder.path("/apikey", () -> {
                ApiBuilder.get("/generate", adminHandler::generateApiKey);
                ApiBuilder.get("/list",     adminHandler::listApiKeys);
                ApiBuilder.delete("/{label}", adminHandler::revokeApiKey);
            });
        });
    }

    /** Stop the Javalin HTTP server. */
    public void stop() {
        if (app != null) {
            app.stop();
            logger.info("Blockway Social REST API stopped.");
        }
    }
}
