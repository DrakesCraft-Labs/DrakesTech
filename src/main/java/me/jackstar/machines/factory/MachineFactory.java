package me.jackstar.drakestech.machines.factory;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.impl.ElectricFurnace;
import me.jackstar.drakestech.machines.impl.SolarGenerator;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MachineFactory {

    public static final String SOLAR_GENERATOR_ID = "solar_generator";
    public static final String ELECTRIC_FURNACE_ID = "electric_furnace";
    private static final String MACHINE_CUSTOM_ITEM_MARKER = "drakestech_machine";

    private final NbtItemHandler nbtItemHandler;

    public MachineFactory(NbtItemHandler nbtItemHandler) {
        this.nbtItemHandler = nbtItemHandler;
    }

    public List<String> getSupportedMachineIds() {
        return List.of(SOLAR_GENERATOR_ID, ELECTRIC_FURNACE_ID);
    }

    public Optional<AbstractMachine> createMachine(String machineId, Location location) {
        String normalized = normalize(machineId);
        if (normalized == null || location == null) {
            return Optional.empty();
        }

        return switch (normalized) {
            case SOLAR_GENERATOR_ID -> Optional.of(new SolarGenerator(location));
            case ELECTRIC_FURNACE_ID -> Optional.of(new ElectricFurnace(location));
            default -> Optional.empty();
        };
    }

    public Optional<ItemStack> createMachineItem(String machineId) {
        String normalized = normalize(machineId);
        if (normalized == null) {
            return Optional.empty();
        }

        ItemStack base;
        switch (normalized) {
            case SOLAR_GENERATOR_ID -> base = new ItemBuilder(Material.DAYLIGHT_DETECTOR)
                    .name("<gold><b>Solar Generator</b></gold>")
                    .lore("<gray>Generates <yellow>10 J/t</yellow> during clear day.")
                    .build();
            case ELECTRIC_FURNACE_ID -> base = new ItemBuilder(Material.FURNACE)
                    .name("<aqua><b>Electric Furnace</b></aqua>")
                    .lore("<gray>Consumes <yellow>50 J</yellow> to smelt items.")
                    .build();
            default -> {
                return Optional.empty();
            }
        }

        nbtItemHandler.setCustomItemId(base, MACHINE_CUSTOM_ITEM_MARKER);
        nbtItemHandler.setMachineId(base, normalized);
        return Optional.of(base);
    }

    public boolean isMachineItem(ItemStack stack) {
        return nbtItemHandler.hasCustomItemId(stack, MACHINE_CUSTOM_ITEM_MARKER) && nbtItemHandler.getMachineId(stack).isPresent();
    }

    public Optional<String> readMachineId(ItemStack stack) {
        if (!isMachineItem(stack)) {
            return Optional.empty();
        }
        return nbtItemHandler.getMachineId(stack).map(this::normalize);
    }

    private String normalize(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
