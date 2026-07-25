package de.gamematex.pvpheads.listener;

import de.gamematex.pvpheads.PvPHeadsKeys;
import de.gamematex.pvpheads.PvPHeadsPlugin;
import de.gamematex.pvpheads.gui.HeadGuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class HeadInteractListener implements Listener {

    private static final String USE_PERMISSION = "pvpheads.use";

    private final PvPHeadsPlugin plugin;
    private final HeadGuiManager guiManager;

    public HeadInteractListener(PvPHeadsPlugin plugin, HeadGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // Verhindert doppeltes Auslösen durch Haupt- und Nebenhand beim selben Klick
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Skull skull)) {
            return;
        }

        boolean isLootHead = skull.getPersistentDataContainer()
                .getOrDefault(PvPHeadsKeys.isLootHead(plugin), PersistentDataType.BYTE, (byte) 0) == 1;

        if (!isLootHead) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage(Component.text("Dazu hast du keine Berechtigung.", NamedTextColor.RED));
            return;
        }

        guiManager.open(player, block);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        guiManager.handleClose(event.getInventory());
    }
}
