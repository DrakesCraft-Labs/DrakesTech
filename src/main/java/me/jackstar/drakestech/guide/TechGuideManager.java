package me.jackstar.drakestech.guide;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.api.DrakesTechApi;
import me.jackstar.drakestech.api.guide.TechGuideEntry;
import me.jackstar.drakestech.api.guide.TechGuideModule;
import me.jackstar.drakestech.config.DrakesTechSettings;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import me.jackstar.drakestech.research.TechResearchService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TechGuideManager implements Listener {

    private static final String GUIDE_BOOK_CUSTOM_ID = "drakestech_guide_book";
    private static final String GUIDE_TARGET_CUSTOM_ID = "drakestech_guide_target";

    private static final String TARGET_PREV = "nav_prev";
    private static final String TARGET_NEXT = "nav_next";
    private static final String TARGET_BACK = "nav_back";
    private static final String TARGET_SEARCH = "nav_search";

    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int SEARCH_SLOT = 50;
    private static final int NEXT_SLOT = 53;

    private final JavaPlugin plugin;
    private final DrakesTechSettings settings;
    private final NbtItemHandler nbtItemHandler;
    private final TechResearchService researchService;
    private DrakesTechApi api;

    public TechGuideManager(JavaPlugin plugin, DrakesTechSettings settings, NbtItemHandler nbtItemHandler,
            TechResearchService researchService) {
        this.plugin = plugin;
        this.settings = settings;
        this.nbtItemHandler = nbtItemHandler;
        this.researchService = researchService;
    }

    public void bindApi(DrakesTechApi api) {
        this.api = api;
    }

    public ItemStack createGuideBook() {
        ItemStack guideBook = new ItemBuilder(settings.getGuideBookMaterial())
                .name(settings.getGuideBookName())
                .lore(settings.getGuideBookLore())
                .build();
        nbtItemHandler.setCustomItemId(guideBook, GUIDE_BOOK_CUSTOM_ID);
        return guideBook;
    }

    public void openGuide(Player player) {
        openGuide(player, 1);
    }

    public void openGuideSearch(Player player, String query) {
        openSearch(player, query, 1);
    }

    private void openGuide(Player player, int page) {
        if (player == null || api == null) {
            return;
        }

        List<TechGuideModule> modules = new ArrayList<>(api.getGuideModules());
        int totalPages = calculateTotalPages(modules.size());
        int safePage = clampPage(page, totalPages);

        String title = settings.getGuideMainTitle() + " <gray>(" + safePage + "/" + totalPages + ")</gray>";
        Inventory inventory = Bukkit.createInventory(new GuideModulesHolder(safePage), INVENTORY_SIZE, MessageUtils.parse(title));

        List<TechGuideModule> pageItems = paginate(modules, safePage);
        for (int i = 0; i < pageItems.size(); i++) {
            TechGuideModule module = pageItems.get(i);
            List<String> lore = new ArrayList<>(module.getDescription());
            lore.add("<dark_gray> ");
            lore.add("<yellow>Click to open module</yellow>");
            ItemStack item = new ItemBuilder(module.getIcon())
                    .name(module.getDisplayName())
                    .lore(lore)
                    .build();
            tagGuideTarget(item, module.getId());
            inventory.setItem(i, item);
        }

        addCommonControls(inventory, safePage, totalPages, true, "<gray>Browse all guide entries.</gray>");
        player.openInventory(inventory);
    }

    private void openModule(Player player, String moduleId, int page) {
        if (player == null || moduleId == null || api == null) {
            return;
        }

        String normalizedModule = normalize(moduleId);
        List<TechGuideEntry> entries = api.getGuideEntries(normalizedModule);
        int totalPages = calculateTotalPages(entries.size());
        int safePage = clampPage(page, totalPages);

        String title = "<gold>Module:</gold> <yellow>" + normalizedModule + "</yellow> <gray>(" + safePage + "/" + totalPages + ")</gray>";
        Inventory inventory = Bukkit.createInventory(
                new GuideModuleEntriesHolder(normalizedModule, safePage),
                INVENTORY_SIZE,
                MessageUtils.parse(title));

        List<TechGuideEntry> pageItems = paginate(entries, safePage);
        for (int i = 0; i < pageItems.size(); i++) {
            TechGuideEntry entry = pageItems.get(i);
            boolean unlocked = researchService.hasUnlocked(player, normalizedModule, entry.getId());
            List<String> lore = new ArrayList<>(entry.getDescription());
            lore.add("<dark_gray> ");
            if (unlocked) {
                lore.add("<yellow>Click to see recipe details</yellow>");
            } else {
                lore.add("<red>Locked</red>");
                lore.add("<gray>Unlock with admin command:</gray>");
                lore.add("<gray>/drakestech research unlock <player> " + normalizedModule + " " + normalize(entry.getId()) + "</gray>");
            }

            ItemStack icon = entry.getPreviewItem();
            ItemStack item;
            if (!unlocked) {
                item = new ItemBuilder(Material.BARRIER)
                        .name("<red><b>Locked</b></red> <gray>" + entry.getDisplayName() + "</gray>")
                        .lore(lore)
                        .build();
            } else if (icon == null || icon.getType().isAir()) {
                item = new ItemBuilder(entry.getIcon())
                        .name(entry.getDisplayName())
                        .lore(lore)
                        .build();
            } else {
                item = icon.clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(MessageUtils.parse(entry.getDisplayName()));
                    List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                    for (String line : lore) {
                        loreComponents.add(MessageUtils.parse(line));
                    }
                    meta.lore(loreComponents);
                    item.setItemMeta(meta);
                }
            }

            tagGuideTarget(item, normalize(entry.getId()));
            inventory.setItem(i, item);
        }

        addCommonControls(inventory, safePage, totalPages, true, "<gray>Browse all guide entries.</gray>");
        addBackControl(inventory, "<gray>Return to module list.</gray>");
        player.openInventory(inventory);
    }

    private void openSearch(Player player, String query, int page) {
        if (player == null || api == null) {
            return;
        }

        String normalizedQuery = normalizeQuery(query);
        String effectiveQuery = normalizedQuery == null ? "" : normalizedQuery;
        List<TechGuideEntry> results = api.searchGuideEntries(effectiveQuery);

        int totalPages = calculateTotalPages(results.size());
        int safePage = clampPage(page, totalPages);
        String label = effectiveQuery.isBlank() ? "all" : effectiveQuery;
        String title = "<gold>Search:</gold> <yellow>" + label + "</yellow> <gray>(" + safePage + "/" + totalPages + ")</gray>";

        Inventory inventory = Bukkit.createInventory(
                new GuideSearchHolder(effectiveQuery, safePage),
                INVENTORY_SIZE,
                MessageUtils.parse(title));

        List<TechGuideEntry> pageItems = paginate(results, safePage);
        for (int i = 0; i < pageItems.size(); i++) {
            TechGuideEntry entry = pageItems.get(i);
            boolean unlocked = researchService.hasUnlocked(player, entry.getModuleId(), entry.getId());
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Module:</gray> <yellow>" + entry.getModuleId() + "</yellow>");
            lore.addAll(entry.getDescription());
            lore.add("<dark_gray> ");
            if (unlocked) {
                lore.add("<yellow>Click to open recipe details</yellow>");
            } else {
                lore.add("<red>Locked</red>");
                lore.add("<gray>/drakestech research unlock <player> " + normalize(entry.getModuleId()) + " " + normalize(entry.getId()) + "</gray>");
            }

            ItemStack item;
            if (!unlocked) {
                item = new ItemBuilder(Material.BARRIER)
                        .name("<red><b>Locked</b></red> <gray>" + entry.getDisplayName() + "</gray>")
                        .lore(lore)
                        .build();
            } else {
                ItemStack icon = entry.getPreviewItem();
                item = (icon == null || icon.getType().isAir())
                        ? new ItemBuilder(entry.getIcon()).name(entry.getDisplayName()).lore(lore).build()
                        : icon.clone();
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MessageUtils.parse(entry.getDisplayName()));
                List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(MessageUtils.parse(line));
                }
                meta.lore(loreComponents);
                item.setItemMeta(meta);
            }

            tagGuideTarget(item, normalize(entry.getModuleId()) + "|" + normalize(entry.getId()));
            inventory.setItem(i, item);
        }

        addCommonControls(inventory, safePage, totalPages, false,
                "<gray>Use /drakestech search <text> to filter.</gray>");
        addBackControl(inventory, "<gray>Return to module list.</gray>");
        player.openInventory(inventory);
    }

    private void openEntryDetail(Player player, String moduleId, String entryId, GuideBackTarget backTarget) {
        if (player == null || moduleId == null || entryId == null || api == null) {
            return;
        }

        TechGuideEntry entry = api.findGuideEntry(moduleId, entryId).orElse(null);
        if (entry == null) {
            return;
        }
        boolean unlocked = researchService.hasUnlocked(player, moduleId, entryId);

        Inventory inventory = Bukkit.createInventory(
                new GuideEntryDetailHolder(backTarget),
                27,
                MessageUtils.parse("<gold>Recipe:</gold> <yellow>" + entry.getDisplayName() + "</yellow>"));

        ItemStack center;
        if (!unlocked) {
            center = new ItemBuilder(Material.BARRIER)
                    .name("<red><b>Locked Entry</b></red>")
                    .lore(
                            "<gray>Entry:</gray> " + entry.getDisplayName(),
                            "<gray>Unlock with:</gray>",
                            "<yellow>/drakestech research unlock <player> " + normalize(moduleId) + " " + normalize(entryId) + "</yellow>")
                    .build();
        } else {
            center = entry.getPreviewItem();
            if (center == null || center.getType().isAir()) {
                center = new ItemBuilder(entry.getIcon())
                        .name(entry.getDisplayName())
                        .lore(entry.getDescription())
                        .build();
            } else {
                center = center.clone();
            }
        }
        inventory.setItem(13, center);

        ItemStack details = new ItemBuilder(Material.BOOK)
                .name(unlocked ? "<aqua><b>How to craft</b></aqua>" : "<red><b>Entry Locked</b></red>")
                .lore(unlocked ? buildRecipeLore(entry) : List.of("<gray>You cannot view recipe details yet.</gray>"))
                .build();
        inventory.setItem(15, details);

        ItemStack back = new ItemBuilder(Material.ARROW)
                .name("<yellow>Back</yellow>")
                .lore("<gray>Return to previous menu.</gray>")
                .build();
        tagGuideTarget(back, TARGET_BACK);
        inventory.setItem(11, back);

        player.openInventory(inventory);
    }

    private List<String> buildRecipeLore(TechGuideEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Item:</gray> " + entry.getDisplayName());
        if (!entry.getDescription().isEmpty()) {
            lore.add("<dark_gray> ");
            lore.add("<gray>Description:</gray>");
            lore.addAll(entry.getDescription());
        }
        if (!entry.getRecipeLines().isEmpty()) {
            lore.add("<dark_gray> ");
            lore.add("<gray>Recipe:</gray>");
            for (String line : entry.getRecipeLines()) {
                lore.add("<yellow>-</yellow> " + line);
            }
        } else {
            lore.add("<dark_gray> ");
            lore.add("<gray>Recipe not defined yet.</gray>");
        }
        return lore;
    }

    private void addCommonControls(Inventory inventory, int page, int totalPages, boolean includeSearchShortcut, String infoLine) {
        if (page > 1) {
            ItemStack previous = new ItemBuilder(Material.ARROW)
                    .name("<yellow>Previous Page</yellow>")
                    .lore("<gray>Go to page " + (page - 1) + ".</gray>")
                    .build();
            tagGuideTarget(previous, TARGET_PREV);
            inventory.setItem(PREV_SLOT, previous);
        }

        if (page < totalPages) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name("<yellow>Next Page</yellow>")
                    .lore("<gray>Go to page " + (page + 1) + ".</gray>")
                    .build();
            tagGuideTarget(next, TARGET_NEXT);
            inventory.setItem(NEXT_SLOT, next);
        }

        ItemStack info = new ItemBuilder(Material.PAPER)
                .name("<aqua><b>Guide Controls</b></aqua>")
                .lore(
                        "<gray>Page:</gray> <yellow>" + page + "/" + totalPages + "</yellow>",
                        infoLine)
                .build();
        inventory.setItem(INFO_SLOT, info);

        if (includeSearchShortcut) {
            ItemStack search = new ItemBuilder(Material.COMPASS)
                    .name("<gold><b>Search</b></gold>")
                    .lore(
                            "<gray>Click to browse all entries.</gray>",
                            "<gray>Use /drakestech search <text> to filter.</gray>")
                    .build();
            tagGuideTarget(search, TARGET_SEARCH);
            inventory.setItem(SEARCH_SLOT, search);
        }
    }

    private void addBackControl(Inventory inventory, String loreLine) {
        ItemStack back = new ItemBuilder(Material.BARRIER)
                .name("<red><b>Back</b></red>")
                .lore(loreLine)
                .build();
        tagGuideTarget(back, TARGET_BACK);
        inventory.setItem(48, back);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!settings.isAutoGiveGuideOnFirstJoin()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> giveGuideBookIfMissing(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuideInteract(PlayerInteractEvent event) {
        if (!settings.isOpenGuideOnRightClick()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isGuideBook(item)) {
            return;
        }

        openGuide(event.getPlayer());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof GuideModulesHolder)
                && !(holder instanceof GuideModuleEntriesHolder)
                && !(holder instanceof GuideEntryDetailHolder)
                && !(holder instanceof GuideSearchHolder)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        String target = readGuideTag(clicked);
        if (target == null) {
            return;
        }

        if (holder instanceof GuideModulesHolder modulesHolder) {
            handleModulesClick(player, modulesHolder, target);
            return;
        }

        if (holder instanceof GuideModuleEntriesHolder moduleHolder) {
            handleModuleEntriesClick(player, moduleHolder, target);
            return;
        }

        if (holder instanceof GuideSearchHolder searchHolder) {
            handleSearchClick(player, searchHolder, target);
            return;
        }

        if (holder instanceof GuideEntryDetailHolder detailHolder && TARGET_BACK.equals(target)) {
            GuideBackTarget back = detailHolder.backTarget;
            if (back == null) {
                openGuide(player, 1);
                return;
            }

            if (back.origin == GuideBackOrigin.MODULE) {
                openModule(player, back.moduleId, back.page);
                return;
            }

            if (back.origin == GuideBackOrigin.SEARCH) {
                openSearch(player, back.query, back.page);
                return;
            }

            openGuide(player, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof GuideModulesHolder
                || holder instanceof GuideModuleEntriesHolder
                || holder instanceof GuideSearchHolder
                || holder instanceof GuideEntryDetailHolder) {
            event.setCancelled(true);
        }
    }

    public void giveGuideBookIfMissing(Player player) {
        if (player == null) {
            return;
        }

        if (hasGuideBook(player)) {
            return;
        }
        player.getInventory().addItem(createGuideBook());
    }

    private void handleModulesClick(Player player, GuideModulesHolder holder, String target) {
        if (TARGET_NEXT.equals(target)) {
            openGuide(player, holder.page + 1);
            return;
        }
        if (TARGET_PREV.equals(target)) {
            openGuide(player, holder.page - 1);
            return;
        }
        if (TARGET_SEARCH.equals(target)) {
            openSearch(player, "", 1);
            return;
        }
        openModule(player, target, 1);
    }

    private void handleModuleEntriesClick(Player player, GuideModuleEntriesHolder holder, String target) {
        if (TARGET_NEXT.equals(target)) {
            openModule(player, holder.moduleId, holder.page + 1);
            return;
        }
        if (TARGET_PREV.equals(target)) {
            openModule(player, holder.moduleId, holder.page - 1);
            return;
        }
        if (TARGET_BACK.equals(target)) {
            openGuide(player, 1);
            return;
        }
        if (TARGET_SEARCH.equals(target)) {
            openSearch(player, "", 1);
            return;
        }

        GuideBackTarget back = new GuideBackTarget(GuideBackOrigin.MODULE, holder.moduleId, null, holder.page);
        openEntryDetail(player, holder.moduleId, target, back);
    }

    private void handleSearchClick(Player player, GuideSearchHolder holder, String target) {
        if (TARGET_NEXT.equals(target)) {
            openSearch(player, holder.query, holder.page + 1);
            return;
        }
        if (TARGET_PREV.equals(target)) {
            openSearch(player, holder.query, holder.page - 1);
            return;
        }
        if (TARGET_BACK.equals(target)) {
            openGuide(player, 1);
            return;
        }

        String[] split = target.split("\\|", 2);
        if (split.length != 2) {
            return;
        }

        GuideBackTarget back = new GuideBackTarget(GuideBackOrigin.SEARCH, null, holder.query, holder.page);
        openEntryDetail(player, split[0], split[1], back);
    }

    private boolean hasGuideBook(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isGuideBook(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGuideBook(ItemStack item) {
        return nbtItemHandler.hasCustomItemId(item, GUIDE_BOOK_CUSTOM_ID);
    }

    private void tagGuideTarget(ItemStack item, String value) {
        nbtItemHandler.setCustomItemId(item, GUIDE_TARGET_CUSTOM_ID);
        nbtItemHandler.setMachineId(item, value);
    }

    private String readGuideTag(ItemStack item) {
        if (!nbtItemHandler.hasCustomItemId(item, GUIDE_TARGET_CUSTOM_ID)) {
            return null;
        }
        return nbtItemHandler.getMachineId(item).orElse(null);
    }

    private int calculateTotalPages(int totalItems) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
    }

    private int clampPage(int page, int totalPages) {
        if (page < 1) {
            return 1;
        }
        return Math.min(page, Math.max(1, totalPages));
    }

    private <T> List<T> paginate(List<T> list, int page) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        int from = Math.max(0, (page - 1) * PAGE_SIZE);
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(list.size(), from + PAGE_SIZE);
        return list.subList(from, to);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeQuery(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static final class GuideModulesHolder implements InventoryHolder {
        private final int page;

        private GuideModulesHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class GuideModuleEntriesHolder implements InventoryHolder {
        private final String moduleId;
        private final int page;

        private GuideModuleEntriesHolder(String moduleId, int page) {
            this.moduleId = moduleId;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class GuideSearchHolder implements InventoryHolder {
        private final String query;
        private final int page;

        private GuideSearchHolder(String query, int page) {
            this.query = query;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class GuideEntryDetailHolder implements InventoryHolder {
        private final GuideBackTarget backTarget;

        private GuideEntryDetailHolder(GuideBackTarget backTarget) {
            this.backTarget = backTarget;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private enum GuideBackOrigin {
        MODULE,
        SEARCH
    }

    private static final class GuideBackTarget {
        private final GuideBackOrigin origin;
        private final String moduleId;
        private final String query;
        private final int page;

        private GuideBackTarget(GuideBackOrigin origin, String moduleId, String query, int page) {
            this.origin = origin;
            this.moduleId = moduleId;
            this.query = query;
            this.page = page;
        }
    }
}
