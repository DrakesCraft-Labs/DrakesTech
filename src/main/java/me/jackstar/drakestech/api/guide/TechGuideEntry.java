package me.jackstar.drakestech.api.guide;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TechGuideEntry {

    private final String id;
    private final String moduleId;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final List<String> recipeLines;
    private final ItemStack previewItem;

    public TechGuideEntry(String id,
            String moduleId,
            String displayName,
            Material icon,
            List<String> description,
            List<String> recipeLines,
            ItemStack previewItem) {
        this.id = normalize(id);
        this.moduleId = normalize(moduleId);
        this.displayName = displayName == null ? id : displayName;
        this.icon = icon == null ? Material.PAPER : icon;
        this.description = description == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(description));
        this.recipeLines = recipeLines == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(recipeLines));
        this.previewItem = previewItem == null ? null : previewItem.clone();
    }

    public String getId() {
        return id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getRecipeLines() {
        return recipeLines;
    }

    public ItemStack getPreviewItem() {
        return previewItem == null ? null : previewItem.clone();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
