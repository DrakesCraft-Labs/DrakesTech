package me.jackstar.drakestech.nbt;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface NbtItemHandler {

    ItemStack setCustomItemId(ItemStack item, String customItemId);

    Optional<String> getCustomItemId(ItemStack item);

    boolean hasCustomItemId(ItemStack item, String customItemId);

    ItemStack setMachineId(ItemStack item, String machineId);

    Optional<String> getMachineId(ItemStack item);

    ItemStack clearTechTags(ItemStack item);
}
