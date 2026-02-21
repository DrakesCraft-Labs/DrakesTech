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
    private boolean toolsEnabled = true;
    private boolean drillMk1Enabled = true;
    private String drillMk1ItemId = "drill_mk1_3x3";
    private String drillMk1FuelItemId = "power_core_t1";
    private int drillMk1FuelUnitsPerItem = 40;
    private int drillMk1EnergyPerBlock = 4;
    private int drillMk1MaxBlocksPerUse = 64;
    private boolean drillMk2Enabled = true;
    private String drillMk2ItemId = "drill_mk2_5x5";
    private String drillMk2FuelItemId = "power_core_t2";
    private int drillMk2FuelUnitsPerItem = 80;
    private int drillMk2EnergyPerBlock = 7;
    private int drillMk2MaxBlocksPerUse = 128;
    private boolean impactChargeEnabled = true;
    private String impactChargeItemId = "impact_charge";
    private String impactChargeFuelItemId = "power_core_t2";
    private int impactChargeFuelUnitsPerItem = 80;
    private int impactChargeEnergyPerUse = 35;
    private double impactChargeRadius = 3.5D;
    private int impactChargeMaxBlocks = 120;
    private int impactChargeCooldownTicks = 40;
    private float impactChargeDamageExplosionYield = 1.75F;
    private boolean impactChargeBreakContainers = false;

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

        toolsEnabled = config.getBoolean("tools.enabled", true);

        drillMk1Enabled = config.getBoolean("tools.drills.mk1.enabled", true);
        drillMk1ItemId = normalizeId(config.getString("tools.drills.mk1.item-id"), "drill_mk1_3x3");
        drillMk1FuelItemId = normalizeId(config.getString("tools.drills.mk1.energy.fuel-item-id"), "power_core_t1");
        drillMk1FuelUnitsPerItem = Math.max(1, config.getInt("tools.drills.mk1.energy.fuel-units-per-item", 40));
        drillMk1EnergyPerBlock = Math.max(1, config.getInt("tools.drills.mk1.energy.units-per-block", 4));
        drillMk1MaxBlocksPerUse = Math.max(1, config.getInt("tools.drills.mk1.max-blocks-per-use", 64));

        drillMk2Enabled = config.getBoolean("tools.drills.mk2.enabled", true);
        drillMk2ItemId = normalizeId(config.getString("tools.drills.mk2.item-id"), "drill_mk2_5x5");
        drillMk2FuelItemId = normalizeId(config.getString("tools.drills.mk2.energy.fuel-item-id"), "power_core_t2");
        drillMk2FuelUnitsPerItem = Math.max(1, config.getInt("tools.drills.mk2.energy.fuel-units-per-item", 80));
        drillMk2EnergyPerBlock = Math.max(1, config.getInt("tools.drills.mk2.energy.units-per-block", 7));
        drillMk2MaxBlocksPerUse = Math.max(1, config.getInt("tools.drills.mk2.max-blocks-per-use", 128));

        impactChargeEnabled = config.getBoolean("tools.impact-charge.enabled", true);
        impactChargeItemId = normalizeId(config.getString("tools.impact-charge.item-id"), "impact_charge");
        impactChargeFuelItemId = normalizeId(config.getString("tools.impact-charge.energy.fuel-item-id"), "power_core_t2");
        impactChargeFuelUnitsPerItem = Math.max(1, config.getInt("tools.impact-charge.energy.fuel-units-per-item", 80));
        impactChargeEnergyPerUse = Math.max(1, config.getInt("tools.impact-charge.energy.units-per-use", 35));
        impactChargeRadius = Math.max(1.0D, config.getDouble("tools.impact-charge.break.radius", 3.5D));
        impactChargeMaxBlocks = Math.max(1, config.getInt("tools.impact-charge.break.max-blocks", 120));
        impactChargeCooldownTicks = Math.max(0, config.getInt("tools.impact-charge.cooldown-ticks", 40));
        impactChargeDamageExplosionYield = Math.max(0.0F, (float) config.getDouble("tools.impact-charge.damage-explosion-yield", 1.75D));
        impactChargeBreakContainers = config.getBoolean("tools.impact-charge.break.containers", false);
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

    public boolean isToolsEnabled() {
        return toolsEnabled;
    }

    public boolean isDrillMk1Enabled() {
        return drillMk1Enabled;
    }

    public String getDrillMk1ItemId() {
        return drillMk1ItemId;
    }

    public String getDrillMk1FuelItemId() {
        return drillMk1FuelItemId;
    }

    public int getDrillMk1FuelUnitsPerItem() {
        return drillMk1FuelUnitsPerItem;
    }

    public int getDrillMk1EnergyPerBlock() {
        return drillMk1EnergyPerBlock;
    }

    public int getDrillMk1MaxBlocksPerUse() {
        return drillMk1MaxBlocksPerUse;
    }

    public boolean isDrillMk2Enabled() {
        return drillMk2Enabled;
    }

    public String getDrillMk2ItemId() {
        return drillMk2ItemId;
    }

    public String getDrillMk2FuelItemId() {
        return drillMk2FuelItemId;
    }

    public int getDrillMk2FuelUnitsPerItem() {
        return drillMk2FuelUnitsPerItem;
    }

    public int getDrillMk2EnergyPerBlock() {
        return drillMk2EnergyPerBlock;
    }

    public int getDrillMk2MaxBlocksPerUse() {
        return drillMk2MaxBlocksPerUse;
    }

    public boolean isImpactChargeEnabled() {
        return impactChargeEnabled;
    }

    public String getImpactChargeItemId() {
        return impactChargeItemId;
    }

    public String getImpactChargeFuelItemId() {
        return impactChargeFuelItemId;
    }

    public int getImpactChargeFuelUnitsPerItem() {
        return impactChargeFuelUnitsPerItem;
    }

    public int getImpactChargeEnergyPerUse() {
        return impactChargeEnergyPerUse;
    }

    public double getImpactChargeRadius() {
        return impactChargeRadius;
    }

    public int getImpactChargeMaxBlocks() {
        return impactChargeMaxBlocks;
    }

    public int getImpactChargeCooldownTicks() {
        return impactChargeCooldownTicks;
    }

    public float getImpactChargeDamageExplosionYield() {
        return impactChargeDamageExplosionYield;
    }

    public boolean isImpactChargeBreakContainers() {
        return impactChargeBreakContainers;
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

    private String normalizeId(String value, String fallback) {
        String normalized = value == null ? null : value.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }
}
