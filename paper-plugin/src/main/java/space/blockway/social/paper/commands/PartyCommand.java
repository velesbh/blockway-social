package space.blockway.social.paper.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the {@code /party} command. All actions relay to the Velocity proxy.
 *
 * <p>Sub-commands: create, invite, join, leave, kick, disband, list, warp
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS =
            List.of("create", "invite", "join", "leave", "kick", "disband", "list", "warp");

    private final BlockwaySocialPaper plugin;
    private final ProxyMessageSender sender;

    public PartyCommand(BlockwaySocialPaper plugin, ProxyMessageSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public boolean onCommand(CommandSender src, Command cmd, String label, String[] args) {
        if (!(src instanceof Player player)) { src.sendMessage("Player only."); return true; }

        if (args.length == 0) { plugin.getGuiManager().openPartyGui(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create"  -> sender.sendPartyCreate(player);
            case "invite"  -> { if (args.length < 2) return true; sender.sendPartyInvite(player, args[1]); }
            case "join"    -> { if (args.length < 2) return true; sender.sendPartyAccept(player, args[1]); }
            case "leave"   -> sender.sendPartyLeave(player);
            case "kick"    -> { if (args.length < 2) return true; sender.sendPartyKick(player, args[1]); }
            case "disband" -> sender.sendPartyDisband(player);
            case "list"    -> plugin.getGuiManager().openPartyGui(player);
            case "warp"    -> {
                if (args.length < 2) { player.sendMessage("Usage: /party warp <server>"); return true; }
                sender.sendPartyWarp(player, args[1]);
            }
            default -> player.sendMessage(plugin.getBwsConfig().getPrefixedMessage("party-player-not-found", "player", args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender src, Command cmd, String label, String[] args) {
        if (args.length == 1) return SUB_COMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && List.of("invite","kick","join").contains(args[0].toLowerCase())) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
