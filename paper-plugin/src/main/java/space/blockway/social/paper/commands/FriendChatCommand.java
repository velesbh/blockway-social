package space.blockway.social.paper.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

import java.util.List;

/**
 * Handles {@code /fc} and {@code /friendchat} — sends a message to all online friends.
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendChatCommand implements CommandExecutor {

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public FriendChatCommand(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender src, Command cmd, String label, String[] args) {
        if (!(src instanceof Player player)) { src.sendMessage("Player only."); return true; }
        if (args.length == 0) { player.sendMessage("Usage: /fc <message>"); return true; }
        String message = String.join(" ", args);
        sender.sendFriendChat(player, message);
        return true;
    }
}
