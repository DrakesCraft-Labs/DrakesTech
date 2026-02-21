package me.jackstar.drakestech.api.machine;

import me.jackstar.drakestech.machines.AbstractMachine;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MachineDefinition {

    private final String id;
    private final String moduleId;
    private final String displayName;
    private final List<String> description;
    private final List<String> recipeLines;
    private final ItemStack machineItem;
    private final MachineSupplier supplier;

    public MachineDefinition(String id,
            String moduleId,
            String displayName,
            List<String> description,
            List<String> recipeLines,
            ItemStack machineItem,
            MachineSupplier supplier) {
        this.id = normalize(id);
        this.moduleId = normalize(moduleId);
        this.displayName = displayName == null ? id : displayName;
        this.description = description == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(description));
        this.recipeLines = recipeLines == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(recipeLines));
        this.machineItem = machineItem == null ? null : machineItem.clone();
        this.supplier = supplier;
    }

    public String getId() {
        return id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getRecipeLines() {
        return recipeLines;
    }

    public AbstractMachine createMachine(Location location) {
        if (supplier == null) {
            return null;
        }
        return supplier.create(location);
    }

    public ItemStack createMachineItem() {
        return machineItem == null ? null : machineItem.clone();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
