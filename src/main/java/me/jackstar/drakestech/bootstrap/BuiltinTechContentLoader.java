package me.jackstar.drakestech.bootstrap;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.api.enchant.TechEnchantmentDefinition;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.api.machine.MachineDefinition;
import me.jackstar.drakestech.machines.impl.ElectricFurnace;
import me.jackstar.drakestech.machines.impl.SolarGenerator;
import me.jackstar.drakestech.recipe.TechRecipeEngine;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class BuiltinTechContentLoader {

    private static final String CONTENT_FILE_NAME = "tech-content.yml";

    private BuiltinTechContentLoader() {
    }

    public static void registerDefaults(JavaPlugin plugin, DrakesTechApi api, TechRecipeEngine recipeEngine) {
        ensureContentFile(plugin);
        File contentFile = new File(plugin.getDataFolder(), CONTENT_FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(contentFile);

        registerModules(plugin, api, config.getConfigurationSection("modules"));
        registerEnchantments(plugin, api, config.getConfigurationSection("enchantments"));
        registerMachines(plugin, api, recipeEngine, config.getConfigurationSection("machines"));
        registerGuideEntries(plugin, api, config.getConfigurationSection("entries"));
        registerEnchantmentGuideEntries(plugin, api, config.getBoolean("guide.auto-create-enchantment-entries", true));
    }

    private static void registerModules(JavaPlugin plugin, DrakesTechApi api, ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("No 'modules' section found in " + CONTENT_FILE_NAME + ".");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection moduleSection = section.getConfigurationSection(id);
            if (moduleSection == null || !moduleSection.getBoolean("enabled", true)) {
                continue;
            }

            String displayName = moduleSection.getString("display-name", fallbackTitle(id));
            Material icon = parseMaterial(moduleSection.getString("icon"), Material.BOOK, plugin, "module " + id);
            List<String> description = readStringList(moduleSection, "description");
            if (description.isEmpty()) {
                description = List.of("<gray>No description.</gray>");
            }

            boolean ok = api.registerGuideModule(plugin, new TechGuideModule(id, displayName, icon, description));
            if (!ok) {
                plugin.getLogger().warning("Failed to register guide module '" + id + "'.");
            }
        }
    }

    private static void registerMachines(JavaPlugin plugin, DrakesTechApi api, TechRecipeEngine recipeEngine,
            ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("No 'machines' section found in " + CONTENT_FILE_NAME + ".");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection machineSection = section.getConfigurationSection(id);
            if (machineSection == null || !machineSection.getBoolean("enabled", true)) {
                continue;
            }

            String template = normalize(machineSection.getString("template"));
            if (template == null) {
                plugin.getLogger().warning("Machine '" + id + "' has no template. Skipping.");
                continue;
            }

            String moduleId = machineSection.getString("module", "machines");
            String displayName = machineSection.getString("display-name", fallbackTitle(id));
            List<String> description = readStringList(machineSection, "description");
            List<String> recipe = readStringList(machineSection, "recipe");
            Material icon = parseMaterial(machineSection.getString("icon"), Material.OBSERVER, plugin, "machine " + id);
            ItemStack machineItem = new ItemBuilder(icon)
                    .name(displayName)
                    .lore(description)
                    .build();

            MachineDefinition definition = switch (template) {
                case "solar_generator" -> new MachineDefinition(
                        id,
                        moduleId,
                        displayName,
                        description,
                        recipe,
                        machineItem,
                        SolarGenerator::new);
                case "electric_furnace" -> new MachineDefinition(
                        id,
                        moduleId,
                        displayName,
                        description,
                        recipe,
                        machineItem,
                        location -> new ElectricFurnace(location, recipeEngine));
                default -> null;
            };

            if (definition == null) {
                plugin.getLogger().warning("Machine '" + id + "' uses unknown template '" + template + "'. Skipping.");
                continue;
            }

            boolean ok = api.registerMachine(plugin, definition);
            if (!ok) {
                plugin.getLogger().warning("Failed to register machine '" + id + "'.");
            }
        }
    }

    private static void registerEnchantments(JavaPlugin plugin, DrakesTechApi api, ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("No 'enchantments' section found in " + CONTENT_FILE_NAME + ".");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection enchantSection = section.getConfigurationSection(id);
            if (enchantSection == null || !enchantSection.getBoolean("enabled", true)) {
                continue;
            }

            String displayName = enchantSection.getString("display-name", fallbackTitle(id));
            int maxLevel = Math.max(1, enchantSection.getInt("max-level", 1));
            List<String> description = readStringList(enchantSection, "description");
            if (description.isEmpty()) {
                description = List.of("<gray>No description.</gray>");
            }

            boolean ok = api.registerEnchantment(plugin, new TechEnchantmentDefinition(id, displayName, maxLevel, description));
            if (!ok) {
                plugin.getLogger().warning("Failed to register enchantment '" + id + "'.");
            }
        }
    }

    private static void registerGuideEntries(JavaPlugin plugin, DrakesTechApi api, ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("No 'entries' section found in " + CONTENT_FILE_NAME + ".");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(id);
            if (entrySection == null || !entrySection.getBoolean("enabled", true)) {
                continue;
            }

            String module = entrySection.getString("module");
            if (module == null || module.isBlank()) {
                plugin.getLogger().warning("Guide entry '" + id + "' has no module. Skipping.");
                continue;
            }

            String displayName = entrySection.getString("display-name", fallbackTitle(id));
            Material icon = parseMaterial(entrySection.getString("icon"), Material.PAPER, plugin, "entry " + id);
            List<String> description = readStringList(entrySection, "description");
            List<String> recipe = readStringList(entrySection, "recipe");
            Material previewMaterial = parseMaterial(entrySection.getString("preview-material"), icon, plugin, "entry preview " + id);

            ItemStack previewItem = new ItemBuilder(previewMaterial)
                    .name(displayName)
                    .lore(description)
                    .build();

            TechGuideEntry entry = new TechGuideEntry(id, module, displayName, icon, description, recipe, previewItem);
            boolean ok = api.registerGuideEntry(plugin, entry);
            if (!ok) {
                plugin.getLogger().warning("Failed to register guide entry '" + id + "'.");
            }
        }
    }

    private static void registerEnchantmentGuideEntries(JavaPlugin plugin, DrakesTechApi api, boolean enabled) {
        if (!enabled) {
            return;
        }

        Collection<TechEnchantmentDefinition> enchantments = api.getEnchantments();
        for (TechEnchantmentDefinition enchantment : enchantments) {
            TechGuideEntry entry = new TechGuideEntry(
                    "enchant_" + enchantment.getId(),
                    "enchantments",
                    enchantment.getDisplayName(),
                    Material.ENCHANTED_BOOK,
                    enchantment.getDescription(),
                    List.of("<gray>Max level:</gray> <yellow>" + enchantment.getMaxLevel() + "</yellow>"),
                    new ItemBuilder(Material.ENCHANTED_BOOK)
                            .name(enchantment.getDisplayName())
                            .lore("<gray>Custom enchantment entry.</gray>")
                            .build());

            api.registerGuideEntry(plugin, entry);
        }
    }

    private static void ensureContentFile(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for " + CONTENT_FILE_NAME + ".");
            return;
        }

        File file = new File(dataFolder, CONTENT_FILE_NAME);
        if (file.exists()) {
            return;
        }

        if (plugin.getResource(CONTENT_FILE_NAME) != null) {
            plugin.saveResource(CONTENT_FILE_NAME, false);
        } else {
            plugin.getLogger().warning("Resource '" + CONTENT_FILE_NAME + "' was not found inside jar.");
        }
    }

    private static List<String> readStringList(ConfigurationSection section, String path) {
        if (section == null) {
            return List.of();
        }

        List<String> values = section.getStringList(path);
        List<String> clean = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                clean.add(trimmed);
            }
        }
        return List.copyOf(clean);
    }

    private static Material parseMaterial(String rawValue, Material fallback, JavaPlugin plugin, String context) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(rawValue.trim(), true);
        if (material == null) {
            plugin.getLogger().warning("Invalid material '" + rawValue + "' in " + context + ". Using " + fallback + ".");
            return fallback;
        }
        return material;
    }

    private static String fallbackTitle(String id) {
        String normalized = normalize(id);
        if (normalized == null) {
            return "<yellow><b>Unknown</b></yellow>";
        }

        String[] split = normalized.split("_");
        List<String> words = new ArrayList<>();
        for (String part : split) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        if (words.isEmpty()) {
            return "<yellow><b>Unknown</b></yellow>";
        }
        return "<yellow><b>" + String.join(" ", words) + "</b></yellow>";
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
