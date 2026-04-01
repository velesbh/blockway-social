package space.blockway.social.paper.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.paper.messaging.ProxyMessageSender;

import java.util.List;

/**
 * 6-row (54-slot) Friends List GUI.
 *
 * <p>Layout:
 * <pre>
 * Row 0: [prev] [ ] [ ] [title skull] [ ] [ ] [ ] [ ] [next]
 * Row 1-4: Friend skulls (28 per page)
 * Row 5: [add] [ ] [ ] [ ] [pending] [ ] [ ] [ ] [refresh]
 * </pre>
 *
 * <p>Click actions:
 * <ul>
 *   <li>Slot 0: previous page</li>
 *   <li>Slot 8: next page</li>
 *   <li>Slots 9–44: left-click = join friend's server, right-click = remove friend</li>
 *   <li>Slot 45: add friend (closes GUI, prompts player to type /friend add)</li>
 *   <li>Slot 53: refresh</li>
 * </ul>
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendsGui extends AbstractGui {

    static final int FRIENDS_PER_PAGE = 28;
    static final int[] FRIEND_SLOTS;

    static {
        FRIEND_SLOTS = new int[28];
        int idx = 0;
        for (int slot = 9; slot <= 44; slot++) {
            if (slot % 9 != 0 && slot % 9 != 8) { // skip column 0 and 8
                FRIEND_SLOTS[idx++] = slot;
            }
        }
    }

    private final Player player;
    private final BlockwaySocialPaper plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // State
    private int page = 0;
    private List<space.blockway.social.shared.dto.FriendDto> friends = List.of();

    public FriendsGui(Player player, BlockwaySocialPaper plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    @Override
    public void open() {
        this.inventory = Bukkit.createInventory(null, 54,
                mm.deserialize(plugin.getBwsConfig().getFriendsGuiTitle()));
        populateFrame();
        player.openInventory(inventory);
        // Friends list is loaded from the proxy asynchronously via plugin messages in a real impl
        // For now we display an empty state — the data arrives via FRIEND_CHAT_MESSAGE after /friend list
    }

    private void populateFrame() {
        inventory.clear();

        // Filler glass panes in border rows
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);
        inventory.setItem(0, makeItem(Material.ARROW, "<white>Previous Page"));
        inventory.setItem(8, makeItem(Material.ARROW, "<white>Next Page"));
        inventory.setItem(4, makeItem(Material.PLAYER_HEAD, "<gradient:#00c3ff:#0080ff>Friends List",
                "<gray>Page: " + (page + 1)));

        // Action bar
        inventory.setItem(45, makeItem(Material.NAME_TAG, "<green>Add Friend",
                "<gray>Close and type /friend add <name>"));
        inventory.setItem(49, makeItem(Material.WRITABLE_BOOK, "<yellow>Pending Requests"));
        inventory.setItem(53, makeItem(Material.CLOCK, "<aqua>Refresh"));

        // Populate friend slots
        int start = page * FRIENDS_PER_PAGE;
        for (int i = 0; i < FRIENDS_PER_PAGE; i++) {
            int friendIdx = start + i;
            if (friendIdx >= friends.size()) break;
            var friend = friends.get(friendIdx);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.displayName(mm.deserialize("<yellow>" + friend.getUsername()));
            String statusLine = friend.isOnline()
                    ? "<green>Online</green> on <yellow>" + friend.getOnlineServer()
                    : "<dark_gray>Offline";
            meta.lore(List.of(
                    mm.deserialize("<gray>Status: " + statusLine),
                    mm.deserialize("<dark_gray>Left-click: Join server"),
                    mm.deserialize("<dark_gray>Right-click: Remove friend")
            ));
            skull.setItemMeta(meta);
            inventory.setItem(FRIEND_SLOTS[i], skull);
        }
    }

    @Override
    public void handleClick(int slot) {
        if (slot == 0 && page > 0) {
            page--;
            populateFrame();
            return;
        }
        if (slot == 8 && (page + 1) * FRIENDS_PER_PAGE < friends.size()) {
            page++;
            populateFrame();
            return;
        }
        if (slot == 45) {
            player.closeInventory();
            player.sendMessage(mm.deserialize("<gray>Type <yellow>/friend add <name></yellow> to add a friend."));
            return;
        }
        if (slot == 53) {
            // Request refresh — in a full implementation this sends a plugin message to refresh
            populateFrame();
            return;
        }
        // Friend slots
        for (int i = 0; i < FRIEND_SLOTS.length; i++) {
            if (FRIEND_SLOTS[i] == slot) {
                int friendIdx = page * FRIENDS_PER_PAGE + i;
                if (friendIdx >= friends.size()) return;
                var friend = friends.get(friendIdx);
                // Join friend's server (left click)
                plugin.getMessageSender().sendFriendJoin(player, friend.getUsername());
                player.closeInventory();
                return;
            }
        }
    }

    /** Called by PaperMessageHandler when fresh friend data arrives. */
    public void updateFriends(List<space.blockway.social.shared.dto.FriendDto> friends) {
        this.friends = friends;
        this.page = 0;
        populateFrame();
    }

    private ItemStack makeItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        if (loreLines.length > 0) {
            meta.lore(java.util.Arrays.stream(loreLines).map(mm::deserialize).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
