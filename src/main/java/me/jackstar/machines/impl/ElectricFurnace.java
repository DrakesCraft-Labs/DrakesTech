package me.jackstar.drakestech.machines.impl;

import me.jackstar.drakestech.energy.EnergyNode;
import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ElectricFurnace extends AbstractMachine implements EnergyNode {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int TICKS_PER_SMELT = 20;
    private static final double ENERGY_PER_SMELT = 50.0D;
    private static final double MAX_ENERGY = 5_000.0D;

    private static final Map<Material, Material> SIMPLE_RECIPES = new HashMap<>();

    static {
        SIMPLE_RECIPES.put(Material.IRON_ORE, Material.IRON_INGOT);
        SIMPLE_RECIPES.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SIMPLE_RECIPES.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SIMPLE_RECIPES.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SIMPLE_RECIPES.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SIMPLE_RECIPES.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SIMPLE_RECIPES.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SIMPLE_RECIPES.put(Material.SAND, Material.GLASS);
        SIMPLE_RECIPES.put(Material.COBBLESTONE, Material.STONE);
        SIMPLE_RECIPES.put(Material.BEEF, Material.COOKED_BEEF);
        SIMPLE_RECIPES.put(Material.CHICKEN, Material.COOKED_CHICKEN);
        SIMPLE_RECIPES.put(Material.MUTTON, Material.COOKED_MUTTON);
        SIMPLE_RECIPES.put(Material.PORKCHOP, Material.COOKED_PORKCHOP);
        SIMPLE_RECIPES.put(Material.POTATO, Material.BAKED_POTATO);
    }

    private final Inventory inventory;
    private double storedEnergy;
    private int progressTicks;

    public ElectricFurnace(Location location) {
        super("electric_furnace", location);
        this.inventory = Bukkit.createInventory(this, 9, "Electric Furnace");
    }

    @Override
    public void tick() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            progressTicks = 0;
            return;
        }

        Material resultType = SIMPLE_RECIPES.get(input.getType());
        if (resultType == null) {
            progressTicks = 0;
            return;
        }

        if (!canOutput(resultType, output)) {
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
            inventory.setItem(OUTPUT_SLOT, new ItemStack(resultType, 1));
        } else {
            output.setAmount(output.getAmount() + 1);
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

    private boolean canOutput(Material resultType, ItemStack output) {
        if (output == null || output.getType().isAir()) {
            return true;
        }
        if (output.getType() != resultType) {
            return false;
        }
        return output.getAmount() < output.getMaxStackSize();
    }
}
