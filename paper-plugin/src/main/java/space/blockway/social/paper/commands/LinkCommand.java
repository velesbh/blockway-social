package space.blockway.social.paper.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

/**
 * Handles {@code /link} and {@code /unlink} for web account linking.
 *
 * @author Enzonic LLC — blockway.space
 */
public class LinkCommand implements CommandExecutor {

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public LinkCommand(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender src, Command cmd, String label, String[] args) {
        if (!(src instanceof Player player)) { src.sendMessage("Player only."); return true; }

        if (label.equalsIgnoreCase("link")) {
            sender.sendLinkGenerate(player);
        } else if (label.equalsIgnoreCase("unlink")) {
            sender.sendLinkRemove(player);
        }
        return true;
    }
}
