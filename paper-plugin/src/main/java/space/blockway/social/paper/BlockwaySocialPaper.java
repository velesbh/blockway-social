package space.blockway.social.paper;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import space.blockway.social.paper.commands.*;
import space.blockway.social.paper.config.PaperConfig;
import space.blockway.social.paper.gui.GuiManager;
import space.blockway.social.paper.listeners.*;
import space.blockway.social.paper.messaging.PaperMessageHandler;
import space.blockway.social.paper.messaging.ProxyMessageSender;

/**
 * Entry point for the Blockway Social Paper backend plugin.
 *
 * <p>This plugin is a thin relay layer: it handles in-game GUI/commands,
 * sends plugin messages to the Velocity proxy for all business logic,
 * and displays results returned from the proxy.
 *
 * <p>No database is accessed from the Paper side — all state lives on Velocity.
 *
 * @author Enzonic LLC — blockway.space
 */
public class BlockwaySocialPaper extends JavaPlugin {

    private PaperConfig bwsConfig;
    private ProxyMessageSender messageSender;
    private PaperMessageHandler messageHandler;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        // 1. Config
        saveDefaultConfig();
        this.bwsConfig = new PaperConfig(getConfig());

        // 2. Plugin messaging
        this.messageSender = new ProxyMessageSender(this);
        this.messageHandler = new PaperMessageHandler(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, ProxyMessageSender.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, ProxyMessageSender.CHANNEL, messageHandler);

        // 3. GUI manager
        this.guiManager = new GuiManager(this);

        // 4. Event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this, messageSender), this);
        pm.registerEvents(new PlayerQuitListener(this, messageSender), this);
        pm.registerEvents(new InventoryClickListener(guiManager), this);

        // 5. Commands
        FriendCommand friendCmd = new FriendCommand(this, messageSender);
        getCommand("friend").setExecutor(friendCmd);
        getCommand("friend").setTabCompleter(friendCmd);

        PartyCommand partyCmd = new PartyCommand(this, messageSender);
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);

        FriendChatCommand fcCmd = new FriendChatCommand(this, messageSender);
        getCommand("fc").setExecutor(fcCmd);
        getCommand("friendchat").setExecutor(fcCmd);

        PartyChatCommand pcCmd = new PartyChatCommand(this, messageSender);
        getCommand("pc").setExecutor(pcCmd);
        getCommand("partychat").setExecutor(pcCmd);

        LinkCommand linkCmd = new LinkCommand(this, messageSender);
        getCommand("link").setExecutor(linkCmd);
        getCommand("unlink").setExecutor(linkCmd);

        getLogger().info("Blockway Social Paper plugin enabled — Enzonic LLC | blockway.space");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, ProxyMessageSender.CHANNEL);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, ProxyMessageSender.CHANNEL);
        getLogger().info("Blockway Social Paper plugin disabled.");
    }

    public PaperConfig getBwsConfig() { return bwsConfig; }
    public ProxyMessageSender getMessageSender() { return messageSender; }
    public GuiManager getGuiManager() { return guiManager; }
}
