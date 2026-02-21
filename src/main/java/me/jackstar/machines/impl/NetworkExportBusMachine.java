package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.ItemTransportNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NetworkExportBusMachine extends AbstractMachine implements ItemTransportNode {

    public static final int TEMPLATE_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private static final int[] OUTPUT_SLOTS = new int[] { OUTPUT_SLOT };

    private final Inventory inventory;
    private final int maxItemsPerCycle;

    public NetworkExportBusMachine(Location location, int maxItemsPerCycle) {
        super("network_export_bus", location);
        this.inventory = Bukkit.createInventory(this, 9, "Network Export Bus");
        this.maxItemsPerCycle = Math.max(1, maxItemsPerCycle);
    }

    @Override
    public void tick() {
        // Filled by TechNetworkService.
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public int[] getInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canAcceptInput(int slot, ItemStack stack) {
        return false;
    }

    public int getMaxItemsPerCycle() {
        return maxItemsPerCycle;
    }
}

