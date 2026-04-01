package space.blockway.social.paper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import space.blockway.social.paper.gui.GuiManager;

/**
 * Routes inventory click and close events to the {@link GuiManager}.
 *
 * @author Enzonic LLC — blockway.space
 */
public class InventoryClickListener implements Listener {

    private final GuiManager guiManager;

    public InventoryClickListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        guiManager.handleClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        guiManager.handleClose(event.getPlayer().getUniqueId());
    }
}
