package me.jackstar.drakestech.research;

import me.jackstar.drakestech.config.DrakesTechSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TechResearchService implements Listener {

    private static final String MODULE_WILDCARD_ENTRY = "*";

    private final JavaPlugin plugin;
    private final DrakesTechSettings settings;
    private final File dataFile;
    private final File contentFile;
    private final Map<UUID, Set<String>> unlockedByPlayer = new ConcurrentHashMap<>();
    private final Map<String, Integer> moduleUnlockCosts = new ConcurrentHashMap<>();
    private final Map<String, Integer> entryUnlockCosts = new ConcurrentHashMap<>();

    public TechResearchService(JavaPlugin plugin, DrakesTechSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.dataFile = new File(plugin.getDataFolder(), "drakestech-research.yml");
        this.contentFile = new File(plugin.getDataFolder(), "tech-content.yml");
    }

    public void start() {
        load();
    }

    public void stop() {
        save();
    }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return settings.isResearchEnabled();
    }

    public boolean hasUnlocked(Player player, String moduleId, String entryId) {
        if (player == null) {
            return false;
        }
        return hasUnlocked(player.getUniqueId(), moduleId, entryId);
    }

    public boolean hasUnlocked(UUID playerId, String moduleId, String entryId) {
        if (!settings.isResearchEnabled()) {
            return true;
        }

        String module = normalize(moduleId);
        String entry = normalize(entryId);
        if (module == null || entry == null) {
            return false;
        }

        if (settings.getDefaultUnlockedModules().contains(module)) {
            return true;
        }

        if (settings.getDefaultUnlockedEntries().contains(composeKey(module, entry))
                || settings.getDefaultUnlockedEntries().contains(composeKey(module, MODULE_WILDCARD_ENTRY))) {
            return true;
        }

        Set<String> unlocked = unlockedByPlayer.get(playerId);
        if (unlocked == null || unlocked.isEmpty()) {
            return false;
        }

        return unlocked.contains(composeKey(module, entry))
                || unlocked.contains(composeKey(module, MODULE_WILDCARD_ENTRY));
    }

    public boolean unlockEntry(Player player, String moduleId, String entryId) {
        if (player == null) {
            return false;
        }
        return unlockEntry(player.getUniqueId(), moduleId, entryId);
    }

    public boolean unlockEntry(UUID playerId, String moduleId, String entryId) {
        if (!settings.isResearchEnabled()) {
            return false;
        }

        String module = normalize(moduleId);
        String entry = normalize(entryId);
        if (module == null || entry == null) {
            return false;
        }

        Set<String> unlocked = unlockedByPlayer.computeIfAbsent(playerId, ignored -> createDefaultSet());
        boolean added = unlocked.add(composeKey(module, entry));
        if (added) {
            save();
        }
        return added;
    }

    public boolean unlockModule(Player player, String moduleId) {
        if (player == null) {
            return false;
        }
        return unlockModule(player.getUniqueId(), moduleId);
    }

    public boolean unlockModule(UUID playerId, String moduleId) {
        return unlockEntry(playerId, moduleId, MODULE_WILDCARD_ENTRY);
    }

    public UnlockXpAttempt attemptUnlockModuleWithXp(Player player, String moduleId) {
        if (player == null) {
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, 0, 0);
        }
        if (!settings.isResearchEnabled()) {
            return new UnlockXpAttempt(UnlockXpResult.RESEARCH_DISABLED, 0, player.getLevel());
        }

        String module = normalize(moduleId);
        if (module == null) {
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, 0, player.getLevel());
        }

        int currentLevels = player.getLevel();
        int requiredLevels = getModuleUnlockCostLevels(module);
        if (hasUnlocked(player.getUniqueId(), module, MODULE_WILDCARD_ENTRY)) {
            return new UnlockXpAttempt(UnlockXpResult.ALREADY_UNLOCKED, requiredLevels, currentLevels);
        }

        if (currentLevels < requiredLevels) {
            return new UnlockXpAttempt(UnlockXpResult.INSUFFICIENT_LEVELS, requiredLevels, currentLevels);
        }

        boolean unlocked = unlockModule(player.getUniqueId(), module);
        if (!unlocked) {
            if (hasUnlocked(player.getUniqueId(), module, MODULE_WILDCARD_ENTRY)) {
                return new UnlockXpAttempt(UnlockXpResult.ALREADY_UNLOCKED, requiredLevels, currentLevels);
            }
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, requiredLevels, currentLevels);
        }

        if (requiredLevels > 0) {
            player.giveExpLevels(-requiredLevels);
        }
        return new UnlockXpAttempt(UnlockXpResult.UNLOCKED, requiredLevels, player.getLevel());
    }

    public UnlockXpAttempt attemptUnlockEntryWithXp(Player player, String moduleId, String entryId) {
        if (player == null) {
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, 0, 0);
        }
        if (!settings.isResearchEnabled()) {
            return new UnlockXpAttempt(UnlockXpResult.RESEARCH_DISABLED, 0, player.getLevel());
        }

        String module = normalize(moduleId);
        String entry = normalize(entryId);
        if (module == null || entry == null) {
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, 0, player.getLevel());
        }

        int currentLevels = player.getLevel();
        int requiredLevels = getEntryUnlockCostLevels(module, entry);
        if (hasUnlocked(player.getUniqueId(), module, entry)) {
            return new UnlockXpAttempt(UnlockXpResult.ALREADY_UNLOCKED, requiredLevels, currentLevels);
        }

        if (currentLevels < requiredLevels) {
            return new UnlockXpAttempt(UnlockXpResult.INSUFFICIENT_LEVELS, requiredLevels, currentLevels);
        }

        boolean unlocked = unlockEntry(player.getUniqueId(), module, entry);
        if (!unlocked) {
            if (hasUnlocked(player.getUniqueId(), module, entry)) {
                return new UnlockXpAttempt(UnlockXpResult.ALREADY_UNLOCKED, requiredLevels, currentLevels);
            }
            return new UnlockXpAttempt(UnlockXpResult.INVALID_INPUT, requiredLevels, currentLevels);
        }

        if (requiredLevels > 0) {
            player.giveExpLevels(-requiredLevels);
        }
        return new UnlockXpAttempt(UnlockXpResult.UNLOCKED, requiredLevels, player.getLevel());
    }

    public boolean lockEntry(Player player, String moduleId, String entryId) {
        if (player == null) {
            return false;
        }
        return lockEntry(player.getUniqueId(), moduleId, entryId);
    }

    public boolean lockEntry(UUID playerId, String moduleId, String entryId) {
        if (!settings.isResearchEnabled()) {
            return false;
        }

        String module = normalize(moduleId);
        String entry = normalize(entryId);
        if (module == null || entry == null) {
            return false;
        }

        Set<String> unlocked = unlockedByPlayer.get(playerId);
        if (unlocked == null) {
            return false;
        }

        boolean removed = unlocked.remove(composeKey(module, entry));
        if (removed) {
            save();
        }
        return removed;
    }

    public Collection<String> getUnlockedKeys(Player player) {
        if (player == null) {
            return List.of();
        }
        return getUnlockedKeys(player.getUniqueId());
    }

    public Collection<String> getUnlockedKeys(UUID playerId) {
        Set<String> unlocked = unlockedByPlayer.get(playerId);
        if (unlocked == null) {
            return List.of();
        }
        return Collections.unmodifiableSet(new HashSet<>(unlocked));
    }

    public int getModuleUnlockCostLevels(String moduleId) {
        String normalizedModule = normalize(moduleId);
        if (normalizedModule == null) {
            return settings.getResearchModuleUnlockCostLevels();
        }
        return Math.max(0, moduleUnlockCosts.getOrDefault(normalizedModule, settings.getResearchModuleUnlockCostLevels()));
    }

    public int getEntryUnlockCostLevels(String moduleId, String entryId) {
        String module = normalize(moduleId);
        String entry = normalize(entryId);
        if (module == null || entry == null) {
            return settings.getResearchEntryUnlockCostLevels();
        }
        String key = composeKey(module, entry);
        return Math.max(0, entryUnlockCosts.getOrDefault(key, settings.getResearchEntryUnlockCostLevels()));
    }

    private void load() {
        unlockedByPlayer.clear();
        loadUnlockCosts();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String uuidRaw : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidRaw);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("[Research] Invalid UUID in research file: " + uuidRaw);
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidRaw);
            if (playerSection == null) {
                continue;
            }

            Set<String> unlocked = createDefaultSet();
            for (String raw : playerSection.getStringList("unlocked")) {
                String normalized = normalizeUnlockKey(raw);
                if (normalized != null) {
                    unlocked.add(normalized);
                }
            }
            unlockedByPlayer.put(uuid, unlocked);
        }
    }

    private void save() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("[Research] Could not create plugin data folder.");
                return;
            }

            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<UUID, Set<String>> entry : unlockedByPlayer.entrySet()) {
                String path = "players." + entry.getKey() + ".unlocked";
                config.set(path, List.copyOf(entry.getValue()));
            }

            config.save(dataFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("[Research] Failed to save research file: " + ex.getMessage());
        }
    }

    private Set<String> createDefaultSet() {
        Set<String> defaults = new HashSet<>();
        for (String module : settings.getDefaultUnlockedModules()) {
            String normalized = normalize(module);
            if (normalized != null) {
                defaults.add(composeKey(normalized, MODULE_WILDCARD_ENTRY));
            }
        }
        for (String key : settings.getDefaultUnlockedEntries()) {
            String normalized = normalizeUnlockKey(key);
            if (normalized != null) {
                defaults.add(normalized);
            }
        }
        return defaults;
    }

    private void loadUnlockCosts() {
        moduleUnlockCosts.clear();
        entryUnlockCosts.clear();

        if (!contentFile.exists()) {
            return;
        }

        YamlConfiguration content = YamlConfiguration.loadConfiguration(contentFile);

        ConfigurationSection modulesSection = content.getConfigurationSection("modules");
        if (modulesSection != null) {
            for (String moduleIdRaw : modulesSection.getKeys(false)) {
                ConfigurationSection moduleSection = modulesSection.getConfigurationSection(moduleIdRaw);
                if (moduleSection == null) {
                    continue;
                }
                String moduleId = normalize(moduleIdRaw);
                if (moduleId == null) {
                    continue;
                }
                int cost = Math.max(0, moduleSection.getInt(
                        "unlock-cost-levels",
                        settings.getResearchModuleUnlockCostLevels()));
                moduleUnlockCosts.put(moduleId, cost);
            }
        }

        ConfigurationSection entriesSection = content.getConfigurationSection("entries");
        if (entriesSection != null) {
            for (String entryIdRaw : entriesSection.getKeys(false)) {
                ConfigurationSection entrySection = entriesSection.getConfigurationSection(entryIdRaw);
                if (entrySection == null) {
                    continue;
                }

                String moduleId = normalize(entrySection.getString("module"));
                String entryId = normalize(entryIdRaw);
                if (moduleId == null || entryId == null) {
                    continue;
                }

                int cost = Math.max(0, entrySection.getInt(
                        "unlock-cost-levels",
                        settings.getResearchEntryUnlockCostLevels()));
                entryUnlockCosts.put(composeKey(moduleId, entryId), cost);
            }
        }
    }

    private String composeKey(String moduleId, String entryId) {
        return normalize(moduleId) + "|" + normalize(entryId);
    }

    private String normalizeUnlockKey(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }

        String module = normalize(parts[0]);
        String entry = normalize(parts[1]);
        if (module == null || entry == null) {
            return null;
        }
        return module + "|" + entry;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!settings.isResearchEnabled()) {
            return;
        }
        unlockedByPlayer.computeIfAbsent(event.getPlayer().getUniqueId(), ignored -> {
            Set<String> defaults = createDefaultSet();
            save();
            return defaults;
        });
    }

    public enum UnlockXpResult {
        UNLOCKED,
        ALREADY_UNLOCKED,
        INSUFFICIENT_LEVELS,
        INVALID_INPUT,
        RESEARCH_DISABLED
    }

    public record UnlockXpAttempt(UnlockXpResult result, int requiredLevels, int currentLevels) {
    }
}
