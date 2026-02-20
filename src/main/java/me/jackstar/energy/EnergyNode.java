package me.jackstar.drakestech.energy;

import org.bukkit.Location;

public interface EnergyNode {
    Location getLocation();

    double getStoredEnergy();

    double getMaxEnergy();

    void receiveEnergy(double amount);

    double extractEnergy(double maxAmount);

    boolean canReceive();

    boolean canExtract();
}
