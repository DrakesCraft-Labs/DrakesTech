package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.energy.EnergyNode;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.ItemTransportNode;
import me.jackstar.drakestech.recipe.TechRecipeEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ElectricFurnace extends AbstractMachine implements EnergyNode, ItemTransportNode {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int TICKS_PER_SMELT = 20;
    private static final double ENERGY_PER_SMELT = 50.0D;
    private static final double MAX_ENERGY = 5_000.0D;

    private final Inventory inventory;
    private final TechRecipeEngine recipeEngine;
    private double storedEnergy;
    private int progressTicks;

    public ElectricFurnace(Location location, TechRecipeEngine recipeEngine) {
        super("electric_furnace", location);
        this.inventory = Bukkit.createInventory(this, 9, "Electric Furnace");
        this.recipeEngine = recipeEngine;
    }

    @Override
    public void tick() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            progressTicks = 0;
            return;
        }

        ItemStack result = recipeEngine.resolveSmeltingResult(input).orElse(null);
        if (result == null || result.getType().isAir()) {
            progressTicks = 0;
            return;
        }

        if (!canOutput(result, output)) {
            progressTicks = 0;
            return;
        }

        if (storedEnergy < ENERGY_PER_SMELT) {
            return;
        }

        progressTicks++;
        if (progressTicks < TICKS_PER_SMELT) {
            return;
        }
        progressTicks = 0;

        double used = extractEnergy(ENERGY_PER_SMELT);
        if (used < ENERGY_PER_SMELT) {
            receiveEnergy(used);
            return;
        }

        input.setAmount(input.getAmount() - 1);
        if (input.getAmount() <= 0) {
            inventory.setItem(INPUT_SLOT, null);
        }

        if (output == null || output.getType().isAir()) {
            inventory.setItem(OUTPUT_SLOT, result.clone());
        } else {
            output.setAmount(output.getAmount() + result.getAmount());
            inventory.setItem(OUTPUT_SLOT, output);
        }

        Location effectLocation = getLocation().clone().add(0.5, 1.0, 0.5);
        if (effectLocation.getWorld() != null) {
            effectLocation.getWorld().spawnParticle(Particle.SMOKE, effectLocation, 6, 0.2, 0.2, 0.2, 0.01);
            effectLocation.getWorld().spawnParticle(Particle.FLAME, effectLocation, 2, 0.1, 0.1, 0.1, 0.0);
        }
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
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    private boolean canOutput(ItemStack result, ItemStack output) {
        if (output == null || output.getType().isAir()) {
            return true;
        }
        if (output.getType() != result.getType()) {
            return false;
        }
        return output.getAmount() + result.getAmount() <= output.getMaxStackSize();
    }

    @Override
    public int[] getInputSlots() {
        return new int[] { INPUT_SLOT };
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] { OUTPUT_SLOT };
    }

    @Override
    public boolean canAcceptInput(int slot, ItemStack stack) {
        if (slot != INPUT_SLOT || stack == null || stack.getType().isAir()) {
            return false;
        }
        return recipeEngine.resolveSmeltingResult(stack).isPresent();
    }
}
