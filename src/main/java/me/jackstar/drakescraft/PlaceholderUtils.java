package me.jackstar.drakescraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderUtils {

    private PlaceholderUtils() {
    }

    public static String applyPlaceholders(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception ignored) {
            return text;
        }
    }
}
