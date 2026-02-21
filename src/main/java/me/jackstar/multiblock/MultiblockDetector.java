package me.jackstar.drakestech.multiblock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

public class MultiblockDetector {

    public boolean matches(Location center, Map<Vector, String> structureMap) {
        for (Map.Entry<Vector, String> entry : structureMap.entrySet()) {
            Vector offset = entry.getKey();
            String expectedMaterial = entry.getValue();

            Block block = center.clone().add(offset).getBlock();
            if (!block.getType().name().equalsIgnoreCase(expectedMaterial)) {
                return false;
            }
        }
        return true;
    }

    public OptionalInt findMatchingRotation(Location center, Map<Vector, Material> structureMap) {
        if (center == null || center.getWorld() == null || structureMap == null || structureMap.isEmpty()) {
            return OptionalInt.empty();
        }

        for (int rotation = 0; rotation < 4; rotation++) {
            if (matchesRotation(center, structureMap, rotation)) {
                return OptionalInt.of(rotation);
            }
        }

        return OptionalInt.empty();
    }

    public Vector rotate(Vector source, int quarterTurnsClockwise) {
        if (source == null) {
            return new Vector();
        }

        int turns = Math.floorMod(quarterTurnsClockwise, 4);
        int x = (int) Math.round(source.getX());
        int y = (int) Math.round(source.getY());
        int z = (int) Math.round(source.getZ());

        return switch (turns) {
            case 1 -> new Vector(-z, y, x);
            case 2 -> new Vector(-x, y, -z);
            case 3 -> new Vector(z, y, -x);
            default -> new Vector(x, y, z);
        };
    }

    private boolean matchesRotation(Location center, Map<Vector, Material> structureMap, int rotation) {
        for (Map.Entry<Vector, Material> entry : structureMap.entrySet()) {
            Material expected = entry.getValue();
            if (expected == null) {
                continue;
            }

            Vector rotated = rotate(entry.getKey(), rotation);
            Location location = center.clone().add(rotated.getX(), rotated.getY(), rotated.getZ());
            Material actual = location.getBlock().getType();
            if (actual != expected) {
                return false;
            }
        }
        return true;
    }

    public Material parseMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim();
        try {
            return Material.valueOf(clean.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Material.matchMaterial(clean);
        }
    }
}
