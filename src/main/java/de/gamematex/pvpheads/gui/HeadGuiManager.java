package de.gamematex.pvpheads.gui;

import de.gamematex.pvpheads.PvPHeadsKeys;
import de.gamematex.pvpheads.PvPHeadsPlugin;
import de.gamematex.pvpheads.util.ItemSerialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sorgt dafür, dass mehrere Spieler denselben platzierten Kopf gemeinsam
 * looten können (wie eine geteilte Truhe): Pro Block-Location wird nur eine
 * Inventory-Instanz gehalten, solange der Kopf existiert.
 */
public class HeadGuiManager {

    private final PvPHeadsPlugin plugin;
    private final Map<Location, Inventory> openInventories = new HashMap<>();

    public HeadGuiManager(PvPHeadsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Öffnet die Loot-GUI für den angegebenen Kopf-Block. Wenn bereits eine
     * Inventory für diese Location existiert (weil ein anderer Spieler schon
     * drin ist), wird dieselbe Instanz wiederverwendet.
     */
    public void open(Player player, Block headBlock) {
        Location key = headBlock.getLocation();
        Inventory inventory = openInventories.get(key);

        if (inventory == null) {
            inventory = buildInventoryFromBlock(headBlock, key);
            if (inventory == null) {
                return;
            }
            openInventories.put(key, inventory);
        }

        player.openInventory(inventory);
    }

    private Inventory buildInventoryFromBlock(Block block, Location key) {
        if (!(block.getState() instanceof Skull skull)) {
            return null;
        }
        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        String lootString = pdc.get(PvPHeadsKeys.lootData(plugin), PersistentDataType.STRING);
        String ownerName = pdc.getOrDefault(PvPHeadsKeys.ownerName(plugin), PersistentDataType.STRING, "Unbekannt");

        List<ItemStack> loot = ItemSerialization.deserialize(lootString);
        int size = computeInventorySize(loot.size());

        HeadInventoryHolder holder = new HeadInventoryHolder(key);
        Component title = Component.text("Loot von " + ownerName, NamedTextColor.DARK_GRAY);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        for (int i = 0; i < loot.size() && i < size; i++) {
            inventory.setItem(i, loot.get(i));
        }
        return inventory;
    }

    /**
     * Größe der GUI dynamisch passend zur Item-Anzahl (Vielfaches von 9,
     * minimum 9, maximum 54 Slots).
     */
    private int computeInventorySize(int itemCount) {
        int size = ((itemCount + 8) / 9) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        return size;
    }

    /**
     * Wird beim Schließen einer Loot-GUI aufgerufen: schreibt den aktuellen
     * Inhalt zurück in die PDC des Blocks (falls der Block noch existiert).
     */
    public void handleClose(Inventory inventory) {
        if (!(inventory.getHolder() instanceof HeadInventoryHolder holder)) {
            return;
        }
        // Nur zurückschreiben, wenn kein weiterer Spieler mehr zuschaut
        if (!inventory.getViewers().isEmpty()) {
            return;
        }

        Location loc = holder.getLocation();
        Block block = loc.getBlock();
        if (block.getState() instanceof Skull skull) {
            saveContentsToSkull(inventory, skull);
        }
        openInventories.remove(loc);
    }

    private void saveContentsToSkull(Inventory inventory, Skull skull) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                remaining.add(item);
            }
        }
        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        pdc.set(PvPHeadsKeys.lootData(plugin), PersistentDataType.STRING, ItemSerialization.serialize(remaining));
        skull.update(true, false);
    }

    /**
     * Liefert die aktuell im Cache gehaltene Inventory für eine Location,
     * ohne sie neu zu erzeugen (wird beim Abbauen des Kopfes gebraucht, damit
     * bereits herausgenommene Items nicht dupliziert werden).
     */
    public Inventory getCached(Location location) {
        return openInventories.get(location);
    }

    /**
     * Entfernt eine Location komplett aus dem Cache und schließt alle
     * aktuellen Betrachter (wird beim Abbau des Kopfes benötigt).
     */
    public List<ItemStack> closeAndCollect(Location location, TileState fallbackState) {
        Inventory inventory = openInventories.remove(location);
        List<ItemStack> contents = new ArrayList<>();

        if (inventory != null) {
            for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                viewer.closeInventory();
            }
            for (ItemStack item : inventory.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    contents.add(item);
                }
            }
        } else if (fallbackState instanceof Skull skull) {
            PersistentDataContainer pdc = skull.getPersistentDataContainer();
            String lootString = pdc.get(PvPHeadsKeys.lootData(plugin), PersistentDataType.STRING);
            contents.addAll(ItemSerialization.deserialize(lootString));
        }
        return contents;
    }
}
