package me.jackstar.drakestech.machines.factory;

import me.jackstar.drakestech.api.machine.MachineDefinition;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MachineFactory {

    private static final String MACHINE_CUSTOM_ITEM_MARKER = "drakestech_machine";

    private final NbtItemHandler nbtItemHandler;
    private final Map<String, MachineDefinition> definitions = new ConcurrentHashMap<>();

    public MachineFactory(NbtItemHandler nbtItemHandler) {
        this.nbtItemHandler = nbtItemHandler;
    }

    public boolean registerMachineDefinition(MachineDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return false;
        }
        String key = normalize(definition.getId());
        return definitions.putIfAbsent(key, definition) == null;
    }

    public boolean unregisterMachineDefinition(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return false;
        }
        return definitions.remove(key) != null;
    }

    public Optional<MachineDefinition> findDefinition(String machineId) {
        return Optional.ofNullable(definitions.get(normalize(machineId)));
    }

    public Collection<MachineDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<String> getSupportedMachineIds() {
        List<String> ids = new ArrayList<>(definitions.keySet());
        ids.sort(String::compareTo);
        return ids;
    }

    public Optional<AbstractMachine> createMachine(String machineId, Location location) {
        String normalized = normalize(machineId);
        if (normalized == null || location == null) {
            return Optional.empty();
        }

        MachineDefinition definition = definitions.get(normalized);
        if (definition == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(definition.createMachine(location));
    }

    public Optional<ItemStack> createMachineItem(String machineId) {
        MachineDefinition definition = definitions.get(normalize(machineId));
        if (definition == null) {
            return Optional.empty();
        }

        ItemStack base = definition.createMachineItem();
        nbtItemHandler.setCustomItemId(base, MACHINE_CUSTOM_ITEM_MARKER);
        nbtItemHandler.setMachineId(base, definition.getId());
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
