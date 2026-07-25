package de.gamematex.pvpheads.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Wandelt Listen von ItemStacks (Loot) in einen Base64-String um, damit sie
 * in einer PersistentDataContainer (STRING) auf Blöcken oder Items gespeichert
 * werden können, und wieder zurück.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static String serialize(List<ItemStack> items) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(byteOut)) {

            dataOut.writeInt(items.size());
            for (ItemStack item : items) {
                dataOut.writeObject(item);
            }
            dataOut.flush();
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Konnte Loot nicht serialisieren.", e);
        }
    }

    public static List<ItemStack> deserialize(String base64) {
        List<ItemStack> items = new ArrayList<>();
        if (base64 == null || base64.isEmpty()) {
            return items;
        }
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataIn = new BukkitObjectInputStream(byteIn)) {

            int size = dataIn.readInt();
            for (int i = 0; i < size; i++) {
                Object obj = dataIn.readObject();
                if (obj instanceof ItemStack stack) {
                    items.add(stack);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Konnte Loot nicht deserialisieren.", e);
        }
        return items;
    }
}
