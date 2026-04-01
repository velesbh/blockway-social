package space.blockway.social.paper.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.shared.dto.PartyDto;

import java.util.List;

/**
 * 3-row (27-slot) Party Management GUI.
 *
 * <p>Layout:
 * <pre>
 * Row 0: [leader indicator] [..] [..] [..] [party info] [..] [..] [..] [disband/leave]
 * Row 1: [member 1..9 heads]
 * Row 2: [invite] [..] [..] [..] [warp] [..] [..] [..] [chat tip]
 * </pre>
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyGui extends AbstractGui {

    private final Player player;
    private final BlockwaySocialPaper plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private PartyDto party;

    public PartyGui(Player player, BlockwaySocialPaper plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    @Override
    public void open() {
        this.inventory = Bukkit.createInventory(null, 27,
                mm.deserialize(plugin.getBwsConfig().getPartyGuiTitle()));
        populate();
        player.openInventory(inventory);
    }

    private void populate() {
        inventory.clear();

        // Filler
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 18; i < 27; i++) inventory.setItem(i, filler);
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);

        if (party == null) {
            inventory.setItem(4, makeItem(Material.BARRIER, "<red>Not in a party",
                    "<gray>Use <yellow>/party create</yellow> to start one."));
            inventory.setItem(18, makeItem(Material.NAME_TAG, "<green>Create Party"));
            return;
        }

        boolean isLeader = party.getLeaderUuid().equals(player.getUniqueId().toString());

        // Slot 0: leader crown indicator
        inventory.setItem(0, makeItem(isLeader ? Material.GOLDEN_SWORD : Material.IRON_SWORD,
                isLeader ? "<gold>You are the Party Leader" : "<gray>Party Member"));

        // Slot 4: party info
        inventory.setItem(4, makeItem(Material.PLAYER_HEAD, "<#9B59B6>" + party.getLeaderUsername() + "'s Party",
                "<gray>Members: <white>" + party.getMemberCount(),
                "<dark_gray>Party ID: " + party.getPartyId().substring(0, 8) + "..."));

        // Slot 8: leave/disband
        if (isLeader) {
            inventory.setItem(8, makeItem(Material.BARRIER, "<red>Disband Party",
                    "<gray>Click to disband the party"));
        } else {
            inventory.setItem(8, makeItem(Material.OAK_DOOR, "<orange>Leave Party",
                    "<gray>Click to leave the party"));
        }

        // Row 1: member heads (slots 9–17)
        List<String> memberUuids = party.getMemberUuids();
        for (int i = 0; i < Math.min(9, memberUuids.size()); i++) {
            boolean memberIsLeader = memberUuids.get(i).equals(party.getLeaderUuid());
            String memberName = party.getMembers() != null && i < party.getMembers().size()
                    ? party.getMembers().get(i).getUsername() : memberUuids.get(i).substring(0, 8);
            inventory.setItem(9 + i, makeItem(Material.PLAYER_HEAD,
                    (memberIsLeader ? "<gold>[Leader] " : "<white>") + memberName,
                    isLeader && !memberIsLeader ? "<dark_gray>Right-click to kick" : ""));
        }

        // Row 2 action bar
        inventory.setItem(18, makeItem(Material.NAME_TAG, "<green>Invite Player",
                "<gray>Sends a party invite to another player"));
        inventory.setItem(22, makeItem(Material.COMPASS,
                isLeader ? "<yellow>Warp Party Here" : "<dark_gray>Warp Party (Leader Only)",
                isLeader ? "<gray>Moves all members to your server" : "<dark_gray>Only the leader can warp"));
        inventory.setItem(26, makeItem(Material.OAK_SIGN, "<aqua>Party Chat",
                "<gray>Use <yellow>/pc <message></yellow> to chat"));
    }

    @Override
    public void handleClick(int slot) {
        if (party == null) {
            if (slot == 18) { player.closeInventory(); plugin.getMessageSender().sendPartyCreate(player); }
            return;
        }
        boolean isLeader = party.getLeaderUuid().equals(player.getUniqueId().toString());

        if (slot == 8) {
            player.closeInventory();
            if (isLeader) plugin.getMessageSender().sendPartyDisband(player);
            else plugin.getMessageSender().sendPartyLeave(player);
        } else if (slot == 18) {
            player.closeInventory();
            player.sendMessage(mm.deserialize("<gray>Type <yellow>/party invite <name></yellow> to invite a player."));
        } else if (slot == 22 && isLeader) {
            // Warp to current server
            String serverName = plugin.getBwsConfig().getServerName();
            plugin.getMessageSender().sendPartyWarp(player, serverName);
        } else if (slot >= 9 && slot <= 17 && isLeader) {
            // Kick member (right-click is handled at GUI level — left click here for simplicity)
            int memberIdx = slot - 9;
            List<String> members = party.getMemberUuids();
            if (memberIdx < members.size() && !members.get(memberIdx).equals(party.getLeaderUuid())) {
                String memberName = party.getMembers() != null && memberIdx < party.getMembers().size()
                        ? party.getMembers().get(memberIdx).getUsername() : members.get(memberIdx);
                plugin.getMessageSender().sendPartyKick(player, memberName);
            }
        }
    }

    public void updateParty(PartyDto party) {
        this.party = party;
        populate();
    }

    private ItemStack makeItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        if (loreLines.length > 0) {
            meta.lore(java.util.Arrays.stream(loreLines)
                    .filter(l -> !l.isEmpty()).map(mm::deserialize).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
