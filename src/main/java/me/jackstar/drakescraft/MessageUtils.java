package me.jackstar.drakescraft.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Parses a string using MiniMessage format.
     * 
     * @param text The text to parse.
     * @return The parsed Component.
     */
    public static Component parseMini(String text) {
        if (text == null)
            return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }

    /**
     * Parses a string using Legacy format (&a).
     * 
     * @param text The text to parse.
     * @return The parsed Component.
     */
    public static Component parseLegacy(String text) {
        if (text == null)
            return Component.empty();
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Hybrid parser: Tries to detect if the string is legacy or modern.
     * Prioritizes MiniMessage unless '&' is explicitly found.
     * 
     * @param text The text to parse.
     * @return The parsed Component.
     */
    public static Component parse(String text) {
        if (text == null)
            return Component.empty();

        // Simple heuristic: if it has legacy codes, treat as legacy.
        // Otherwise assume modern MiniMessage.
        // This stops <red>Hello &aWorld from breaking if we only looked for one.
        // Ideally configuration should dictate which parser to use, but for now:
        if (text.contains("&")) {
            return parseLegacy(text);
        }
        return parseMini(text);
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(parse(text));
    }

    public static String color(String text) {
        if (text == null)
            return "";
        return text.replace("&", "§");
    }
}
