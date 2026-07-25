package de.gamematex.pvpheads.gui;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Markiert eine Inventory-Instanz als "gehört zu einem platzierten Loot-Kopf
 * an dieser Location". So können wir beim Schließen erkennen, wohin die
 * (eventuell veränderten) Inhalte zurückgeschrieben werden müssen.
 */
public class HeadInventoryHolder implements InventoryHolder {

    private final Location location;
    private Inventory inventory;

    public HeadInventoryHolder(Location location) {
        this.location = location;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
