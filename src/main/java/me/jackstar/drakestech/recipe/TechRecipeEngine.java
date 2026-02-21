package me.jackstar.drakestech.recipe;

import me.jackstar.drakestech.item.TechItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TechRecipeEngine {

    private static final String ITEM_PREFIX = "item:";
    private static final String MATERIAL_PREFIX = "material:";

    private final JavaPlugin plugin;
    private final TechItemRegistry itemRegistry;
    private final File contentFile;

    private final Map<Material, ItemStack> vanillaSmeltingRecipes = new EnumMap<>(Material.class);
    private final Map<String, SmeltingOutput> customSmeltingRecipes = new HashMap<>();
    private boolean useVanillaFallback = true;

    public TechRecipeEngine(JavaPlugin plugin, TechItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.contentFile = new File(plugin.getDataFolder(), "tech-content.yml");
        reload();
    }

    public synchronized void reload() {
        vanillaSmeltingRecipes.clear();
        customSmeltingRecipes.clear();
        useVanillaFallback = true;

        loadVanillaSmeltingRecipes();
        loadCustomSmeltingRecipes();

        plugin.getLogger().info("[Recipes] Loaded custom smelting recipes: " + customSmeltingRecipes.size());
        plugin.getLogger().info("[Recipes] Loaded vanilla smelting recipes: " + vanillaSmeltingRecipes.size());
    }

    public synchronized Optional<ItemStack> resolveSmeltingResult(ItemStack input) {
        if (input == null || input.getType().isAir()) {
            return Optional.empty();
        }

        String inputKey = buildInputKey(input);
        SmeltingOutput custom = customSmeltingRecipes.get(inputKey);
        if (custom != null) {
            ItemStack resolved = custom.createItemStack(itemRegistry).orElse(null);
            if (resolved == null || resolved.getType().isAir()) {
                plugin.getLogger().warning("[Recipes] Custom output could not be resolved for key '" + inputKey + "'.");
                return Optional.empty();
            }
            return Optional.of(resolved);
        }

        if (itemRegistry.readTechItemId(input).isPresent()) {
            return Optional.empty();
        }

        if (!useVanillaFallback) {
            return Optional.empty();
        }

        ItemStack vanilla = vanillaSmeltingRecipes.get(input.getType());
        if (vanilla == null) {
            return Optional.empty();
        }
        return Optional.of(vanilla.clone());
    }

    public synchronized int getCustomSmeltingRecipeCount() {
        return customSmeltingRecipes.size();
    }

    public synchronized int getVanillaSmeltingRecipeCount() {
        return vanillaSmeltingRecipes.size();
    }

    private void loadVanillaSmeltingRecipes() {
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            if (!(recipe instanceof CookingRecipe<?> cookingRecipe)) {
                continue;
            }

            ItemStack output = cookingRecipe.getResult();
            if (output == null || output.getType().isAir()) {
                continue;
            }

            RecipeChoice inputChoice = cookingRecipe.getInputChoice();
            if (inputChoice instanceof RecipeChoice.MaterialChoice materialChoice) {
                for (Material material : materialChoice.getChoices()) {
                    vanillaSmeltingRecipes.putIfAbsent(material, output.clone());
                }
                continue;
            }

            if (inputChoice instanceof RecipeChoice.ExactChoice exactChoice) {
                for (ItemStack stack : exactChoice.getChoices()) {
                    if (stack != null && !stack.getType().isAir()) {
                        vanillaSmeltingRecipes.putIfAbsent(stack.getType(), output.clone());
                    }
                }
                continue;
            }

            ItemStack legacyInput = cookingRecipe.getInput();
            if (legacyInput != null && !legacyInput.getType().isAir()) {
                vanillaSmeltingRecipes.putIfAbsent(legacyInput.getType(), output.clone());
            }
        }
    }

    private void loadCustomSmeltingRecipes() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(contentFile);
        useVanillaFallback = config.getBoolean("recipes.use-vanilla-fallback", true);

        ConfigurationSection smeltingSection = config.getConfigurationSection("recipes.smelting");
        if (smeltingSection == null) {
            return;
        }

        for (String key : smeltingSection.getKeys(false)) {
            ConfigurationSection recipeSection = smeltingSection.getConfigurationSection(key);
            if (recipeSection == null || !recipeSection.getBoolean("enabled", true)) {
                continue;
            }

            List<String> inputs = parseInputKeys(recipeSection);
            SmeltingOutput output = parseOutput(recipeSection.getString("output"),
                    Math.max(1, recipeSection.getInt("amount", 1)));

            if (inputs.isEmpty() || output == null) {
                plugin.getLogger().warning("[Recipes] Invalid custom smelting recipe '" + key + "'. Skipping.");
                continue;
            }

            for (String inputKey : inputs) {
                SmeltingOutput replaced = customSmeltingRecipes.put(inputKey, output);
                if (replaced != null) {
                    plugin.getLogger().warning("[Recipes] Replaced custom smelting rule for input '" + inputKey + "'.");
                }
            }
        }
    }

    private List<String> parseInputKeys(ConfigurationSection section) {
        List<String> keys = new ArrayList<>();

        String singleInput = section.getString("input");
        String parsedSingle = parseInputToken(singleInput);
        if (parsedSingle != null) {
            keys.add(parsedSingle);
        }

        List<String> listInput = section.getStringList("inputs");
        for (String value : listInput) {
            String parsed = parseInputToken(value);
            if (parsed != null) {
                keys.add(parsed);
            }
        }
        return keys;
    }

    private String parseInputToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String clean = raw.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ITEM_PREFIX)) {
            String itemId = normalize(clean.substring(ITEM_PREFIX.length()));
            return itemId == null ? null : ITEM_PREFIX + itemId;
        }
        if (lower.startsWith(MATERIAL_PREFIX)) {
            Material material = parseMaterial(clean.substring(MATERIAL_PREFIX.length()));
            return material == null ? null : MATERIAL_PREFIX + material.name();
        }

        Material material = parseMaterial(clean);
        if (material != null) {
            return MATERIAL_PREFIX + material.name();
        }

        String itemId = normalize(clean);
        if (itemId != null && itemRegistry.find(itemId).isPresent()) {
            return ITEM_PREFIX + itemId;
        }

        return null;
    }

    private SmeltingOutput parseOutput(String rawOutput, int amount) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return null;
        }

        String clean = rawOutput.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ITEM_PREFIX)) {
            String itemId = normalize(clean.substring(ITEM_PREFIX.length()));
            if (itemId == null) {
                return null;
            }
            return SmeltingOutput.customItem(itemId, amount);
        }
        if (lower.startsWith(MATERIAL_PREFIX)) {
            Material material = parseMaterial(clean.substring(MATERIAL_PREFIX.length()));
            if (material == null || material.isAir()) {
                return null;
            }
            return SmeltingOutput.vanilla(material, amount);
        }

        Material material = parseMaterial(clean);
        if (material != null && !material.isAir()) {
            return SmeltingOutput.vanilla(material, amount);
        }

        String itemId = normalize(clean);
        if (itemId != null && itemRegistry.find(itemId).isPresent()) {
            return SmeltingOutput.customItem(itemId, amount);
        }

        return null;
    }

    private String buildInputKey(ItemStack input) {
        Optional<String> customId = itemRegistry.readTechItemId(input);
        if (customId.isPresent()) {
            return ITEM_PREFIX + customId.get();
        }
        return MATERIAL_PREFIX + input.getType().name();
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private record SmeltingOutput(OutputType type, String customItemId, Material material, int amount) {
        static SmeltingOutput customItem(String customItemId, int amount) {
            return new SmeltingOutput(OutputType.CUSTOM_ITEM, customItemId, null, Math.max(1, amount));
        }

        static SmeltingOutput vanilla(Material material, int amount) {
            return new SmeltingOutput(OutputType.VANILLA, null, material, Math.max(1, amount));
        }

        Optional<ItemStack> createItemStack(TechItemRegistry itemRegistry) {
            if (type == OutputType.CUSTOM_ITEM) {
                return itemRegistry.createItem(customItemId, amount);
            }
            if (material == null || material.isAir()) {
                return Optional.empty();
            }
            return Optional.of(new ItemStack(material, amount));
        }
    }

    private enum OutputType {
        CUSTOM_ITEM,
        VANILLA
    }
}
