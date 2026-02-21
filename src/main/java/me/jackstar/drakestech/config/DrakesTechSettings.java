package me.jackstar.drakestech.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DrakesTechSettings {

    private final JavaPlugin plugin;
    private final File file;

    private boolean autoGiveGuideOnFirstJoin = true;
    private boolean openGuideOnRightClick = true;
    private Material guideBookMaterial = Material.WRITTEN_BOOK;
    private String guideBookName = "<gradient:gold:red><b>DrakesTech Guide</b></gradient>";
    private List<String> guideBookLore = List.of("<gray>Right click to open modules.</gray>");
    private String guideMainTitle = "<gold><b>DrakesTech Modules</b></gold>";
    private boolean researchEnabled = true;
    private List<String> defaultUnlockedModules = List.of("machines");
    private List<String> defaultUnlockedEntries = List.of();
    private int researchModuleUnlockCostLevels = 20;
    private int researchEntryUnlockCostLevels = 8;
    private boolean automationItemTransferEnabled = true;
    private int automationItemTransferIntervalTicks = 10;
    private int automationItemTransferMaxItemsPerMove = 8;
    private boolean techStorageOnlyPluginItems = true;
    private boolean networkEnabled = true;
    private int networkCycleIntervalTicks = 10;
    private int networkMaxNodesPerNetwork = 1024;
    private int networkExportMaxItemsPerCycle = 64;

    public DrakesTechSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "drakestech.yml");
        saveDefault();
        reload();
    }

    public void reload() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        autoGiveGuideOnFirstJoin = config.getBoolean("guide.auto-give-on-first-join", true);
        openGuideOnRightClick = config.getBoolean("guide.open-on-right-click", true);

        String materialRaw = config.getString("guide.book.material", "WRITTEN_BOOK");
        Material parsedMaterial = parseMaterial(materialRaw);
        guideBookMaterial = parsedMaterial == null ? Material.WRITTEN_BOOK : parsedMaterial;

        guideBookName = config.getString("guide.book.name", guideBookName);
        List<String> lore = config.getStringList("guide.book.lore");
        guideBookLore = lore.isEmpty() ? List.of("<gray>Right click to open modules.</gray>") : new ArrayList<>(lore);
        guideMainTitle = config.getString("guide.main-title", guideMainTitle);

        researchEnabled = config.getBoolean("research.enabled", true);
        defaultUnlockedModules = normalizeList(config.getStringList("research.default-unlocked-modules"));
        if (defaultUnlockedModules.isEmpty()) {
            defaultUnlockedModules = List.of("machines");
        }
        defaultUnlockedEntries = normalizeList(config.getStringList("research.default-unlocked-entries"));
        researchModuleUnlockCostLevels = Math.max(0, config.getInt("research.unlock-costs.module-levels", 20));
        researchEntryUnlockCostLevels = Math.max(0, config.getInt("research.unlock-costs.entry-levels", 8));

        automationItemTransferEnabled = config.getBoolean("automation.item-transfer.enabled", true);
        automationItemTransferIntervalTicks = Math.max(1, config.getInt("automation.item-transfer.interval-ticks", 10));
        automationItemTransferMaxItemsPerMove = Math.max(1, config.getInt("automation.item-transfer.max-items-per-move", 8));
        techStorageOnlyPluginItems = config.getBoolean("automation.tech-storage.only-plugin-items", true);

        networkEnabled = config.getBoolean("network.enabled", true);
        networkCycleIntervalTicks = Math.max(1, config.getInt("network.cycle-interval-ticks", 10));
        networkMaxNodesPerNetwork = Math.max(8, config.getInt("network.max-nodes-per-network", 1024));
        networkExportMaxItemsPerCycle = Math.max(1, config.getInt("network.export.max-items-per-cycle", 64));
    }

    public boolean isAutoGiveGuideOnFirstJoin() {
        return autoGiveGuideOnFirstJoin;
    }

    public boolean isOpenGuideOnRightClick() {
        return openGuideOnRightClick;
    }

    public Material getGuideBookMaterial() {
        return guideBookMaterial;
    }

    public String getGuideBookName() {
        return guideBookName;
    }

    public List<String> getGuideBookLore() {
        return guideBookLore;
    }

    public String getGuideMainTitle() {
        return guideMainTitle;
    }

    public boolean isResearchEnabled() {
        return researchEnabled;
    }

    public List<String> getDefaultUnlockedModules() {
        return defaultUnlockedModules;
    }

    public List<String> getDefaultUnlockedEntries() {
        return defaultUnlockedEntries;
    }

    public int getResearchModuleUnlockCostLevels() {
        return researchModuleUnlockCostLevels;
    }

    public int getResearchEntryUnlockCostLevels() {
        return researchEntryUnlockCostLevels;
    }

    public boolean isAutomationItemTransferEnabled() {
        return automationItemTransferEnabled;
    }

    public int getAutomationItemTransferIntervalTicks() {
        return automationItemTransferIntervalTicks;
    }

    public int getAutomationItemTransferMaxItemsPerMove() {
        return automationItemTransferMaxItemsPerMove;
    }

    public boolean isTechStorageOnlyPluginItems() {
        return techStorageOnlyPluginItems;
    }

    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    public int getNetworkCycleIntervalTicks() {
        return networkCycleIntervalTicks;
    }

    public int getNetworkMaxNodesPerNetwork() {
        return networkMaxNodesPerNetwork;
    }

    public int getNetworkExportMaxItemsPerCycle() {
        return networkExportMaxItemsPerCycle;
    }

    private void saveDefault() {
        if (!file.exists() && plugin.getResource("drakestech.yml") != null) {
            plugin.saveResource("drakestech.yml", false);
            return;
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException ignored) {
        }
    }

    private Material parseMaterial(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim();
        try {
            return Material.valueOf(clean.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Material.matchMaterial(clean);
        }
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String clean = value.trim().toLowerCase(Locale.ROOT);
            if (!clean.isEmpty()) {
                normalized.add(clean);
            }
        }
        return List.copyOf(normalized);
    }
}
