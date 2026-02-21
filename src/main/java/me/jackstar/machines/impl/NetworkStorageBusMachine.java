package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Set;
import java.util.Optional;

public class NetworkStorageBusMachine extends AbstractMachine {

    public NetworkStorageBusMachine(Location location) {
        super("network_storage_bus", location);
    }

    @Override
    public void tick() {
        // Passive bridge node for adjacent external inventory.
    }

    public Optional<Inventory> resolveTargetInventory(Set<Location> blockedMachineLocations) {
        if (getLocation() == null || getLocation().getWorld() == null) {
            return Optional.empty();
        }

        for (BlockFace face : new BlockFace[] {
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.SOUTH,
                BlockFace.WEST,
                BlockFace.UP,
                BlockFace.DOWN
        }) {
            Location targetLocation = normalize(getLocation().getBlock().getRelative(face).getLocation());
            if (blockedMachineLocations != null && blockedMachineLocations.contains(targetLocation)) {
                continue;
            }

            BlockState state = targetLocation.getBlock().getState();
            if (!(state instanceof InventoryHolder holder)) {
                continue;
            }

            Inventory inventory = holder.getInventory();
            if (inventory == null || inventory.getSize() <= 0) {
                continue;
            }

            return Optional.of(inventory);
        }

        return Optional.empty();
    }

    private Location normalize(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}

