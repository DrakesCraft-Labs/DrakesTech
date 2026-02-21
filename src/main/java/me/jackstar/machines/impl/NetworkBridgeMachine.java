package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;

public class NetworkBridgeMachine extends AbstractMachine {

    public NetworkBridgeMachine(Location location) {
        super("network_bridge", location);
    }

    @Override
    public void tick() {
        // Passive network connectivity node.
    }
}

