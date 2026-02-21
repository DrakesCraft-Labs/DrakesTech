package me.jackstar.drakestech.item;

import me.jackstar.drakestech.api.item.TechItemDefinition;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TechItemRegistry {

    private static final String TECH_ITEM_MARKER = "drakestech_item";

    private final NbtItemHandler nbtItemHandler;
    private final Map<String, TechItemDefinition> definitions = new ConcurrentHashMap<>();

    public TechItemRegistry(NbtItemHandler nbtItemHandler) {
        this.nbtItemHandler = nbtItemHandler;
    }

    public boolean register(TechItemDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return false;
        }
        String key = normalize(definition.getId());
        return definitions.putIfAbsent(key, definition) == null;
    }

    public boolean unregister(String itemId) {
        String key = normalize(itemId);
        if (key == null) {
            return false;
        }
        return definitions.remove(key) != null;
    }

    public Optional<TechItemDefinition> find(String itemId) {
        return Optional.ofNullable(definitions.get(normalize(itemId)));
    }

    public Collection<TechItemDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<String> getSupportedIds() {
        List<String> ids = new ArrayList<>(definitions.keySet());
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    public Optional<ItemStack> createItem(String itemId) {
        return createItem(itemId, 1);
    }

    public Optional<ItemStack> createItem(String itemId, int amount) {
        TechItemDefinition definition = definitions.get(normalize(itemId));
        if (definition == null) {
            return Optional.empty();
        }

        ItemStack stack = definition.createItem(amount);
        nbtItemHandler.setCustomItemId(stack, TECH_ITEM_MARKER);
        nbtItemHandler.setMachineId(stack, definition.getId());
        return Optional.of(stack);
    }

    public boolean isTechItem(ItemStack stack) {
        return nbtItemHandler.hasCustomItemId(stack, TECH_ITEM_MARKER) && nbtItemHandler.getMachineId(stack).isPresent();
    }

    public Optional<String> readTechItemId(ItemStack stack) {
        if (!isTechItem(stack)) {
            return Optional.empty();
        }
        return nbtItemHandler.getMachineId(stack).map(this::normalize);
    }

    public boolean matches(ItemStack stack, String itemId) {
        String target = normalize(itemId);
        if (target == null) {
            return false;
        }
        return readTechItemId(stack).map(target::equals).orElse(false);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
