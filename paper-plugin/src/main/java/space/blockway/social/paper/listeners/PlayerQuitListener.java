package space.blockway.social.paper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

/**
 * Notifies the Velocity proxy when a player leaves this backend server.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerQuitListener implements Listener {

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public PlayerQuitListener(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Only send if there are other players who can carry the message
        if (plugin.getServer().getOnlinePlayers().size() > 1) {
            sender.sendPlayerLeave(event.getPlayer(), plugin.getBwsConfig().getServerName());
        }
        // Velocity also tracks this via its own DisconnectEvent
    }
}
