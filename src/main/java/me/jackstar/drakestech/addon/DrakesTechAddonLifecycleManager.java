package me.jackstar.drakestech.addon;

import me.jackstar.drakestech.core.DrakesTechApiService;
import me.jackstar.drakestech.manager.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DrakesTechAddonLifecycleManager implements Listener {

    private final JavaPlugin hostPlugin;
    private final DrakesTechApiService apiService;
    private final MachineManager machineManager;
    private final Set<String> loadedAddonPlugins = ConcurrentHashMap.newKeySet();

    public DrakesTechAddonLifecycleManager(JavaPlugin hostPlugin, DrakesTechApiService apiService, MachineManager machineManager) {
        this.hostPlugin = hostPlugin;
        this.apiService = apiService;
        this.machineManager = machineManager;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, hostPlugin);
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            tryRegisterAddon(plugin);
        }
    }

    public void stop() {
        for (String pluginName : new ArrayList<>(apiService.getContentOwners())) {
            if (hostPlugin.getName().equalsIgnoreCase(pluginName)) {
                continue;
            }
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (plugin == null) {
                apiService.unregisterOwnedContent(pluginName);
                continue;
            }
            tryUnloadAddon(plugin);
        }
        loadedAddonPlugins.clear();
    }

    public int reloadAll() {
        int reloaded = 0;
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (tryUnloadAddon(plugin)) {
                reloaded++;
            }
            if (tryRegisterAddon(plugin)) {
                reloaded++;
            }
        }
        return reloaded;
    }

    public List<String> getLoadedAddonPlugins() {
        List<String> names = new ArrayList<>(loadedAddonPlugins);
        names.sort(String::compareToIgnoreCase);
        return Collections.unmodifiableList(names);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        tryRegisterAddon(event.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        tryUnloadAddon(event.getPlugin());
    }

    private boolean tryRegisterAddon(Plugin plugin) {
        if (plugin == null || plugin == hostPlugin || !plugin.isEnabled()) {
            return false;
        }
        if (loadedAddonPlugins.contains(plugin.getName())) {
            return false;
        }

        boolean changed = false;
        if (plugin instanceof DrakesTechAddon addon) {
            try {
                addon.onDrakesTechLoad(apiService);
                hostPlugin.getLogger().info("[Addon] Loaded " + plugin.getName() + " (DrakesTechAddon).");
                changed = true;
            } catch (Exception ex) {
                hostPlugin.getLogger().warning("[Addon] Failed to load " + plugin.getName() + ": " + ex.getMessage());
            }
        }

        DrakesTechApiService.OwnerStats stats = apiService.getOwnerStats(plugin.getName());
        if (stats.machines() + stats.modules() + stats.entries() + stats.enchantments() + stats.items() > 0) {
            changed = true;
        }

        if (!changed) {
            return false;
        }

        loadedAddonPlugins.add(plugin.getName());
        if (changed) {
            machineManager.reloadMachinesFromDisk();
        }
        return changed;
    }

    private boolean tryUnloadAddon(Plugin plugin) {
        if (plugin == null || plugin == hostPlugin) {
            return false;
        }

        boolean changed = false;
        if (plugin instanceof DrakesTechAddon addon) {
            try {
                addon.onDrakesTechUnload(apiService);
            } catch (Exception ex) {
                hostPlugin.getLogger().warning("[Addon] Failed unload hook for " + plugin.getName() + ": " + ex.getMessage());
            }
        }

        int removed = apiService.unregisterOwnedContent(plugin.getName());
        if (removed > 0) {
            hostPlugin.getLogger().info("[Addon] Unloaded " + plugin.getName() + ", removed " + removed + " registration(s).");
            changed = true;
        }

        loadedAddonPlugins.remove(plugin.getName());

        if (changed) {
            machineManager.reloadMachinesFromDisk();
        }
        return changed;
    }
}
