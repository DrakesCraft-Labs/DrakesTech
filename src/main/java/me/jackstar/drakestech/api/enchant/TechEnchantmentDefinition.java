package me.jackstar.drakestech.api.enchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TechEnchantmentDefinition {

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final List<String> description;

    public TechEnchantmentDefinition(String id, String displayName, int maxLevel, List<String> description) {
        this.id = normalize(id);
        this.displayName = displayName == null ? id : displayName;
        this.maxLevel = Math.max(1, maxLevel);
        this.description = description == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(description));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
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
