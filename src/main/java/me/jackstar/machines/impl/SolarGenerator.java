package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.energy.EnergyNode;
import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;
import org.bukkit.World;

public class SolarGenerator extends AbstractMachine implements EnergyNode {

    private static final double GENERATION_PER_TICK = 10.0D;
    private static final double MAX_ENERGY = 10_000.0D;

    private double storedEnergy;

    public SolarGenerator(Location location) {
        super("solar_generator", location);
    }

    @Override
    public void tick() {
        World world = getLocation().getWorld();
        if (world == null) {
            return;
        }

        long time = world.getTime();
        boolean day = time >= 0 && time < 12300;
        if (day && !world.hasStorm()) {
            receiveEnergy(GENERATION_PER_TICK);
        }
    }

    @Override
    public double getStoredEnergy() {
        return storedEnergy;
    }

    @Override
    public double getMaxEnergy() {
        return MAX_ENERGY;
    }

    @Override
    public void receiveEnergy(double amount) {
        if (amount <= 0) {
            return;
        }
        storedEnergy = Math.min(MAX_ENERGY, storedEnergy + amount);
    }

    @Override
    public double extractEnergy(double maxAmount) {
        if (maxAmount <= 0 || storedEnergy <= 0) {
            return 0;
        }
        double extracted = Math.min(maxAmount, storedEnergy);
        storedEnergy -= extracted;
        return extracted;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return true;
    }
}
