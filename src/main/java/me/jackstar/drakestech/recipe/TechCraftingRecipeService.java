package me.jackstar.drakestech.recipe;

import me.jackstar.drakestech.item.TechItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TechCraftingRecipeService {

    private static final String ITEM_PREFIX = "item:";
    private static final String MATERIAL_PREFIX = "material:";

    private final JavaPlugin plugin;
    private final TechItemRegistry itemRegistry;
    private final File contentFile;
    private final Set<NamespacedKey> registeredRecipeKeys = new HashSet<>();

    public TechCraftingRecipeService(JavaPlugin plugin, TechItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.contentFile = new File(plugin.getDataFolder(), "tech-content.yml");
    }

    public synchronized void reload() {
        unregisterAll();

        FileConfiguration config = YamlConfiguration.loadConfiguration(contentFile);
        ConfigurationSection craftingSection = config.getConfigurationSection("recipes.crafting");
        if (craftingSection == null) {
            plugin.getLogger().info("[Crafting] No recipes.crafting section found.");
            return;
        }

        int loaded = 0;
        for (String id : craftingSection.getKeys(false)) {
            ConfigurationSection section = craftingSection.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, "dt_" + sanitizeKey(id));
            Bukkit.removeRecipe(key);

            String type = normalize(section.getString("type", "shaped"));
            boolean ok = switch (type) {
                case "shapeless" -> registerShapeless(key, id, section);
                case "shaped" -> registerShaped(key, id, section);
                default -> {
                    plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has unknown type '" + type + "'.");
                    yield false;
                }
            };
            if (ok) {
                registeredRecipeKeys.add(key);
                loaded++;
            }
        }

        plugin.getLogger().info("[Crafting] Loaded crafting recipes: " + loaded);
    }

    public synchronized int getRegisteredRecipeCount() {
        return registeredRecipeKeys.size();
    }

    private boolean registerShaped(NamespacedKey key, String id, ConfigurationSection section) {
        ItemStack output = resolveOutput(section.getString("output"), Math.max(1, section.getInt("amount", 1)));
        if (output == null || output.getType().isAir()) {
            plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has invalid output.");
            return false;
        }

        List<String> rawShape = section.getStringList("shape");
        String[] shape = normalizeShape(rawShape);
        if (shape.length == 0) {
            plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has empty shape.");
            return false;
        }

        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has no ingredients section.");
            return false;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, output);
        recipe.shape(shape);

        Set<Character> usedKeys = extractUsedKeys(shape);
        for (Character character : usedKeys) {
            String token = ingredientsSection.getString(String.valueOf(character));
            RecipeChoice choice = resolveIngredient(token);
            if (choice == null) {
                plugin.getLogger().warning("[Crafting] Recipe '" + id + "' missing ingredient mapping for '" + character + "'.");
                return false;
            }
            recipe.setIngredient(character, choice);
        }

        return Bukkit.addRecipe(recipe);
    }

    private boolean registerShapeless(NamespacedKey key, String id, ConfigurationSection section) {
        ItemStack output = resolveOutput(section.getString("output"), Math.max(1, section.getInt("amount", 1)));
        if (output == null || output.getType().isAir()) {
            plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has invalid output.");
            return false;
        }

        List<String> ingredients = section.getStringList("ingredients");
        if (ingredients.isEmpty()) {
            plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has no ingredients.");
            return false;
        }

        ShapelessRecipe recipe = new ShapelessRecipe(key, output);
        for (String ingredientToken : ingredients) {
            RecipeChoice choice = resolveIngredient(ingredientToken);
            if (choice == null) {
                plugin.getLogger().warning("[Crafting] Recipe '" + id + "' has invalid ingredient '" + ingredientToken + "'.");
                return false;
            }
            recipe.addIngredient(choice);
        }

        return Bukkit.addRecipe(recipe);
    }

    private String[] normalizeShape(List<String> rawShape) {
        if (rawShape == null || rawShape.isEmpty()) {
            return new String[0];
        }

        int rows = Math.min(3, rawShape.size());
        int width = 0;
        List<String> trimmedRows = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            String row = rawShape.get(i) == null ? "" : rawShape.get(i);
            if (row.length() > 3) {
                row = row.substring(0, 3);
            }
            trimmedRows.add(row);
            width = Math.max(width, row.length());
        }

        width = Math.max(1, Math.min(width, 3));
        String[] normalized = new String[rows];
        for (int i = 0; i < rows; i++) {
            String row = trimmedRows.get(i);
            if (row.length() < width) {
                row = row + " ".repeat(width - row.length());
            }
            normalized[i] = row;
        }
        return normalized;
    }

    private Set<Character> extractUsedKeys(String[] shape) {
        Set<Character> keys = new HashSet<>();
        for (String row : shape) {
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != ' ') {
                    keys.add(c);
                }
            }
        }
        return keys;
    }

    private ItemStack resolveOutput(String token, int amount) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String clean = token.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ITEM_PREFIX)) {
            String itemId = normalize(clean.substring(ITEM_PREFIX.length()));
            return itemId == null ? null : itemRegistry.createItem(itemId, amount).orElse(null);
        }
        if (lower.startsWith(MATERIAL_PREFIX)) {
            Material material = parseMaterial(clean.substring(MATERIAL_PREFIX.length()));
            return material == null ? null : new ItemStack(material, amount);
        }

        Material material = parseMaterial(clean);
        if (material != null) {
            return new ItemStack(material, amount);
        }

        String itemId = normalize(clean);
        if (itemId == null) {
            return null;
        }
        return itemRegistry.createItem(itemId, amount).orElse(null);
    }

    private RecipeChoice resolveIngredient(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String clean = token.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ITEM_PREFIX)) {
            String itemId = normalize(clean.substring(ITEM_PREFIX.length()));
            if (itemId == null) {
                return null;
            }
            return itemRegistry.createItem(itemId, 1)
                    .<RecipeChoice>map(item -> new RecipeChoice.ExactChoice(item))
                    .orElse(null);
        }
        if (lower.startsWith(MATERIAL_PREFIX)) {
            Material material = parseMaterial(clean.substring(MATERIAL_PREFIX.length()));
            return material == null ? null : new RecipeChoice.MaterialChoice(material);
        }

        Material material = parseMaterial(clean);
        if (material != null) {
            return new RecipeChoice.MaterialChoice(material);
        }

        String itemId = normalize(clean);
        if (itemId == null) {
            return null;
        }
        return itemRegistry.createItem(itemId, 1)
                .<RecipeChoice>map(item -> new RecipeChoice.ExactChoice(item))
                .orElse(null);
    }

    private Material parseMaterial(String raw) {
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

    private String sanitizeKey(String value) {
        if (value == null) {
            return "recipe";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9_\\-./]", "_");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private void unregisterAll() {
        for (NamespacedKey key : registeredRecipeKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredRecipeKeys.clear();
    }
}
