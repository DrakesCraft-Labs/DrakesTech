package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.ItemTransportNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class NetworkImportBusMachine extends AbstractMachine implements ItemTransportNode {

    private static final int[] INPUT_SLOTS = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };

    private final Inventory inventory;

    public NetworkImportBusMachine(Location location) {
        super("network_import_bus", location);
        this.inventory = Bukkit.createInventory(this, 9, "Network Import Bus");
    }

    @Override
    public void tick() {
        // Pulled by TechNetworkService.
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[0];
    }

    @Override
    public boolean canAcceptInput(int slot, ItemStack stack) {
        return stack != null && !stack.getType().isAir();
    }
}

