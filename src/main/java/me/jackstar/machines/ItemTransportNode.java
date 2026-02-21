package me.jackstar.drakestech.machines;

import org.bukkit.inventory.ItemStack;

public interface ItemTransportNode {

    int[] getInputSlots();

    int[] getOutputSlots();

    default boolean canAcceptInput(int slot, ItemStack stack) {
        return stack != null && !stack.getType().isAir();
    }
}
