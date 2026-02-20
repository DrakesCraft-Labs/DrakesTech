package me.jackstar.drakestech.machines;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class AbstractMachine implements InventoryHolder {

    private final Location location;
    private final String id;

    public AbstractMachine(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public abstract void tick(); // Called every server tick/second

    public Location getLocation() {
        return location;
    }

    public String getId() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return null; // Override if machine has inventory
    }
}
