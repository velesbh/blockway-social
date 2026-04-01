package space.blockway.social.paper.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import space.blockway.social.paper.BlockwaySocialPaper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks open GUI instances per player and routes inventory events to them.
 *
 * @author Enzonic LLC — blockway.space
 */
public class GuiManager {

    private final BlockwaySocialPaper plugin;
    private final Map<UUID, AbstractGui> openGuis = new ConcurrentHashMap<>();

    public GuiManager(BlockwaySocialPaper plugin) {
        this.plugin = plugin;
    }

    public void openFriendsGui(Player player) {
        FriendsGui gui = new FriendsGui(player, plugin);
        openGuis.put(player.getUniqueId(), gui);
        gui.open();
    }

    public void openPartyGui(Player player) {
        PartyGui gui = new PartyGui(player, plugin);
        openGuis.put(player.getUniqueId(), gui);
        gui.open();
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        AbstractGui gui = openGuis.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui.getInventory())) return;
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getInventory().getSize()) return; // clicked player inv
        gui.handleClick(event.getSlot());
    }

    public void handleClose(UUID playerUuid) {
        openGuis.remove(playerUuid);
    }

    public AbstractGui getOpenGui(UUID playerUuid) {
        return openGuis.get(playerUuid);
    }
}
