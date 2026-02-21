package me.jackstar.drakestech.api.guide;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TechGuideModule {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;

    public TechGuideModule(String id, String displayName, Material icon, List<String> description) {
        this.id = normalize(id);
        this.displayName = displayName == null ? id : displayName;
        this.icon = icon == null ? Material.BOOK : icon;
        this.description = description == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(description));
    }

    public String getId() {
        return id;
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
