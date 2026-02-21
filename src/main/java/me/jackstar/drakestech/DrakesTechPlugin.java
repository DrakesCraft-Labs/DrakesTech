package me.jackstar.drakestech;

import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.addon.DrakesTechAddonLifecycleManager;
import me.jackstar.drakestech.bootstrap.BuiltinTechContentLoader;
import me.jackstar.drakestech.commands.DrakesTechCommand;
import me.jackstar.drakestech.config.DrakesTechSettings;
import me.jackstar.drakestech.core.DrakesTechApiService;
import me.jackstar.drakestech.guide.TechGuideManager;
import me.jackstar.drakestech.item.TechItemRegistry;
import me.jackstar.drakestech.listeners.DrakesTechBlockListener;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import me.jackstar.drakestech.multiblock.MultiblockService;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import me.jackstar.drakestech.nbt.PdcNbtItemHandler;
import me.jackstar.drakestech.recipe.TechCraftingRecipeService;
import me.jackstar.drakestech.recipe.TechRecipeEngine;
import me.jackstar.drakestech.research.TechResearchService;
import me.jackstar.drakestech.tools.TechToolService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class DrakesTechPlugin extends JavaPlugin {

    private static final String[] DRAGON_BANNER = {
            "              / \\  //\\",
            "      |\\___/|      /   \\//  \\\\",
            "      /O  O  \\__  /    //  | \\ \\",
            "     /     /  \\/_/    //   |  \\  \\",
            "     \\_^_\\'/   \\/_   //    |   \\   \\"
    };

    private MachineManager machineManager;
    private MachineFactory machineFactory;
    private TechItemRegistry itemRegistry;
    private DrakesTechSettings settings;
    private TechGuideManager guideManager;
    private DrakesTechApiService apiService;
    private DrakesTechAddonLifecycleManager addonLifecycleManager;
    private TechRecipeEngine recipeEngine;
    private TechCraftingRecipeService craftingRecipeService;
    private TechResearchService researchService;
    private MultiblockService multiblockService;
    private NbtItemHandler nbtItemHandler;
    private TechToolService toolService;

    @Override
    public void onEnable() {
        logDragonBanner("DrakesTech");
        logLoading("Saving default resources");
        saveDefaultResources();

        logLoading("Loading settings");
        settings = new DrakesTechSettings(this);

        logLoading("Initializing NBT and machine factory");
        nbtItemHandler = new PdcNbtItemHandler(this);
        machineFactory = new MachineFactory(nbtItemHandler);
        itemRegistry = new TechItemRegistry(nbtItemHandler);

        logLoading("Loading recipe engine");
        recipeEngine = new TechRecipeEngine(this, itemRegistry);
        craftingRecipeService = new TechCraftingRecipeService(this, itemRegistry);

        logLoading("Loading research service");
        researchService = new TechResearchService(this, settings);
        researchService.start();

        logLoading("Initializing guide manager and public API");
        guideManager = new TechGuideManager(this, settings, nbtItemHandler, researchService);
        apiService = new DrakesTechApiService(this, machineFactory, itemRegistry, guideManager, researchService);
        guideManager.bindApi(apiService);

        logLoading("Registering API service for expansions");
        ServicesManager services = getServer().getServicesManager();
        services.register(DrakesTechApi.class, apiService, this, ServicePriority.Normal);

        logLoading("Registering built-in content");
        BuiltinTechContentLoader.registerDefaults(this, apiService, recipeEngine, nbtItemHandler, settings);
        recipeEngine.reload();
        craftingRecipeService.reload();

        logLoading("Starting machine manager");
        machineManager = new MachineManager(this, machineFactory, settings);
        machineManager.start();
        multiblockService = new MultiblockService(this, machineFactory, machineManager);
        multiblockService.reload();
        toolService = new TechToolService(this, apiService, settings, itemRegistry, machineManager);
        toolService.start();

        logLoading("Starting addon lifecycle manager");
        addonLifecycleManager = new DrakesTechAddonLifecycleManager(this, apiService, machineManager);
        addonLifecycleManager.start();

        getServer().getScheduler().runTaskLater(this, () -> {
            logLoading("Reloading machines after startup to catch late expansion registrations");
            machineManager.reloadMachinesFromDisk();
        }, 60L);

        logLoading("Registering command executors");
        PluginCommand drakesTechCommand = getCommand("drakestech");
        if (drakesTechCommand != null) {
            DrakesTechCommand commandHandler = new DrakesTechCommand(this, machineFactory, machineManager, apiService, researchService);
            drakesTechCommand.setExecutor(commandHandler);
            drakesTechCommand.setTabCompleter(commandHandler);
        } else {
            getLogger().warning("Command 'drakestech' not found in plugin.yml.");
        }

        logLoading("Registering listeners");
        getServer().getPluginManager().registerEvents(new DrakesTechBlockListener(machineManager, machineFactory, multiblockService), this);
        getServer().getPluginManager().registerEvents(guideManager, this);
        getServer().getPluginManager().registerEvents(researchService, this);
        getServer().getPluginManager().registerEvents(toolService, this);

        getLogger().info("[Ready] DrakesTech enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Shutdown] DrakesTech stopping...");

        ServicesManager services = getServer().getServicesManager();
        services.unregisterAll(this);

        if (addonLifecycleManager != null) {
            addonLifecycleManager.stop();
        }

        if (machineManager != null) {
            machineManager.stop();
        }
        if (toolService != null) {
            toolService.stop();
        }
        if (researchService != null) {
            researchService.stop();
        }
        getLogger().info("[Shutdown] DrakesTech disabled.");
    }

    public void reloadRuntime() {
        if (settings != null) {
            settings.reload();
        }
        if (apiService != null) {
            apiService.unregisterOwnedContent(getName());
            if (recipeEngine != null) {
                BuiltinTechContentLoader.registerDefaults(this, apiService, recipeEngine, nbtItemHandler, settings);
                recipeEngine.reload();
            }
            if (craftingRecipeService != null) {
                craftingRecipeService.reload();
            }
        }
        if (researchService != null) {
            researchService.reload();
        }
        if (addonLifecycleManager != null) {
            addonLifecycleManager.reloadAll();
        }
        if (machineManager != null) {
            machineManager.reloadMachinesFromDisk();
        }
        if (toolService != null) {
            toolService.reload();
        }
        if (multiblockService != null) {
            multiblockService.reload();
        }
        getLogger().info("[Reload] DrakesTech runtime reloaded.");
    }

    public DrakesTechApi getApi() {
        return apiService;
    }

    public List<String> getLoadedAddons() {
        if (addonLifecycleManager == null) {
            return List.of();
        }
        return addonLifecycleManager.getLoadedAddonPlugins();
    }

    public TechRecipeEngine getRecipeEngine() {
        return recipeEngine;
    }

    public TechCraftingRecipeService getCraftingRecipeService() {
        return craftingRecipeService;
    }

    private void saveDefaultResources() {
        File settingsFile = new File(getDataFolder(), "drakestech.yml");
        if (!settingsFile.exists() && getResource("drakestech.yml") != null) {
            saveResource("drakestech.yml", false);
        }
        File contentFile = new File(getDataFolder(), "tech-content.yml");
        if (!contentFile.exists() && getResource("tech-content.yml") != null) {
            saveResource("tech-content.yml", false);
        }
    }

    private void logLoading(String step) {
        getLogger().info("[Loading] " + step + "...");
    }

    private void logDragonBanner(String pluginName) {
        getLogger().info("========================================");
        getLogger().info(" " + pluginName + " - loading");
        for (String line : DRAGON_BANNER) {
            getLogger().info(line);
        }
        getLogger().info("========================================");
    }
}
