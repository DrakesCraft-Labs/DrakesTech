package me.jackstar.drakestech.multiblock;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record MultiblockDefinition(
        String id,
        String machineId,
        Material centerMaterial,
        boolean autoAssemble,
        boolean consumeComponents,
        Map<Vector, Material> parts) {

    public MultiblockDefinition {
        id = normalize(id);
        machineId = normalize(machineId);
        parts = parts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parts));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
