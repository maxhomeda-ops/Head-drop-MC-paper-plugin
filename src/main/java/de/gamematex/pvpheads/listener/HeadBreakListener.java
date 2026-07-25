package de.gamematex.pvpheads.listener;

import de.gamematex.pvpheads.PvPHeadsKeys;
import de.gamematex.pvpheads.PvPHeadsPlugin;
import de.gamematex.pvpheads.gui.HeadGuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class HeadBreakListener implements Listener {

    private static final String USE_PERMISSION = "pvpheads.use";

    private final PvPHeadsPlugin plugin;
    private final HeadGuiManager guiManager;

    public HeadBreakListener(PvPHeadsPlugin plugin, HeadGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Skull skull)) {
            return;
        }

        PersistentDataContainer blockPdc = skull.getPersistentDataContainer();
        boolean isLootHead = blockPdc.getOrDefault(PvPHeadsKeys.isLootHead(plugin), PersistentDataType.BYTE, (byte) 0) == 1;
        if (!isLootHead) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(USE_PERMISSION)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Dazu hast du keine Berechtigung.", NamedTextColor.RED));
            return;
        }

        String ownerName = blockPdc.getOrDefault(PvPHeadsKeys.ownerName(plugin), PersistentDataType.STRING, "Unbekannt");

        // Aktuellen Loot-Stand holen (falls gerade eine GUI offen war/ist, zählt deren Inhalt,
        // damit bereits entnommene Items nicht dupliziert werden) und alle Betrachter rauswerfen.
        List<ItemStack> remainingLoot = guiManager.closeAndCollect(block.getLocation(), skull);

        // Standard-Drop (vanilla Spielerkopf) unterbinden - wir droppen selbst
        event.setDropItems(false);

        Location dropLocation = block.getLocation().add(0.5, 0.2, 0.5);

        // Loot fällt einzeln auf den Boden, statt im Kopf-Item gespeichert zu bleiben
        for (ItemStack item : remainingLoot) {
            block.getWorld().dropItemNaturally(dropLocation, item);
        }

        // Der Kopf selbst droppt zusätzlich als leeres Trophäen-Item (Skin bleibt erhalten)
        ItemStack trophy = buildTrophyItem(skull, ownerName);
        block.getWorld().dropItemNaturally(dropLocation, trophy);
    }

    private ItemStack buildTrophyItem(Skull skull, String ownerName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Skin 1:1 vom Block übernehmen (bereits korrekt aufgelöstes Profil inkl. Textur)
        if (skull.getOwnerProfile() != null) {
            meta.setOwnerProfile(skull.getOwnerProfile());
        }

        meta.displayName(Component.text(ownerName + "'s Kopf", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        // Bewusst KEIN loot_head/loot_data-Tag mehr: der Kopf ist jetzt rein kosmetisch,
        // trägt keine Beute mehr und wird daher auch nicht mehr von der Interact-/
        // Dupe-Schutz-Logik als Loot-Kopf erkannt.

        item.setItemMeta(meta);
        return item;
    }
}
