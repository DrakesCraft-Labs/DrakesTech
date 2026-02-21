package me.jackstar.drakestech.recipe;

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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TechRecipeEngine {

    private final JavaPlugin plugin;
    private final File contentFile;

    private final Map<Material, ItemStack> vanillaSmeltingRecipes = new EnumMap<>(Material.class);
    private final Map<Material, ItemStack> customSmeltingRecipes = new EnumMap<>(Material.class);
    private boolean useVanillaFallback = true;

    public TechRecipeEngine(JavaPlugin plugin) {
        this.plugin = plugin;
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

        ItemStack custom = customSmeltingRecipes.get(input.getType());
        if (custom != null) {
            return Optional.of(custom.clone());
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

            List<Material> inputs = parseInputMaterials(recipeSection);
            Material outputType = parseMaterial(recipeSection.getString("output"));
            int amount = Math.max(1, recipeSection.getInt("amount", 1));

            if (inputs.isEmpty() || outputType == null || outputType.isAir()) {
                plugin.getLogger().warning("[Recipes] Invalid custom smelting recipe '" + key + "'. Skipping.");
                continue;
            }

            ItemStack output = new ItemStack(outputType, amount);
            for (Material input : inputs) {
                customSmeltingRecipes.put(input, output.clone());
            }
        }
    }

    private List<Material> parseInputMaterials(ConfigurationSection section) {
        List<Material> materials = new ArrayList<>();

        String singleInput = section.getString("input");
        if (singleInput != null && !singleInput.isBlank()) {
            Material parsed = parseMaterial(singleInput);
            if (parsed != null && !parsed.isAir()) {
                materials.add(parsed);
            }
        }

        List<String> listInput = section.getStringList("inputs");
        for (String value : listInput) {
            Material parsed = parseMaterial(value);
            if (parsed != null && !parsed.isAir()) {
                materials.add(parsed);
            }
        }

        return materials;
    }

    private Material parseMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT), true);
    }
}
