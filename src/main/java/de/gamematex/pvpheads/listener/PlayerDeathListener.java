package de.gamematex.pvpheads.listener;

import de.gamematex.pvpheads.PvPHeadsKeys;
import de.gamematex.pvpheads.PvPHeadsPlugin;
import de.gamematex.pvpheads.util.ItemSerialization;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Entscheidet bei jedem Spielertod, ob es sich um einen PvP-Kill handelt
 * (letzter Schaden kam direkt oder indirekt von einem anderen Spieler).
 * Nur dann: normaler Item-Drop wird unterbunden und stattdessen ein
 * platzierter Loot-Kopf mit dem gesamten Inventar des Opfers erzeugt.
 * Bei PvE-/Fallschaden etc. passiert nichts Besonderes (Vanilla-Verhalten).
 */
public class PlayerDeathListener implements Listener {

    private static final BlockFace[] ROTATIONS = {
            BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST
    };

    private final PvPHeadsPlugin plugin;

    public PlayerDeathListener(PvPHeadsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = resolveKillerPlayer(victim);

        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            // Kein PvP-Tod (Mob, Fallschaden, Umgebung, Suizid) -> nichts anfassen
            return;
        }

        List<ItemStack> loot = new ArrayList<>(event.getDrops());
        event.getDrops().clear(); // verhindert, dass der Loot zusätzlich normal droppt

        spawnLootHead(victim, killer, loot);
    }

    private Player resolveKillerPlayer(Player victim) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageEvent)) {
            return null;
        }
        return resolveToPlayer(damageEvent.getDamager());
    }

    private Player resolveToPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player; // Pfeile, Tridents, Feuerwerksraketen etc.
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player player) {
            return player; // Splash-/Lingering-Potions
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player; // von einem Spieler gezündetes TNT
        }
        return null;
    }

    private void spawnLootHead(Player victim, Player killer, List<ItemStack> loot) {
        Block targetBlock = findPlaceableBlock(victim.getLocation());

        targetBlock.setType(Material.PLAYER_HEAD, false);
        BlockData blockData = targetBlock.getBlockData();
        if (blockData instanceof Rotatable rotatable) {
            rotatable.setRotation(yawToRotation(victim.getLocation().getYaw()));
            targetBlock.setBlockData(rotatable, false);
        }

        if (!(targetBlock.getState() instanceof Skull skull)) {
            plugin.getLogger().warning("Konnte Loot-Kopf an " + targetBlock.getLocation()
                    + " nicht erzeugen (kein Skull-BlockState).");
            return;
        }

        // Skin exakt vom Profil des Opfers übernehmen (funktioniert auch offline/cracked)
        skull.setOwnerProfile(victim.getPlayerProfile());

        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        pdc.set(PvPHeadsKeys.isLootHead(plugin), PersistentDataType.BYTE, (byte) 1);
        pdc.set(PvPHeadsKeys.lootData(plugin), PersistentDataType.STRING, ItemSerialization.serialize(loot));
        pdc.set(PvPHeadsKeys.ownerName(plugin), PersistentDataType.STRING, victim.getName());
        pdc.set(PvPHeadsKeys.ownerUuid(plugin), PersistentDataType.STRING, victim.getUniqueId().toString());
        pdc.set(PvPHeadsKeys.killerName(plugin), PersistentDataType.STRING, killer.getName());

        skull.update(true, false);
    }

    private Block findPlaceableBlock(Location deathLocation) {
        Block feet = deathLocation.getBlock();
        if (isReplaceable(feet)) {
            return feet;
        }
        Block above = feet.getRelative(BlockFace.UP);
        if (isReplaceable(above)) {
            return above;
        }
        // Fallback: erzwungen am Fuß-Block platzieren, falls kein freier Platz gefunden wurde
        return feet;
    }

    private boolean isReplaceable(Block block) {
        return block.getType().isAir() || block.isReplaceable();
    }

    private BlockFace yawToRotation(float yaw) {
        float normalized = (yaw % 360 + 360) % 360;
        int index = Math.round(normalized / 22.5f) % 16;
        return ROTATIONS[index];
    }
}
