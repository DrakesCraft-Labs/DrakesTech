package me.jackstar.drakestech.commands;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.DrakesTechPlugin;
import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.api.enchant.TechEnchantmentDefinition;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.api.item.TechItemDefinition;
import me.jackstar.drakestech.api.machine.MachineDefinition;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import me.jackstar.drakestech.network.TechNetworkService;
import me.jackstar.drakestech.recipe.TechCraftingRecipeService;
import me.jackstar.drakestech.recipe.TechRecipeEngine;
import me.jackstar.drakestech.research.TechResearchService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DrakesTechCommand implements TabExecutor {

    private final DrakesTechPlugin plugin;
    private final MachineFactory machineFactory;
    private final MachineManager machineManager;
    private final DrakesTechApi api;
    private final TechResearchService researchService;

    public DrakesTechCommand(DrakesTechPlugin plugin, MachineFactory machineFactory, MachineManager machineManager, DrakesTechApi api,
            TechResearchService researchService) {
        this.plugin = plugin;
        this.machineFactory = machineFactory;
        this.machineManager = machineManager;
        this.api = api;
        this.researchService = researchService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("drakestech.admin")) {
            MessageUtils.send(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if ("give".equalsIgnoreCase(args[0])) {
            return handleGive(sender, label, args);
        }
        if ("guide".equalsIgnoreCase(args[0])) {
            return handleGuide(sender, args);
        }
        if ("search".equalsIgnoreCase(args[0])) {
            return handleSearch(sender, args);
        }
        if ("research".equalsIgnoreCase(args[0])) {
            return handleResearch(sender, args);
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadRuntime();
            MessageUtils.send(sender, "<green>DrakesTech reloaded.</green>");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            return handleList(sender, args);
        }
        if ("diagnostics".equalsIgnoreCase(args[0])) {
            return handleDiagnostics(sender);
        }

        sendUsage(sender, label);
        return true;
    }

    private boolean handleGive(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /" + label + " give <player> <machine|item> <id> [amount]</red>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found or offline.</red>");
            return true;
        }

        String type = args[2].toLowerCase();
        String id = args[3];
        int amount = 1;
        if (args.length >= 5) {
            try {
                amount = Math.max(1, Integer.parseInt(args[4]));
            } catch (NumberFormatException ignored) {
                MessageUtils.send(sender, "<red>Amount must be a number.</red>");
                return true;
            }
        }

        if ("machine".equals(type)) {
            Optional<ItemStack> machineItem = machineFactory.createMachineItem(id);
            if (machineItem.isEmpty()) {
                MessageUtils.send(sender, "<red>Unknown machine id: <gray>" + id + "</gray></red>");
                MessageUtils.send(sender,
                        "<yellow>Available:</yellow> <gray>" + String.join(", ", machineFactory.getSupportedMachineIds()) + "</gray>");
                return true;
            }

            for (int i = 0; i < amount; i++) {
                target.getInventory().addItem(machineItem.get().clone());
            }
            MessageUtils.send(sender,
                    "<green>Given <yellow>" + id + "</yellow> x" + amount + " to <aqua>" + target.getName() + "</aqua>.</green>");
            MessageUtils.send(target, "<green>You received machine: <yellow>" + id + "</yellow> x" + amount + ".</green>");
            return true;
        }

        if ("item".equals(type)) {
            Optional<ItemStack> custom = api.createTechItem(id, amount);
            if (custom.isEmpty()) {
                String available = api.getTechItems().stream().map(TechItemDefinition::getId).sorted().collect(Collectors.joining(", "));
                MessageUtils.send(sender, "<red>Unknown tech item id: <gray>" + id + "</gray></red>");
                MessageUtils.send(sender, "<yellow>Available:</yellow> <gray>" + (available.isBlank() ? "none" : available) + "</gray>");
                return true;
            }

            target.getInventory().addItem(custom.get());
            MessageUtils.send(sender,
                    "<green>Given item <yellow>" + id + "</yellow> x" + amount + " to <aqua>" + target.getName() + "</aqua>.</green>");
            MessageUtils.send(target, "<green>You received tech item: <yellow>" + id + "</yellow> x" + amount + ".</green>");
            return true;
        }

        MessageUtils.send(sender, "<red>Unknown give type. Use <yellow>machine</yellow> or <yellow>item</yellow>.</red>");
        return true;
    }

    private boolean handleGuide(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            MessageUtils.send(sender, "<red>Usage: /drakestech guide <player></red>");
            return true;
        }

        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found or offline.</red>");
            return true;
        }

        api.openGuide(target);
        MessageUtils.send(sender, "<green>Opened tech guide for <aqua>" + target.getName() + "</aqua>.</green>");
        return true;
    }

    private boolean handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, "<red>Usage: /drakestech search [player] <query></red>");
            return true;
        }

        Player target = null;
        int queryStart = 1;

        if (args.length >= 3) {
            Player maybePlayer = Bukkit.getPlayerExact(args[1]);
            if (maybePlayer != null && maybePlayer.isOnline()) {
                target = maybePlayer;
                queryStart = 2;
            }
        }

        if (target == null) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                MessageUtils.send(sender, "<red>Usage from console: /drakestech search <player> <query></red>");
                return true;
            }
        }

        String query = String.join(" ", Arrays.copyOfRange(args, queryStart, args.length)).trim();
        if (query.isBlank()) {
            MessageUtils.send(sender, "<red>You must provide a query.</red>");
            return true;
        }

        api.openGuideSearch(target, query);
        MessageUtils.send(sender, "<green>Opened search for <aqua>" + target.getName() + "</aqua>: <yellow>" + query + "</yellow>.</green>");
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, "<red>Usage: /drakestech list <machines|items|modules|entries|enchantments|addons> [module_id]</red>");
            return true;
        }

        String type = args[1].toLowerCase();
        switch (type) {
            case "machines" -> {
                List<String> ids = machineFactory.getSupportedMachineIds();
                MessageUtils.send(sender, "<yellow>Machines:</yellow> <gray>" + String.join(", ", ids) + "</gray>");
                return true;
            }
            case "modules" -> {
                String modules = api.getGuideModules().stream()
                        .map(TechGuideModule::getId)
                        .sorted()
                        .collect(Collectors.joining(", "));
                MessageUtils.send(sender, "<yellow>Guide modules:</yellow> <gray>" + modules + "</gray>");
                return true;
            }
            case "items" -> {
                String items = api.getTechItems().stream()
                        .map(TechItemDefinition::getId)
                        .sorted()
                        .collect(Collectors.joining(", "));
                MessageUtils.send(sender, "<yellow>Tech items:</yellow> <gray>" + (items.isBlank() ? "none" : items) + "</gray>");
                return true;
            }
            case "entries" -> {
                if (args.length < 3) {
                    MessageUtils.send(sender, "<red>Usage: /drakestech list entries <module_id></red>");
                    return true;
                }
                List<TechGuideEntry> entries = api.getGuideEntries(args[2]);
                String text = entries.stream().map(TechGuideEntry::getId).sorted().collect(Collectors.joining(", "));
                MessageUtils.send(sender, "<yellow>Entries:</yellow> <gray>" + (text.isBlank() ? "none" : text) + "</gray>");
                return true;
            }
            case "enchantments" -> {
                String enchants = api.getEnchantments().stream()
                        .map(TechEnchantmentDefinition::getId)
                        .sorted()
                        .collect(Collectors.joining(", "));
                MessageUtils.send(sender, "<yellow>Enchantments:</yellow> <gray>" + (enchants.isBlank() ? "none" : enchants) + "</gray>");
                return true;
            }
            case "addons" -> {
                List<String> addons = plugin.getLoadedAddons();
                String text = addons.isEmpty() ? "none" : String.join(", ", addons);
                MessageUtils.send(sender, "<yellow>Loaded addons:</yellow> <gray>" + text + "</gray>");
                return true;
            }
            default -> {
                MessageUtils.send(sender, "<red>Unknown list type: <gray>" + type + "</gray></red>");
                return true;
            }
        }
    }

    private boolean handleDiagnostics(CommandSender sender) {
        int loadedMachines = machineManager.getMachines().size();
        int registeredMachineTypes = api.getMachines().size();
        int guideModules = api.getGuideModules().size();
        int guideEntries = api.getGuideModules().stream().mapToInt(module -> api.getGuideEntries(module.getId()).size()).sum();
        int enchantments = api.getEnchantments().size();
        int items = api.getTechItems().size();
        List<String> addons = plugin.getLoadedAddons();
        TechRecipeEngine recipeEngine = plugin.getRecipeEngine();
        TechNetworkService networkService = machineManager.getNetworkService();

        MessageUtils.send(sender, "<yellow>DrakesTech diagnostics:</yellow>");
        MessageUtils.send(sender, "<gray>Machine types:</gray> <aqua>" + registeredMachineTypes + "</aqua>");
        MessageUtils.send(sender, "<gray>Placed machines:</gray> <aqua>" + loadedMachines + "</aqua>");
        MessageUtils.send(sender, "<gray>Guide modules:</gray> <aqua>" + guideModules + "</aqua>");
        MessageUtils.send(sender, "<gray>Guide entries:</gray> <aqua>" + guideEntries + "</aqua>");
        MessageUtils.send(sender, "<gray>Enchantments:</gray> <aqua>" + enchantments + "</aqua>");
        MessageUtils.send(sender, "<gray>Tech items:</gray> <aqua>" + items + "</aqua>");
        MessageUtils.send(sender, "<gray>Active networks:</gray> <aqua>" + networkService.getNetworkCount() + "</aqua>");
        MessageUtils.send(sender, "<gray>Network mapped nodes:</gray> <aqua>" + networkService.getMappedNodeCount() + "</aqua>");
        MessageUtils.send(sender, "<gray>Research enabled:</gray> <aqua>" + researchService.isEnabled() + "</aqua>");
        if (recipeEngine != null) {
            MessageUtils.send(sender, "<gray>Custom smelting recipes:</gray> <aqua>" + recipeEngine.getCustomSmeltingRecipeCount() + "</aqua>");
            MessageUtils.send(sender, "<gray>Vanilla smelting cache:</gray> <aqua>" + recipeEngine.getVanillaSmeltingRecipeCount() + "</aqua>");
        }
        TechCraftingRecipeService craftingRecipeService = plugin.getCraftingRecipeService();
        if (craftingRecipeService != null) {
            MessageUtils.send(sender, "<gray>Crafting recipes:</gray> <aqua>" + craftingRecipeService.getRegisteredRecipeCount() + "</aqua>");
        }
        MessageUtils.send(sender, "<gray>Addons:</gray> <aqua>" + addons.size() + "</aqua>");
        if (!addons.isEmpty()) {
            MessageUtils.send(sender, "<gray>Addon list:</gray> <aqua>" + String.join(", ", addons) + "</aqua>");
        }
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        MessageUtils.send(sender, "<yellow>DrakesTech commands:</yellow>");
        MessageUtils.send(sender, "<gray>/" + label + " give <player> <machine|item> <id> [amount]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " guide [player]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " search [player] <query></gray>");
        MessageUtils.send(sender, "<gray>/" + label + " research <unlock|lock|module|status|list> ...</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " list <machines|items|modules|entries|enchantments|addons> [module_id]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " diagnostics</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " reload</gray>");
    }

    private boolean handleResearch(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendResearchUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target.getUniqueId() == null) {
            MessageUtils.send(sender, "<red>Player not found.</red>");
            return true;
        }

        UUID uuid = target.getUniqueId();
        switch (action) {
            case "unlock" -> {
                if (args.length < 5) {
                    MessageUtils.send(sender, "<red>Usage: /drakestech research unlock <player> <module> <entry></red>");
                    return true;
                }
                String module = args[3];
                String entry = args[4];
                boolean changed = researchService.unlockEntry(uuid, module, entry);
                MessageUtils.send(sender, changed
                        ? "<green>Unlocked <yellow>" + module + "|" + entry + "</yellow> for <aqua>" + target.getName() + "</aqua>.</green>"
                        : "<red>No change. It may already be unlocked or research is disabled.</red>");
                return true;
            }
            case "lock" -> {
                if (args.length < 5) {
                    MessageUtils.send(sender, "<red>Usage: /drakestech research lock <player> <module> <entry></red>");
                    return true;
                }
                String module = args[3];
                String entry = args[4];
                boolean changed = researchService.lockEntry(uuid, module, entry);
                MessageUtils.send(sender, changed
                        ? "<green>Locked <yellow>" + module + "|" + entry + "</yellow> for <aqua>" + target.getName() + "</aqua>.</green>"
                        : "<red>No change. It may already be locked or protected by defaults.</red>");
                return true;
            }
            case "module" -> {
                if (args.length < 4) {
                    MessageUtils.send(sender, "<red>Usage: /drakestech research module <player> <module></red>");
                    return true;
                }
                String module = args[3];
                boolean changed = researchService.unlockModule(uuid, module);
                MessageUtils.send(sender, changed
                        ? "<green>Unlocked module <yellow>" + module + "</yellow> for <aqua>" + target.getName() + "</aqua>.</green>"
                        : "<red>No change. Module may already be unlocked.</red>");
                return true;
            }
            case "status" -> {
                if (args.length < 5) {
                    MessageUtils.send(sender, "<red>Usage: /drakestech research status <player> <module> <entry></red>");
                    return true;
                }
                String module = args[3];
                String entry = args[4];
                boolean unlocked = researchService.hasUnlocked(uuid, module, entry);
                MessageUtils.send(sender, "<gray>Status for <aqua>" + target.getName() + "</aqua>:</gray> <yellow>" + module + "|" + entry
                        + "</yellow> = <aqua>" + unlocked + "</aqua>");
                return true;
            }
            case "list" -> {
                List<String> keys = researchService.getUnlockedKeys(uuid).stream().sorted().toList();
                MessageUtils.send(sender, "<gray>Unlocked entries for <aqua>" + target.getName() + "</aqua>: <yellow>"
                        + (keys.isEmpty() ? "none" : String.join(", ", keys)) + "</yellow>");
                return true;
            }
            default -> {
                sendResearchUsage(sender);
                return true;
            }
        }
    }

    private void sendResearchUsage(CommandSender sender) {
        MessageUtils.send(sender, "<yellow>Research commands:</yellow>");
        MessageUtils.send(sender, "<gray>/drakestech research unlock <player> <module> <entry></gray>");
        MessageUtils.send(sender, "<gray>/drakestech research lock <player> <module> <entry></gray>");
        MessageUtils.send(sender, "<gray>/drakestech research module <player> <module></gray>");
        MessageUtils.send(sender, "<gray>/drakestech research status <player> <module> <entry></gray>");
        MessageUtils.send(sender, "<gray>/drakestech research list <player></gray>");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return matchPrefix(args[0], List.of("give", "guide", "search", "research", "list", "diagnostics", "reload"));
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "give" -> tabGive(args);
            case "guide" -> args.length == 2 ? onlinePlayers(args[1]) : Collections.emptyList();
            case "search" -> args.length == 2 ? onlinePlayers(args[1]) : Collections.emptyList();
            case "research" -> tabResearch(args);
            case "list" -> tabList(args);
            default -> Collections.emptyList();
        };
    }

    private List<String> tabGive(String[] args) {
        if (args.length == 2) {
            return onlinePlayers(args[1]);
        }
        if (args.length == 3) {
            return matchPrefix(args[2], List.of("machine", "item"));
        }
        if (args.length == 4) {
            if ("machine".equalsIgnoreCase(args[2])) {
                return matchPrefix(args[3], machineFactory.getSupportedMachineIds());
            }
            if ("item".equalsIgnoreCase(args[2])) {
                List<String> ids = api.getTechItems().stream().map(TechItemDefinition::getId).sorted().toList();
                return matchPrefix(args[3], ids);
            }
        }
        if (args.length == 5) {
            return matchPrefix(args[4], List.of("1", "8", "16", "32", "64"));
        }
        return Collections.emptyList();
    }

    private List<String> tabResearch(String[] args) {
        if (args.length == 2) {
            return matchPrefix(args[1], List.of("unlock", "lock", "module", "status", "list"));
        }
        if (args.length == 3) {
            return onlinePlayers(args[2]);
        }

        String action = args[1].toLowerCase();
        if ("module".equals(action) && args.length == 4) {
            List<String> modules = api.getGuideModules().stream().map(TechGuideModule::getId).sorted().toList();
            return matchPrefix(args[3], modules);
        }
        if (("unlock".equals(action) || "lock".equals(action) || "status".equals(action)) && args.length == 4) {
            List<String> modules = api.getGuideModules().stream().map(TechGuideModule::getId).sorted().toList();
            return matchPrefix(args[3], modules);
        }
        if (("unlock".equals(action) || "lock".equals(action) || "status".equals(action)) && args.length == 5) {
            List<String> entries = api.getGuideEntries(args[3]).stream().map(TechGuideEntry::getId).sorted().toList();
            return matchPrefix(args[4], entries);
        }

        return Collections.emptyList();
    }

    private List<String> tabList(String[] args) {
        if (args.length == 2) {
            return matchPrefix(args[1], List.of("machines", "items", "modules", "entries", "enchantments", "addons"));
        }
        if (args.length == 3 && "entries".equalsIgnoreCase(args[1])) {
            List<String> modules = api.getGuideModules().stream().map(TechGuideModule::getId).sorted().toList();
            return matchPrefix(args[2], modules);
        }
        return Collections.emptyList();
    }

    private List<String> onlinePlayers(String token) {
        List<String> names = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return matchPrefix(token, names);
    }

    private List<String> matchPrefix(String token, List<String> options) {
        String needle = token == null ? "" : token.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option == null) {
                continue;
            }
            if (option.toLowerCase().startsWith(needle)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
