package de.gamematex.pvpheads;

import org.bukkit.NamespacedKey;

/**
 * Zentrale Sammlung aller NamespacedKeys, die dieses Plugin für
 * PersistentDataContainer (Block-States und ItemStacks) verwendet.
 */
public final class PvPHeadsKeys {

    private PvPHeadsKeys() {
    }

    // Markiert einen Kopf (Block oder Item) eindeutig als PvPHeads-Loot-Kopf
    public static NamespacedKey isLootHead(PvPHeadsPlugin plugin) {
        return new NamespacedKey(plugin, "loot_head");
    }

    // Base64-serialisierte Liste der enthaltenen ItemStacks
    public static NamespacedKey lootData(PvPHeadsPlugin plugin) {
        return new NamespacedKey(plugin, "loot_data");
    }

    // Name des getöteten Spielers (für den GUI-Titel)
    public static NamespacedKey ownerName(PvPHeadsPlugin plugin) {
        return new NamespacedKey(plugin, "owner_name");
    }

    // UUID des getöteten Spielers (für spätere Erweiterungen, Statistiken etc.)
    public static NamespacedKey ownerUuid(PvPHeadsPlugin plugin) {
        return new NamespacedKey(plugin, "owner_uuid");
    }

    // Name des Killers (für das Schild-Feature)
    public static NamespacedKey killerName(PvPHeadsPlugin plugin) {
        return new NamespacedKey(plugin, "killer_name");
    }
}
