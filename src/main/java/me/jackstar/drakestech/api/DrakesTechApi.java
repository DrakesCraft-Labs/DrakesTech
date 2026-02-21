package me.jackstar.drakestech.api;

import me.jackstar.drakestech.api.enchant.TechEnchantmentDefinition;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.api.machine.MachineDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DrakesTechApi {

    boolean registerMachine(Plugin owner, MachineDefinition definition);

    boolean unregisterMachine(String machineId);

    Optional<MachineDefinition> findMachine(String machineId);

    Collection<MachineDefinition> getMachines();

    boolean registerGuideModule(Plugin owner, TechGuideModule module);

    boolean registerGuideEntry(Plugin owner, TechGuideEntry entry);

    Collection<TechGuideModule> getGuideModules();

    List<TechGuideEntry> getGuideEntries(String moduleId);

    List<TechGuideEntry> searchGuideEntries(String query);

    Optional<TechGuideEntry> findGuideEntry(String moduleId, String entryId);

    boolean hasUnlockedGuideEntry(Player player, String moduleId, String entryId);

    boolean unlockGuideEntry(Player player, String moduleId, String entryId);

    boolean lockGuideEntry(Player player, String moduleId, String entryId);

    boolean unlockGuideModule(Player player, String moduleId);

    Collection<String> getUnlockedGuideKeys(Player player);

    boolean registerEnchantment(Plugin owner, TechEnchantmentDefinition definition);

    Collection<TechEnchantmentDefinition> getEnchantments();

    ItemStack createGuideBook();

    void openGuide(Player player);

    void openGuideSearch(Player player, String query);
}
