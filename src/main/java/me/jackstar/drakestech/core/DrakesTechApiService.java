package me.jackstar.drakestech.core;

import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.api.enchant.TechEnchantmentDefinition;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.api.machine.MachineDefinition;
import me.jackstar.drakestech.guide.TechGuideManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import me.jackstar.drakestech.research.TechResearchService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class DrakesTechApiService implements DrakesTechApi {

    private static final Pattern MINI_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final Plugin hostPlugin;
    private final MachineFactory machineFactory;
    private final TechGuideManager guideManager;
    private final TechResearchService researchService;

    private final Map<String, TechGuideModule> modules = new ConcurrentHashMap<>();
    private final Map<String, List<TechGuideEntry>> entriesByModule = new ConcurrentHashMap<>();
    private final Map<String, TechEnchantmentDefinition> enchantments = new ConcurrentHashMap<>();

    private final Map<String, String> machineOwner = new LinkedHashMap<>();
    private final Map<String, String> moduleOwner = new LinkedHashMap<>();
    private final Map<String, String> entryOwner = new LinkedHashMap<>();
    private final Map<String, String> enchantOwner = new LinkedHashMap<>();

    public DrakesTechApiService(Plugin hostPlugin, MachineFactory machineFactory, TechGuideManager guideManager,
            TechResearchService researchService) {
        this.hostPlugin = hostPlugin;
        this.machineFactory = machineFactory;
        this.guideManager = guideManager;
        this.researchService = researchService;
    }

    @Override
    public synchronized boolean registerMachine(Plugin owner, MachineDefinition definition) {
        if (definition == null || definition.getId() == null || definition.createMachineItem() == null) {
            return false;
        }
        String ownerName = ownerName(owner);
        boolean registered = machineFactory.registerMachineDefinition(definition);
        if (!registered) {
            return false;
        }

        machineOwner.put(definition.getId(), ownerName);

        if (definition.getModuleId() != null) {
            TechGuideEntry autoEntry = new TechGuideEntry(
                    definition.getId(),
                    definition.getModuleId(),
                    definition.getDisplayName(),
                    definition.createMachineItem().getType(),
                    definition.getDescription(),
                    definition.getRecipeLines(),
                    definition.createMachineItem());
            registerGuideEntry(owner, autoEntry);
        }

        hostPlugin.getLogger().info("[API] Machine registered: " + definition.getId() + " by " + ownerName);
        return true;
    }

    @Override
    public synchronized boolean unregisterMachine(String machineId) {
        String key = normalize(machineId);
        if (key == null) {
            return false;
        }
        machineOwner.remove(key);
        return machineFactory.unregisterMachineDefinition(key);
    }

    @Override
    public Optional<MachineDefinition> findMachine(String machineId) {
        return machineFactory.findDefinition(machineId);
    }

    @Override
    public Collection<MachineDefinition> getMachines() {
        return machineFactory.getDefinitions();
    }

    @Override
    public synchronized boolean registerGuideModule(Plugin owner, TechGuideModule module) {
        if (module == null || module.getId() == null) {
            return false;
        }
        String key = normalize(module.getId());
        if (modules.containsKey(key)) {
            return false;
        }
        modules.put(key, module);
        entriesByModule.putIfAbsent(key, new ArrayList<>());
        moduleOwner.put(key, ownerName(owner));
        hostPlugin.getLogger().info("[API] Guide module registered: " + key + " by " + ownerName(owner));
        return true;
    }

    @Override
    public synchronized boolean registerGuideEntry(Plugin owner, TechGuideEntry entry) {
        if (entry == null || entry.getId() == null || entry.getModuleId() == null) {
            return false;
        }

        String moduleKey = normalize(entry.getModuleId());
        if (!modules.containsKey(moduleKey)) {
            return false;
        }

        List<TechGuideEntry> entries = entriesByModule.computeIfAbsent(moduleKey, ignored -> new ArrayList<>());
        String targetId = normalize(entry.getId());
        boolean exists = entries.stream().anyMatch(existing -> normalize(existing.getId()).equals(targetId));
        if (exists) {
            return false;
        }

        entries.add(entry);
        entryOwner.put(moduleKey + ":" + targetId, ownerName(owner));
        return true;
    }

    @Override
    public Collection<TechGuideModule> getGuideModules() {
        List<TechGuideModule> moduleList = new ArrayList<>(modules.values());
        moduleList.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return Collections.unmodifiableList(moduleList);
    }

    @Override
    public List<TechGuideEntry> getGuideEntries(String moduleId) {
        String key = normalize(moduleId);
        if (key == null) {
            return List.of();
        }
        List<TechGuideEntry> entries = entriesByModule.getOrDefault(key, List.of());
        List<TechGuideEntry> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public List<TechGuideEntry> searchGuideEntries(String query) {
        String normalizedQuery = normalizeQuery(query);
        List<TechGuideEntry> allEntries = new ArrayList<>();
        for (List<TechGuideEntry> moduleEntries : entriesByModule.values()) {
            allEntries.addAll(moduleEntries);
        }

        if (normalizedQuery == null) {
            allEntries.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
            return Collections.unmodifiableList(allEntries);
        }

        List<TechGuideEntry> results = new ArrayList<>();
        for (TechGuideEntry entry : allEntries) {
            if (matches(entry, normalizedQuery)) {
                results.add(entry);
            }
        }

        results.sort((a, b) -> {
            int moduleCompare = a.getModuleId().compareToIgnoreCase(b.getModuleId());
            if (moduleCompare != 0) {
                return moduleCompare;
            }
            return a.getId().compareToIgnoreCase(b.getId());
        });
        return Collections.unmodifiableList(results);
    }

    @Override
    public Optional<TechGuideEntry> findGuideEntry(String moduleId, String entryId) {
        String moduleKey = normalize(moduleId);
        String entryKey = normalize(entryId);
        if (moduleKey == null || entryKey == null) {
            return Optional.empty();
        }

        return entriesByModule.getOrDefault(moduleKey, List.of()).stream()
                .filter(entry -> entryKey.equals(normalize(entry.getId())))
                .findFirst();
    }

    @Override
    public boolean hasUnlockedGuideEntry(Player player, String moduleId, String entryId) {
        return researchService.hasUnlocked(player, moduleId, entryId);
    }

    @Override
    public boolean unlockGuideEntry(Player player, String moduleId, String entryId) {
        return researchService.unlockEntry(player, moduleId, entryId);
    }

    @Override
    public boolean lockGuideEntry(Player player, String moduleId, String entryId) {
        return researchService.lockEntry(player, moduleId, entryId);
    }

    @Override
    public boolean unlockGuideModule(Player player, String moduleId) {
        return researchService.unlockModule(player, moduleId);
    }

    @Override
    public Collection<String> getUnlockedGuideKeys(Player player) {
        return researchService.getUnlockedKeys(player);
    }

    @Override
    public synchronized boolean registerEnchantment(Plugin owner, TechEnchantmentDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return false;
        }
        String key = normalize(definition.getId());
        if (enchantments.containsKey(key)) {
            return false;
        }
        enchantments.put(key, definition);
        enchantOwner.put(key, ownerName(owner));
        hostPlugin.getLogger().info("[API] Enchantment registered: " + key + " by " + ownerName(owner));
        return true;
    }

    @Override
    public Collection<TechEnchantmentDefinition> getEnchantments() {
        List<TechEnchantmentDefinition> defs = new ArrayList<>(enchantments.values());
        defs.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        return Collections.unmodifiableList(defs);
    }

    @Override
    public ItemStack createGuideBook() {
        return guideManager.createGuideBook();
    }

    @Override
    public void openGuide(Player player) {
        guideManager.openGuide(player);
    }

    @Override
    public void openGuideSearch(Player player, String query) {
        guideManager.openGuideSearch(player, query);
    }

    public String getOwnerForMachine(String id) {
        return machineOwner.get(normalize(id));
    }

    public int getTotalGuideEntries() {
        return entriesByModule.values().stream().mapToInt(List::size).sum();
    }

    public synchronized int unregisterOwnedContent(String ownerName) {
        String owner = normalizeOwner(ownerName);
        if (owner == null) {
            return 0;
        }

        int removed = 0;

        List<String> machinesToRemove = machineOwner.entrySet().stream()
                .filter(entry -> owner.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (String machineId : machinesToRemove) {
            if (machineFactory.unregisterMachineDefinition(machineId)) {
                removed++;
            }
            machineOwner.remove(machineId);
        }

        List<String> enchantmentsToRemove = enchantOwner.entrySet().stream()
                .filter(entry -> owner.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (String enchantId : enchantmentsToRemove) {
            if (enchantments.remove(enchantId) != null) {
                removed++;
            }
            enchantOwner.remove(enchantId);
        }

        List<String> entriesToRemove = entryOwner.entrySet().stream()
                .filter(entry -> owner.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (String compositeKey : entriesToRemove) {
            String moduleId = parseModuleId(compositeKey);
            String entryId = parseEntryId(compositeKey);
            if (moduleId != null && entryId != null) {
                List<TechGuideEntry> entries = entriesByModule.get(moduleId);
                if (entries != null && entries.removeIf(entry -> normalize(entry.getId()).equals(entryId))) {
                    removed++;
                }
            }
            entryOwner.remove(compositeKey);
        }

        List<String> modulesToRemove = moduleOwner.entrySet().stream()
                .filter(entry -> owner.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        for (String moduleId : modulesToRemove) {
            List<TechGuideEntry> removedEntries = entriesByModule.remove(moduleId);
            if (removedEntries != null && !removedEntries.isEmpty()) {
                removed += removedEntries.size();
            }

            String prefix = moduleId + ":";
            List<String> orphanKeys = new ArrayList<>();
            for (String key : entryOwner.keySet()) {
                if (key.startsWith(prefix)) {
                    orphanKeys.add(key);
                }
            }
            for (String key : orphanKeys) {
                entryOwner.remove(key);
            }

            if (modules.remove(moduleId) != null) {
                removed++;
            }
            moduleOwner.remove(moduleId);
        }

        return removed;
    }

    public synchronized Set<String> getContentOwners() {
        Set<String> owners = new HashSet<>();
        owners.addAll(machineOwner.values());
        owners.addAll(moduleOwner.values());
        owners.addAll(entryOwner.values());
        owners.addAll(enchantOwner.values());
        owners.removeIf(value -> value == null || value.isBlank());
        return Collections.unmodifiableSet(owners);
    }

    public synchronized OwnerStats getOwnerStats(String ownerName) {
        String owner = normalizeOwner(ownerName);
        if (owner == null) {
            return new OwnerStats(0, 0, 0, 0);
        }

        int machines = (int) machineOwner.values().stream().filter(owner::equalsIgnoreCase).count();
        int modulesCount = (int) moduleOwner.values().stream().filter(owner::equalsIgnoreCase).count();
        int entries = (int) entryOwner.values().stream().filter(owner::equalsIgnoreCase).count();
        int enchants = (int) enchantOwner.values().stream().filter(owner::equalsIgnoreCase).count();
        return new OwnerStats(machines, modulesCount, entries, enchants);
    }

    private String ownerName(Plugin owner) {
        if (owner == null) {
            return hostPlugin.getName();
        }
        return owner.getName();
    }

    private String normalizeOwner(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String parseModuleId(String compositeKey) {
        if (compositeKey == null) {
            return null;
        }
        int separator = compositeKey.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        return compositeKey.substring(0, separator);
    }

    private String parseEntryId(String compositeKey) {
        if (compositeKey == null) {
            return null;
        }
        int separator = compositeKey.indexOf(':');
        if (separator <= 0 || separator + 1 >= compositeKey.length()) {
            return null;
        }
        return compositeKey.substring(separator + 1);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean matches(TechGuideEntry entry, String query) {
        if (contains(entry.getId(), query)) {
            return true;
        }
        if (contains(entry.getModuleId(), query)) {
            return true;
        }
        if (contains(entry.getDisplayName(), query)) {
            return true;
        }

        for (String line : entry.getDescription()) {
            if (contains(line, query)) {
                return true;
            }
        }
        for (String line : entry.getRecipeLines()) {
            if (contains(line, query)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String query) {
        if (value == null || query == null) {
            return false;
        }
        String normalized = MINI_TAG_PATTERN.matcher(value).replaceAll("")
                .toLowerCase(Locale.ROOT);
        return normalized.contains(query);
    }

    public record OwnerStats(int machines, int modules, int entries, int enchantments) {
    }
}
