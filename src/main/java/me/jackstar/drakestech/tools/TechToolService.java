package me.jackstar.drakestech.tools;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.api.item.TechItemDefinition;
import me.jackstar.drakestech.config.DrakesTechSettings;
import me.jackstar.drakestech.item.TechItemRegistry;
import me.jackstar.drakestech.manager.MachineManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TechToolService implements Listener {

    private static final Set<Material> NEVER_BREAK = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.LIGHT);

    private final JavaPlugin plugin;
    private final DrakesTechApi api;
    private final DrakesTechSettings settings;
    private final TechItemRegistry itemRegistry;
    private final MachineManager machineManager;
    private final Set<UUID> drillGuard = ConcurrentHashMap.newKeySet();
    private final Map<FuelBufferKey, Integer> bufferedFuel = new ConcurrentHashMap<>();
    private final Map<UUID, Long> impactCooldownUntilTick = new ConcurrentHashMap<>();
    private final Set<NamespacedKey> recipeKeys = new HashSet<>();

    public TechToolService(
            JavaPlugin plugin,
            DrakesTechApi api,
            DrakesTechSettings settings,
            TechItemRegistry itemRegistry,
            MachineManager machineManager) {
        this.plugin = plugin;
        this.api = api;
        this.settings = settings;
        this.itemRegistry = itemRegistry;
        this.machineManager = machineManager;
    }

    public void start() {
        if (!settings.isToolsEnabled()) {
            return;
        }
        registerFallbackContent();
        registerFallbackRecipes();
    }

    public void reload() {
        unregisterRecipes();
        bufferedFuel.clear();
        impactCooldownUntilTick.clear();
        start();
    }

    public void stop() {
        unregisterRecipes();
        bufferedFuel.clear();
        impactCooldownUntilTick.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrillBreak(BlockBreakEvent event) {
        if (!settings.isToolsEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (drillGuard.contains(playerId)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        String toolId = itemRegistry.readTechItemId(tool).orElse(null);
        DrillProfile profile = resolveDrillProfile(toolId);
        if (profile == null || !profile.enabled) {
            return;
        }

        if (machineManager.getMachineAt(event.getBlock().getLocation()).isPresent()) {
            return;
        }

        List<Block> targets = collectDrillTargets(event.getBlock(), player, profile.radius);
        if (targets.size() <= 1) {
            return;
        }

        List<Block> allowed = new ArrayList<>();
        for (Block block : targets) {
            if (!isDrillBreakable(block)) {
                continue;
            }
            if (machineManager.getMachineAt(block.getLocation()).isPresent()) {
                continue;
            }
            if (!canBreakWithProtectionChecks(player, block)) {
                continue;
            }
            allowed.add(block);
        }

        int blockLimit = Math.min(profile.maxBlocksPerUse, allowed.size());
        if (blockLimit <= 1) {
            return;
        }

        int energyRequired = profile.energyPerBlock * blockLimit;
        if (!consumeEnergy(player, profile.fuelItemId, profile.fuelUnitsPerItem, energyRequired)) {
            MessageUtils.send(player, "<red>No tienes suficiente energia para activar el drill.</red>");
            return;
        }

        event.setCancelled(true);
        drillGuard.add(playerId);
        try {
            for (int i = 0; i < blockLimit; i++) {
                Block block = allowed.get(i);
                if (!isDrillBreakable(block)) {
                    continue;
                }
                block.breakNaturally(tool);
            }
        } finally {
            drillGuard.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onImpactChargeUse(PlayerInteractEvent event) {
        if (!settings.isToolsEnabled() || !settings.isImpactChargeEnabled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        String toolId = itemRegistry.readTechItemId(inHand).orElse(null);
        if (!settings.getImpactChargeItemId().equals(toolId)) {
            return;
        }

        long now = currentServerTick();
        long cooldownUntil = impactCooldownUntilTick.getOrDefault(player.getUniqueId(), 0L);
        if (cooldownUntil > now) {
            long remaining = cooldownUntil - now;
            MessageUtils.send(player, "<red>Impact Charge en cooldown: <yellow>" + remaining + " ticks</yellow>.</red>");
            event.setCancelled(true);
            return;
        }

        String fuelId = firstAvailableItemId(settings.getImpactChargeFuelItemId(), "energy_core", "power_core_t2");
        if (!consumeEnergy(
                player,
                fuelId,
                settings.getImpactChargeFuelUnitsPerItem(),
                settings.getImpactChargeEnergyPerUse())) {
            MessageUtils.send(player, "<red>No tienes energia suficiente para Impact Charge.</red>");
            event.setCancelled(true);
            return;
        }

        consumeHeldItem(player);
        impactCooldownUntilTick.put(player.getUniqueId(), now + settings.getImpactChargeCooldownTicks());
        event.setCancelled(true);

        Block targetBlock = event.getClickedBlock();
        Location center = targetBlock != null
                ? targetBlock.getLocation().add(0.5D, 0.5D, 0.5D)
                : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2.0D));
        detonateImpact(player, center);
    }

    private void detonateImpact(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        float explosionYield = settings.getImpactChargeDamageExplosionYield();
        if (explosionYield > 0.0F) {
            world.createExplosion(center, explosionYield, false, false, player);
        }

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        int maxBlocks = settings.getImpactChargeMaxBlocks();
        double radius = settings.getImpactChargeRadius();
        int ceil = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;

        int broken = 0;
        for (int x = -ceil; x <= ceil && broken < maxBlocks; x++) {
            for (int y = -ceil; y <= ceil && broken < maxBlocks; y++) {
                for (int z = -ceil; z <= ceil && broken < maxBlocks; z++) {
                    double distanceSquared = (x * x) + (y * y) + (z * z);
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    Block block = world.getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!isImpactBreakable(block)) {
                        continue;
                    }
                    if (!settings.isImpactChargeBreakContainers() && block.getState() instanceof TileState) {
                        continue;
                    }
                    if (machineManager.getMachineAt(block.getLocation()).isPresent()) {
                        continue;
                    }
                    if (!canBreakWithProtectionChecks(player, block)) {
                        continue;
                    }

                    if (block.breakNaturally()) {
                        broken++;
                    }
                }
            }
        }

        MessageUtils.send(player, "<gray>Impact Charge activo.</gray> <yellow>Bloques rotos:</yellow> <aqua>" + broken + "</aqua>");
    }

    private List<Block> collectDrillTargets(Block center, Player player, int radius) {
        List<Block> targets = new ArrayList<>();
        DrillPlane plane = determineDrillPlane(player);

        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                Block block = switch (plane) {
                    case HORIZONTAL -> center.getRelative(a, 0, b);
                    case VERTICAL_X -> center.getRelative(0, a, b);
                    case VERTICAL_Z -> center.getRelative(a, b, 0);
                };
                targets.add(block);
            }
        }
        return targets;
    }

    private DrillPlane determineDrillPlane(Player player) {
        float pitch = Math.abs(player.getLocation().getPitch());
        if (pitch >= 60.0F) {
            return DrillPlane.HORIZONTAL;
        }

        float yaw = player.getLocation().getYaw();
        while (yaw < 0.0F) {
            yaw += 360.0F;
        }
        yaw = yaw % 360.0F;

        boolean eastWest = (yaw >= 45.0F && yaw < 135.0F) || (yaw >= 225.0F && yaw < 315.0F);
        return eastWest ? DrillPlane.VERTICAL_Z : DrillPlane.VERTICAL_X;
    }

    private boolean canBreakWithProtectionChecks(Player player, Block block) {
        if (block == null || block.getType().isAir()) {
            return false;
        }
        PluginManager manager = plugin.getServer().getPluginManager();
        BlockBreakEvent probe = new BlockBreakEvent(block, player);
        manager.callEvent(probe);
        return !probe.isCancelled();
    }

    private boolean isDrillBreakable(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        return !NEVER_BREAK.contains(type);
    }

    private boolean isImpactBreakable(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        return !NEVER_BREAK.contains(type);
    }

    private DrillProfile resolveDrillProfile(String toolId) {
        if (toolId == null) {
            return null;
        }
        if (settings.isDrillMk1Enabled() && settings.getDrillMk1ItemId().equals(toolId)) {
            String fuel = firstAvailableItemId(settings.getDrillMk1FuelItemId(), "energy_core", "power_core_t1");
            return new DrillProfile(
                    true,
                    fuel,
                    settings.getDrillMk1FuelUnitsPerItem(),
                    settings.getDrillMk1EnergyPerBlock(),
                    settings.getDrillMk1MaxBlocksPerUse(),
                    1);
        }
        if (settings.isDrillMk2Enabled() && settings.getDrillMk2ItemId().equals(toolId)) {
            String fuel = firstAvailableItemId(settings.getDrillMk2FuelItemId(), "energy_core", "power_core_t2");
            return new DrillProfile(
                    true,
                    fuel,
                    settings.getDrillMk2FuelUnitsPerItem(),
                    settings.getDrillMk2EnergyPerBlock(),
                    settings.getDrillMk2MaxBlocksPerUse(),
                    2);
        }
        return null;
    }

    private boolean consumeEnergy(Player player, String fuelItemId, int unitsPerItem, int unitsRequired) {
        if (unitsRequired <= 0) {
            return true;
        }
        if (fuelItemId == null || fuelItemId.isBlank()) {
            return false;
        }

        FuelBufferKey key = new FuelBufferKey(player.getUniqueId(), normalize(fuelItemId));
        int buffered = bufferedFuel.getOrDefault(key, 0);
        if (buffered >= unitsRequired) {
            bufferedFuel.put(key, buffered - unitsRequired);
            return true;
        }

        int missingUnits = unitsRequired - buffered;
        int itemsNeeded = (int) Math.ceil((double) missingUnits / (double) Math.max(1, unitsPerItem));
        if (!hasTechItems(player.getInventory(), fuelItemId, itemsNeeded)) {
            return false;
        }

        removeTechItems(player.getInventory(), fuelItemId, itemsNeeded);
        int gained = itemsNeeded * Math.max(1, unitsPerItem);
        int newBuffer = Math.max(0, gained - missingUnits);
        bufferedFuel.put(key, newBuffer);
        return true;
    }

    private boolean hasTechItems(PlayerInventory inventory, String itemId, int requiredAmount) {
        if (requiredAmount <= 0) {
            return true;
        }
        int count = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (!itemRegistry.matches(stack, itemId)) {
                continue;
            }
            count += stack.getAmount();
            if (count >= requiredAmount) {
                return true;
            }
        }
        return false;
    }

    private void removeTechItems(PlayerInventory inventory, String itemId, int amount) {
        int remaining = Math.max(0, amount);
        if (remaining == 0) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (!itemRegistry.matches(stack, itemId)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                inventory.setItem(slot, null);
            } else {
                inventory.setItem(slot, stack);
            }
            remaining -= take;
        }
    }

    private void consumeHeldItem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null || main.getType().isAir()) {
            return;
        }
        int amount = main.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        main.setAmount(amount - 1);
        player.getInventory().setItemInMainHand(main);
    }

    private long currentServerTick() {
        return Bukkit.getCurrentTick();
    }

    private void registerFallbackContent() {
        if (api.getGuideModules().stream().noneMatch(module -> "weapons".equalsIgnoreCase(module.getId()))) {
            api.registerGuideModule(plugin, new TechGuideModule(
                    "weapons",
                    "<gradient:#ff5c5c:#ffbf5c><b>Weapons</b></gradient>",
                    Material.NETHERITE_AXE,
                    List.of("<gray>Herramientas ofensivas y minado avanzado.</gray>")));
        }

        registerFallbackItem(
                settings.getDrillMk1ItemId(),
                "<gradient:#7de0ff:#4ab6ff><b>Drill MK-I 3x3</b></gradient>",
                Material.DIAMOND_PICKAXE,
                94001,
                List.of(
                        "<gray>Excava en area 3x3 segun orientacion del jugador.</gray>",
                        "<gray>Consume energia por bloque roto.</gray>"));

        registerFallbackItem(
                settings.getDrillMk2ItemId(),
                "<gradient:#5cf4ff:#7a7aff><b>Drill MK-II 5x5</b></gradient>",
                Material.NETHERITE_PICKAXE,
                94002,
                List.of(
                        "<gray>Excava en area 5x5 para mineria masiva.</gray>",
                        "<gray>Costo energetico superior y controlado.</gray>"));

        registerFallbackItem(
                settings.getImpactChargeItemId(),
                "<gradient:#ffb85c:#ff5c5c><b>Impact Charge</b></gradient>",
                Material.FIRE_CHARGE,
                94003,
                List.of(
                        "<gray>Detonacion controlada sin destruir maquinas.</gray>",
                        "<gray>Respeta protecciones por bloque.</gray>"));

        registerFallbackGuideEntry(
                settings.getDrillMk1ItemId(),
                "weapons",
                "<gradient:#7de0ff:#4ab6ff><b>Drill MK-I 3x3</b></gradient>",
                Material.DIAMOND_PICKAXE,
                List.of(
                        "<gray>Herramienta de mineria en area.</gray>",
                        "<gray>Requiere energia del inventario para activar el modo AOE.</gray>"),
                List.of(
                        "<gray>Top row:</gray> <yellow>item:hardened_metal_t1 | item:power_core_t1 | item:hardened_metal_t1</yellow>",
                        "<gray>Middle row:</gray> <yellow>item:copper_wire_t1 | Iron Pickaxe | item:copper_wire_t1</yellow>",
                        "<gray>Bottom row:</gray> <yellow>- | item:copper_plate_t1 | -</yellow>"));

        registerFallbackGuideEntry(
                settings.getDrillMk2ItemId(),
                "weapons",
                "<gradient:#5cf4ff:#7a7aff><b>Drill MK-II 5x5</b></gradient>",
                Material.NETHERITE_PICKAXE,
                List.of(
                        "<gray>Version industrial para extraccion extrema.</gray>",
                        "<gray>Mayor area, mayor costo energetico.</gray>"),
                List.of(
                        "<gray>Top row:</gray> <yellow>item:hardened_metal_t2 | item:power_core_t2 | item:hardened_metal_t2</yellow>",
                        "<gray>Middle row:</gray> <yellow>item:copper_wire_t2 | item:drill_mk1_3x3 | item:copper_wire_t2</yellow>",
                        "<gray>Bottom row:</gray> <yellow>item:redstone_alloy_ingot_t2 | Diamond Pickaxe | item:redstone_alloy_ingot_t2</yellow>"));

        registerFallbackGuideEntry(
                settings.getImpactChargeItemId(),
                "weapons",
                "<gradient:#ffb85c:#ff5c5c><b>Impact Charge</b></gradient>",
                Material.FIRE_CHARGE,
                List.of(
                        "<gray>Carga explosiva controlada para mineria profunda.</gray>",
                        "<gray>No rompe maquinas DrakesTech y aplica checks de proteccion.</gray>"),
                List.of(
                        "<gray>Top row:</gray> <yellow>item:redstone_alloy_ingot_t2 | Gunpowder | item:redstone_alloy_ingot_t2</yellow>",
                        "<gray>Middle row:</gray> <yellow>item:hardened_metal_t2 | item:power_core_t2 | item:hardened_metal_t2</yellow>",
                        "<gray>Bottom row:</gray> <yellow>item:copper_wire_t2 | Fire Charge | item:copper_wire_t2</yellow>"));
    }

    private void registerFallbackItem(String id, String displayName, Material material, int customModelData, List<String> lore) {
        if (id == null || id.isBlank() || api.findTechItem(id).isPresent()) {
            return;
        }
        api.registerTechItem(plugin, new TechItemDefinition(id, displayName, material, lore, customModelData, false));
    }

    private void registerFallbackGuideEntry(
            String entryId,
            String moduleId,
            String displayName,
            Material icon,
            List<String> description,
            List<String> recipe) {
        if (entryId == null || entryId.isBlank()) {
            return;
        }
        if (api.findGuideEntry(moduleId, entryId).isPresent()) {
            return;
        }
        ItemStack preview = api.createTechItem(entryId).orElseGet(() -> new ItemBuilder(icon).name(displayName).lore(description).build());
        api.registerGuideEntry(plugin, new TechGuideEntry(entryId, moduleId, displayName, icon, description, recipe, preview));
    }

    private void registerFallbackRecipes() {
        String hardenedT1 = tokenForItemOrMaterial("hardened_metal_t1", Material.IRON_INGOT);
        String hardenedT2 = tokenForItemOrMaterial("hardened_metal_t2", Material.DIAMOND);
        String wireT1 = tokenForItemOrMaterial("copper_wire_t1", Material.REDSTONE);
        String wireT2 = tokenForItemOrMaterial("copper_wire_t2", Material.REDSTONE);
        String plateT1 = tokenForItemOrMaterial("copper_plate_t1", Material.COPPER_INGOT);
        String powerT1 = tokenForItemOrMaterial("power_core_t1", Material.REDSTONE_BLOCK);
        String powerT2 = tokenForItemOrMaterial("power_core_t2", Material.LAPIS_BLOCK);
        String alloyT2 = tokenForItemOrMaterial("redstone_alloy_ingot_t2", Material.IRON_INGOT);

        registerShapedRecipe(
                "tool_drill_mk1_3x3",
                settings.getDrillMk1ItemId(),
                new String[] {"ABA", "CDC", " E "},
                Map.of(
                        'A', hardenedT1,
                        'B', powerT1,
                        'C', wireT1,
                        'D', "material:IRON_PICKAXE",
                        'E', plateT1));

        registerShapedRecipe(
                "tool_drill_mk2_5x5",
                settings.getDrillMk2ItemId(),
                new String[] {"ABA", "CDC", "EFE"},
                Map.of(
                        'A', hardenedT2,
                        'B', powerT2,
                        'C', wireT2,
                        'D', "item:drill_mk1_3x3",
                        'E', alloyT2,
                        'F', "material:DIAMOND_PICKAXE"));

        registerShapedRecipe(
                "tool_impact_charge",
                settings.getImpactChargeItemId(),
                new String[] {"ABA", "CDC", "EFE"},
                Map.of(
                        'A', alloyT2,
                        'B', "material:GUNPOWDER",
                        'C', hardenedT2,
                        'D', powerT2,
                        'E', wireT2,
                        'F', "material:FIRE_CHARGE"));
    }

    private void registerShapedRecipe(String keySuffix, String outputItemId, String[] shape, Map<Character, String> ingredients) {
        if (outputItemId == null || outputItemId.isBlank()) {
            return;
        }
        ItemStack output = api.createTechItem(outputItemId).orElse(null);
        if (output == null || output.getType().isAir()) {
            plugin.getLogger().warning("[Tools] Could not create output item for recipe: " + outputItemId);
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, keySuffix.toLowerCase(Locale.ROOT));
        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, output);
        recipe.shape(shape);

        for (Map.Entry<Character, String> ingredient : ingredients.entrySet()) {
            RecipeChoice choice = resolveIngredient(ingredient.getValue());
            if (choice == null) {
                plugin.getLogger().warning("[Tools] Invalid ingredient '" + ingredient.getValue() + "' for recipe " + keySuffix + ".");
                return;
            }
            recipe.setIngredient(ingredient.getKey(), choice);
        }

        if (Bukkit.addRecipe(recipe)) {
            recipeKeys.add(key);
        }
    }

    private RecipeChoice resolveIngredient(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String clean = token.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.startsWith("item:")) {
            String itemId = normalize(clean.substring("item:".length()));
            if (itemId == null) {
                return null;
            }
            ItemStack stack = api.createTechItem(itemId).orElse(null);
            return stack == null ? null : new RecipeChoice.ExactChoice(stack);
        }
        if (lower.startsWith("material:")) {
            Material material = parseMaterial(clean.substring("material:".length()));
            return material == null ? null : new RecipeChoice.MaterialChoice(material);
        }

        Material material = parseMaterial(clean);
        if (material != null) {
            return new RecipeChoice.MaterialChoice(material);
        }
        return null;
    }

    private Material parseMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim();
        try {
            return Material.valueOf(clean.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Material.matchMaterial(clean);
        }
    }

    private void unregisterRecipes() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }
        recipeKeys.clear();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstAvailableItemId(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (normalized == null) {
                continue;
            }
            if (api.findTechItem(normalized).isPresent()) {
                return normalized;
            }
        }
        return null;
    }

    private String tokenForItemOrMaterial(String preferredItemId, Material fallbackMaterial) {
        String existing = firstAvailableItemId(preferredItemId);
        if (existing != null) {
            return "item:" + existing;
        }
        return "material:" + fallbackMaterial.name();
    }

    private enum DrillPlane {
        HORIZONTAL,
        VERTICAL_X,
        VERTICAL_Z
    }

    private record DrillProfile(
            boolean enabled,
            String fuelItemId,
            int fuelUnitsPerItem,
            int energyPerBlock,
            int maxBlocksPerUse,
            int radius) {
    }

    private record FuelBufferKey(UUID playerId, String fuelItemId) {
    }
}
