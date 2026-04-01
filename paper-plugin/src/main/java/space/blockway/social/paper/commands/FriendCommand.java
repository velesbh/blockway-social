package space.blockway.social.paper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the {@code /friend} command. All actions are relayed to the Velocity proxy for processing.
 *
 * <p>Sub-commands: add, remove, list, accept, deny, join
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("add", "remove", "list", "accept", "deny", "join");

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public FriendCommand(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender src, Command cmd, String label, String[] args) {
        if (!(src instanceof Player player)) {
            src.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openFriendsGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { player.sendMessage(plugin.getBwsConfig().getPrefixedMessage("friend-not-found", "player", "")); return true; }
                sender.sendFriendRequest(player, args[1]);
                player.sendMessage(plugin.getBwsConfig().getPrefixedMessage("friend-request-sent", "player", args[1]));
            }
            case "remove" -> {
                if (args.length < 2) return true;
                sender.sendFriendRemove(player, args[1]);
            }
            case "list" -> plugin.getGuiManager().openFriendsGui(player);
            case "accept" -> {
                if (args.length < 2) return true;
                sender.sendFriendAccept(player, args[1]);
            }
            case "deny" -> {
                if (args.length < 2) return true;
                sender.sendFriendDeny(player, args[1]);
            }
            case "join" -> {
                if (args.length < 2) return true;
                sender.sendFriendJoin(player, args[1]);
            }
            default -> player.sendMessage(plugin.getBwsConfig().getPrefixedMessage("friend-not-found", "player", args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender src, Command cmd, String label, String[] args) {
        if (args.length == 1) return SUB_COMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("create")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
