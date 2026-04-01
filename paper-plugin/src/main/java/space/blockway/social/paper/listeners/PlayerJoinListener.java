package space.blockway.social.paper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

/**
 * Notifies the Velocity proxy when a player joins this backend server.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerJoinListener implements Listener {

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public PlayerJoinListener(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sender.sendPlayerJoin(event.getPlayer(), plugin.getBwsConfig().getServerName());
    }
}
