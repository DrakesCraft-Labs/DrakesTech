package me.jackstar.drakestech.multiblock;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockService {

    private final JavaPlugin plugin;
    private final MachineFactory machineFactory;
    private final MachineManager machineManager;
    private final MultiblockDetector detector = new MultiblockDetector();
    private final File contentFile;
    private final Map<String, MultiblockDefinition> definitions = new ConcurrentHashMap<>();

    public MultiblockService(JavaPlugin plugin, MachineFactory machineFactory, MachineManager machineManager) {
        this.plugin = plugin;
        this.machineFactory = machineFactory;
        this.machineManager = machineManager;
        this.contentFile = new File(plugin.getDataFolder(), "tech-content.yml");
    }

    public void reload() {
        definitions.clear();

        FileConfiguration config = YamlConfiguration.loadConfiguration(contentFile);
        ConfigurationSection multiblocksSection = config.getConfigurationSection("multiblocks");
        if (multiblocksSection == null) {
            return;
        }

        for (String id : multiblocksSection.getKeys(false)) {
            ConfigurationSection section = multiblocksSection.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }

            String machineId = normalize(section.getString("machine-id"));
            Material centerMaterial = detector.parseMaterial(section.getString("center"));
            if (machineId == null || centerMaterial == null || centerMaterial.isAir()) {
                plugin.getLogger().warning("[Multiblock] '" + id + "' is missing machine-id or valid center material.");
                continue;
            }

            boolean autoAssemble = section.getBoolean("auto-assemble-on-place", true);
            boolean consumeComponents = section.getBoolean("consume-components", true);
            List<String> partLines = section.getStringList("parts");
            Map<Vector, Material> parts = parseParts(id, partLines);
            if (parts.isEmpty()) {
                plugin.getLogger().warning("[Multiblock] '" + id + "' has no valid parts.");
                continue;
            }

            MultiblockDefinition definition = new MultiblockDefinition(
                    id,
                    machineId,
                    centerMaterial,
                    autoAssemble,
                    consumeComponents,
                    parts);
            definitions.put(definition.id(), definition);
        }

        plugin.getLogger().info("[Multiblock] Loaded structures: " + definitions.size());
    }

    public Collection<MultiblockDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<String> getDefinitionIds() {
        List<String> ids = new ArrayList<>(definitions.keySet());
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    public boolean tryAutoAssembleAround(Location changedBlock, Player player) {
        if (changedBlock == null || changedBlock.getWorld() == null || definitions.isEmpty()) {
            return false;
        }

        List<Location> candidateCenters = new ArrayList<>();
        candidateCenters.add(changedBlock.getBlock().getLocation());
        for (BlockFace face : new BlockFace[] {
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        }) {
            candidateCenters.add(changedBlock.getBlock().getRelative(face).getLocation());
        }

        for (Location center : candidateCenters) {
            if (machineManager.getMachineAt(center).isPresent()) {
                continue;
            }
            Block centerBlock = center.getBlock();

            for (MultiblockDefinition definition : definitions.values()) {
                if (!definition.autoAssemble()) {
                    continue;
                }
                if (centerBlock.getType() != definition.centerMaterial()) {
                    continue;
                }
                if (machineFactory.findDefinition(definition.machineId()).isEmpty()) {
                    continue;
                }

                OptionalInt rotation = detector.findMatchingRotation(center, definition.parts());
                if (rotation.isEmpty()) {
                    continue;
                }

                if (definition.consumeComponents()) {
                    consumeParts(center, definition, rotation.getAsInt());
                }

                boolean assembled = machineFactory.createMachine(definition.machineId(), center)
                        .map(machine -> {
                            machineManager.registerMachine(machine);
                            return true;
                        })
                        .orElse(false);
                if (!assembled) {
                    continue;
                }

                emitAssembleFeedback(center, player, definition);
                return true;
            }
        }

        return false;
    }

    private Map<Vector, Material> parseParts(String id, List<String> lines) {
        Map<Vector, Material> parts = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            int separator = line.indexOf(':');
            if (separator <= 0 || separator >= line.length() - 1) {
                plugin.getLogger().warning("[Multiblock] Invalid part line in '" + id + "': " + line);
                continue;
            }

            String vectorPart = line.substring(0, separator).trim();
            String materialPart = line.substring(separator + 1).trim();
            Vector offset = parseVector(vectorPart);
            Material material = detector.parseMaterial(materialPart);
            if (offset == null || material == null || material.isAir()) {
                plugin.getLogger().warning("[Multiblock] Invalid part entry in '" + id + "': " + line);
                continue;
            }
            parts.put(offset, material);
        }
        return parts;
    }

    private Vector parseVector(String value) {
        String[] split = value.split(",");
        if (split.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(split[0].trim());
            int y = Integer.parseInt(split[1].trim());
            int z = Integer.parseInt(split[2].trim());
            return new Vector(x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void consumeParts(Location center, MultiblockDefinition definition, int rotation) {
        for (Vector rawOffset : definition.parts().keySet()) {
            Vector rotated = detector.rotate(rawOffset, rotation);
            Location location = center.clone().add(rotated.getX(), rotated.getY(), rotated.getZ());
            if (location.getBlockX() == center.getBlockX()
                    && location.getBlockY() == center.getBlockY()
                    && location.getBlockZ() == center.getBlockZ()) {
                continue;
            }
            location.getBlock().setType(Material.AIR, false);
        }
    }

    private void emitAssembleFeedback(Location center, Player player, MultiblockDefinition definition) {
        Location effect = center.clone().add(0.5, 1.0, 0.5);
        if (effect.getWorld() != null) {
            effect.getWorld().spawnParticle(Particle.ENCHANT, effect, 20, 0.35, 0.5, 0.35, 0.01);
            effect.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, effect, 8, 0.25, 0.25, 0.25, 0.01);
        }
        if (player != null) {
            MessageUtils.send(player, "<green>Multiblock assembled:</green> <yellow>" + definition.id()
                    + "</yellow> <gray>-></gray> <aqua>" + definition.machineId() + "</aqua>");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
