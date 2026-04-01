package space.blockway.social.paper.gui;

import org.bukkit.inventory.Inventory;

/**
 * Base class for all Blockway Social chest-based GUIs.
 *
 * @author Enzonic LLC — blockway.space
 */
public abstract class AbstractGui {

    protected Inventory inventory;

    /** Build and open the inventory to the player. */
    public abstract void open();

    /** Handle a click at the given slot index. The event is already cancelled. */
    public abstract void handleClick(int slot);

    public Inventory getInventory() {
        return inventory;
    }
}
