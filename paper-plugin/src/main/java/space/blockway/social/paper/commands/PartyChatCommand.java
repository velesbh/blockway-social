package space.blockway.social.paper.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

/**
 * Handles {@code /pc} and {@code /partychat} — sends a message to all party members.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyChatCommand implements CommandExecutor {

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public PartyChatCommand(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender src, Command cmd, String label, String[] args) {
        if (!(src instanceof Player player)) { src.sendMessage("Player only."); return true; }
        if (args.length == 0) { player.sendMessage("Usage: /pc <message>"); return true; }
        sender.sendPartyChat(player, String.join(" ", args));
        return true;
    }
}
