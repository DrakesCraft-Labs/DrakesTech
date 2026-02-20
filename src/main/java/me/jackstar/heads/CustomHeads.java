package me.jackstar.drakestech.heads;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.UUID;

public enum CustomHeads {

    RUBY_ORE(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTI5NGU0OGIyYzU5YmMxNjE4OTJkODkzMjI4MjM5ZTE3ZDM4NjkyY2Q2ZWI1MDk5YzgxZjExYTY4NjY4ZGIifX19"),
    SAPPHIRE_ORE(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM1YjU0YmIyMzllZGY2ZGE2ZjBlYTM2YjE5ZWY1NTY2Y2QyM2E4OWM1ZGI0ZDE2OTA2YzQ2ZTRiNDkwIn19");

    private final String texture;
    private ItemStack item;

    CustomHeads(String texture) {
        this.texture = texture;
    }

    public ItemStack getItem() {
        if (item == null) {
            item = createSkull(texture);
        }
        return item.clone();
    }

    private ItemStack createSkull(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.getProperties().add(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}
