package me.jackstar.drakestech.api.machine;

import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;

@FunctionalInterface
public interface MachineSupplier {
    AbstractMachine create(Location location);
}
