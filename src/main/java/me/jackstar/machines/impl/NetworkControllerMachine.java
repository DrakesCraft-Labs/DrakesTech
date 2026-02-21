package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;

public class NetworkControllerMachine extends AbstractMachine {

    public NetworkControllerMachine(Location location) {
        super("network_controller", location);
    }

    @Override
    public void tick() {
        // Network topology is handled by TechNetworkService.
    }
}

