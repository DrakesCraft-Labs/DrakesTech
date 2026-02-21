package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.ItemTransportNode;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TechStorageChestMachine extends AbstractMachine implements ItemTransportNode {

    private static final String TECH_ITEM_MARKER = "drakestech_item";
    private static final String MACHINE_ITEM_MARKER = "drakestech_machine";

    private final Inventory inventory;
    private final NbtItemHandler nbtItemHandler;
    private final boolean onlyPluginItems;
    private final int[] transportSlots;

    public TechStorageChestMachine(Location location, int size, NbtItemHandler nbtItemHandler, boolean onlyPluginItems) {
        super("tech_storage_chest", location);
        int normalizedSize = normalizeSize(size);
        this.inventory = Bukkit.createInventory(this, normalizedSize, "Tech Storage Chest");
        this.nbtItemHandler = nbtItemHandler;
        this.onlyPluginItems = onlyPluginItems;
        this.transportSlots = buildTransportSlots(normalizedSize);
    }

    @Override
    public void tick() {
        // Passive storage machine. Transport is handled by MachineManager item network.
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public int[] getInputSlots() {
        return transportSlots;
    }

    @Override
    public int[] getOutputSlots() {
        return transportSlots;
    }

    @Override
    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        if (!onlyPluginItems) {
            return true;
        }
        return nbtItemHandler.hasCustomItemId(stack, TECH_ITEM_MARKER)
                || nbtItemHandler.hasCustomItemId(stack, MACHINE_ITEM_MARKER);
    }

    private int normalizeSize(int size) {
        int normalized = Math.max(9, size);
        normalized = (normalized / 9) * 9;
        if (normalized <= 0) {
            normalized = 9;
        }
        return Math.min(54, normalized);
    }

    private int[] buildTransportSlots(int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
