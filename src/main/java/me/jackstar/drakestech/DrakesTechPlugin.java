package me.jackstar.drakestech;

import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.addon.DrakesTechAddonLifecycleManager;
import me.jackstar.drakestech.bootstrap.BuiltinTechContentLoader;
import me.jackstar.drakestech.commands.DrakesTechCommand;
import me.jackstar.drakestech.config.DrakesTechSettings;
import me.jackstar.drakestech.core.DrakesTechApiService;
import me.jackstar.drakestech.guide.TechGuideManager;
import me.jackstar.drakestech.listeners.DrakesTechBlockListener;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import me.jackstar.drakestech.nbt.PdcNbtItemHandler;
import me.jackstar.drakestech.recipe.TechRecipeEngine;
import me.jackstar.drakestech.research.TechResearchService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

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
    private DrakesTechSettings settings;
    private TechGuideManager guideManager;
    private DrakesTechApiService apiService;
    private DrakesTechAddonLifecycleManager addonLifecycleManager;
    private TechRecipeEngine recipeEngine;
    private TechResearchService researchService;

    @Override
    public void onEnable() {
        logDragonBanner("DrakesTech");
        logLoading("Saving default resources");
        saveDefaultResources();

        logLoading("Loading settings");
        settings = new DrakesTechSettings(this);

        logLoading("Loading recipe engine");
        recipeEngine = new TechRecipeEngine(this);

        logLoading("Loading research service");
        researchService = new TechResearchService(this, settings);
        researchService.start();

        logLoading("Initializing NBT and machine factory");
        NbtItemHandler nbtItemHandler = new PdcNbtItemHandler(this);
        machineFactory = new MachineFactory(nbtItemHandler);

        logLoading("Initializing guide manager and public API");
        guideManager = new TechGuideManager(this, settings, nbtItemHandler, researchService);
        apiService = new DrakesTechApiService(this, machineFactory, guideManager, researchService);
        guideManager.bindApi(apiService);

        logLoading("Registering API service for expansions");
        ServicesManager services = getServer().getServicesManager();
        services.register(DrakesTechApi.class, apiService, this, ServicePriority.Normal);

        logLoading("Registering built-in content");
        BuiltinTechContentLoader.registerDefaults(this, apiService, recipeEngine);

        logLoading("Starting machine manager");
        machineManager = new MachineManager(this, machineFactory);
        machineManager.start();

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
            drakesTechCommand.setExecutor(new DrakesTechCommand(this, machineFactory, machineManager, apiService, researchService));
        } else {
            getLogger().warning("Command 'drakestech' not found in plugin.yml.");
        }

        logLoading("Registering listeners");
        getServer().getPluginManager().registerEvents(new DrakesTechBlockListener(machineManager, machineFactory), this);
        getServer().getPluginManager().registerEvents(guideManager, this);
        getServer().getPluginManager().registerEvents(researchService, this);

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
        if (researchService != null) {
            researchService.stop();
        }
        getLogger().info("[Shutdown] DrakesTech disabled.");
    }

    public void reloadRuntime() {
        if (settings != null) {
            settings.reload();
        }
        if (recipeEngine != null) {
            recipeEngine.reload();
        }
        if (researchService != null) {
            researchService.reload();
        }
        if (apiService != null) {
            apiService.unregisterOwnedContent(getName());
            if (recipeEngine != null) {
                BuiltinTechContentLoader.registerDefaults(this, apiService, recipeEngine);
            }
        }
        if (addonLifecycleManager != null) {
            addonLifecycleManager.reloadAll();
        }
        if (machineManager != null) {
            machineManager.reloadMachinesFromDisk();
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

    private void saveDefaultResources() {
        if (getResource("drakestech.yml") != null) {
            saveResource("drakestech.yml", false);
        }
        if (getResource("tech-content.yml") != null) {
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
