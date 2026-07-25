package de.gamematex.pvpheads.listener;

import de.gamematex.pvpheads.PvPHeadsKeys;
import de.gamematex.pvpheads.PvPHeadsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Wenn ein Spieler ein Schild direkt neben (oder über/unter) einem platzierten
 * Loot-Kopf setzt, wird der Text automatisch mit "Killer" und "Opfer" befüllt,
 * statt das leere Schild-Eingabefenster zu übernehmen.
 */
public class HeadSignListener implements Listener {

    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private final PvPHeadsPlugin plugin;

    public HeadSignListener(PvPHeadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Skull lootHead = findAdjacentLootHead(event.getBlock());
        if (lootHead == null) {
            return;
        }

        PersistentDataContainer pdc = lootHead.getPersistentDataContainer();
        String victim = pdc.getOrDefault(PvPHeadsKeys.ownerName(plugin), PersistentDataType.STRING, "Unbekannt");
        String killer = pdc.getOrDefault(PvPHeadsKeys.killerName(plugin), PersistentDataType.STRING, "Unbekannt");

        event.line(0, Component.text("Erlegt von", NamedTextColor.DARK_GRAY));
        event.line(1, Component.text(killer, NamedTextColor.RED));
        event.line(2, Component.text("Opfer:", NamedTextColor.DARK_GRAY));
        event.line(3, Component.text(victim, NamedTextColor.BLUE));
    }

    private Skull findAdjacentLootHead(Block signBlock) {
        for (BlockFace face : ADJACENT_FACES) {
            Block relative = signBlock.getRelative(face);
            if (!(relative.getState() instanceof Skull skull)) {
                continue;
            }
            boolean isLootHead = skull.getPersistentDataContainer()
                    .getOrDefault(PvPHeadsKeys.isLootHead(plugin), PersistentDataType.BYTE, (byte) 0) == 1;
            if (isLootHead) {
                return skull;
            }
        }
        return null;
    }
}
