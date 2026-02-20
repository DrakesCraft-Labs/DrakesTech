package me.jackstar.drakestech.nbt;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

public class PdcNbtItemHandler implements NbtItemHandler {

    private final NamespacedKey customItemIdKey;
    private final NamespacedKey machineIdKey;

    public PdcNbtItemHandler(JavaPlugin plugin) {
        this.customItemIdKey = new NamespacedKey(plugin, "drakestech_custom_item_id");
        this.machineIdKey = new NamespacedKey(plugin, "drakestech_machine_id");
    }

    @Override
    public ItemStack setCustomItemId(ItemStack item, String customItemId) {
        return setStringTag(item, customItemIdKey, normalize(customItemId));
    }

    @Override
    public Optional<String> getCustomItemId(ItemStack item) {
        return getStringTag(item, customItemIdKey);
    }

    @Override
    public boolean hasCustomItemId(ItemStack item, String customItemId) {
        return getCustomItemId(item).map(id -> id.equals(normalize(customItemId))).orElse(false);
    }

    @Override
    public ItemStack setMachineId(ItemStack item, String machineId) {
        return setStringTag(item, machineIdKey, normalize(machineId));
    }

    @Override
    public Optional<String> getMachineId(ItemStack item) {
        return getStringTag(item, machineIdKey);
    }

    @Override
    public ItemStack clearTechTags(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(customItemIdKey);
        pdc.remove(machineIdKey);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack setStringTag(ItemStack item, NamespacedKey key, String value) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (value == null) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.STRING, value);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Optional<String> getStringTag(ItemStack item, NamespacedKey key) {
        if (item == null) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String value = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return Optional.ofNullable(value);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
