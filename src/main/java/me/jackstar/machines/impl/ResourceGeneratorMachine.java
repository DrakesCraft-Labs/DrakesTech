package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.energy.EnergyNode;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.ItemTransportNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ResourceGeneratorMachine extends AbstractMachine implements EnergyNode, ItemTransportNode {

    private static final int OUTPUT_SLOT = 0;

    private final Inventory inventory;
    private final Material outputMaterial;
    private final int outputAmount;
    private final int ticksPerCycle;
    private final double energyPerCycle;
    private final double maxEnergy;

    private double storedEnergy;
    private int progressTicks;

    public ResourceGeneratorMachine(String machineId,
            Location location,
            Material outputMaterial,
            int outputAmount,
            int ticksPerCycle,
            double energyPerCycle,
            double maxEnergy) {
        super(machineId, location);
        this.outputMaterial = outputMaterial == null ? Material.COBBLESTONE : outputMaterial;
        this.outputAmount = Math.max(1, outputAmount);
        this.ticksPerCycle = Math.max(1, ticksPerCycle);
        this.energyPerCycle = Math.max(0.0D, energyPerCycle);
        this.maxEnergy = Math.max(100.0D, maxEnergy);
        this.inventory = Bukkit.createInventory(this, 9, machineId + " Generator");
    }

    @Override
    public void tick() {
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (!canOutput(output)) {
            progressTicks = 0;
            return;
        }
        if (storedEnergy < energyPerCycle) {
            return;
        }

        progressTicks++;
        if (progressTicks < ticksPerCycle) {
            return;
        }
        progressTicks = 0;

        double used = extractEnergy(energyPerCycle);
        if (used < energyPerCycle) {
            receiveEnergy(used);
            return;
        }

        if (output == null || output.getType().isAir()) {
            inventory.setItem(OUTPUT_SLOT, new ItemStack(outputMaterial, outputAmount));
            return;
        }
        output.setAmount(Math.min(output.getMaxStackSize(), output.getAmount() + outputAmount));
        inventory.setItem(OUTPUT_SLOT, output);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public double getStoredEnergy() {
        return storedEnergy;
    }

    @Override
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public void receiveEnergy(double amount) {
        if (amount <= 0) {
            return;
        }
        storedEnergy = Math.min(maxEnergy, storedEnergy + amount);
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
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public int[] getInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] { OUTPUT_SLOT };
    }

    private boolean canOutput(ItemStack output) {
        if (output == null || output.getType().isAir()) {
            return true;
        }
        if (output.getType() != outputMaterial) {
            return false;
        }
        return output.getAmount() + outputAmount <= output.getMaxStackSize();
    }
}
