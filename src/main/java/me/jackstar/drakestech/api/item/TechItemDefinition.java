package me.jackstar.drakestech.api.item;

import me.jackstar.drakescraft.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TechItemDefinition {

    private final String id;
    private final String displayName;
    private final Material baseMaterial;
    private final List<String> lore;
    private final int customModelData;
    private final boolean glowing;

    public TechItemDefinition(String id,
            String displayName,
            Material baseMaterial,
            List<String> lore,
            int customModelData,
            boolean glowing) {
        this.id = normalize(id);
        this.displayName = displayName == null ? id : displayName;
        this.baseMaterial = baseMaterial == null ? Material.PAPER : baseMaterial;
        this.lore = lore == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lore));
        this.customModelData = customModelData;
        this.glowing = glowing;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public ItemStack createItem(int amount) {
        int safeAmount = Math.max(1, amount);
        ItemBuilder builder = new ItemBuilder(baseMaterial, safeAmount)
                .name(displayName)
                .lore(lore);
        if (customModelData > 0) {
            builder.modelData(customModelData);
        }
        if (glowing) {
            builder.glowing();
        }
        return builder.build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
