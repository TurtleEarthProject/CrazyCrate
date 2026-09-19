package com.badbones69.crazycrates.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Utility for accepting MiniMessage and common legacy color formats.
 */
public final class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final Pattern EXPANDED_HEX = Pattern.compile("(?i)(?:&x(?:&[0-9a-f]){6}|§x(?:§[0-9a-f]){6})");
    private static final Pattern HEX = Pattern.compile("(?i)[&§]#([0-9a-f]{6})");
    private static final Pattern LEGACY = Pattern.compile("(?i)[&§]([0-9a-fk-or])");

    private ColorUtils() {
    }

    /**
     * Converts legacy color codes to MiniMessage while preserving existing
     * MiniMessage tags.
     */
    public static @NotNull String color(@NotNull String text) {
        text = EXPANDED_HEX.matcher(text).replaceAll(match -> {
            String value = match.group();
            StringBuilder hex = new StringBuilder(6);
            for (int index = 3; index < value.length(); index += 2) {
                hex.append(value.charAt(index));
            }
            return "<reset><#" + hex + ">";
        });
        text = HEX.matcher(text).replaceAll(match -> "<reset><#" + match.group(1) + ">");
        return LEGACY.matcher(text).replaceAll(match -> switch (Character.toLowerCase(match.group(1).charAt(0))) {
            case '0' -> "<reset><black>";
            case '1' -> "<reset><dark_blue>";
            case '2' -> "<reset><dark_green>";
            case '3' -> "<reset><dark_aqua>";
            case '4' -> "<reset><dark_red>";
            case '5' -> "<reset><dark_purple>";
            case '6' -> "<reset><gold>";
            case '7' -> "<reset><gray>";
            case '8' -> "<reset><dark_gray>";
            case '9' -> "<reset><blue>";
            case 'a' -> "<reset><green>";
            case 'b' -> "<reset><aqua>";
            case 'c' -> "<reset><red>";
            case 'd' -> "<reset><light_purple>";
            case 'e' -> "<reset><yellow>";
            case 'f' -> "<reset><white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> match.group();
        });
    }

    /**
     * Translates '&amp;' color codes for each string in a list.
     */
    public static @NotNull List<String> color(@NotNull List<String> texts) {
        return texts.stream().map(ColorUtils::color).collect(Collectors.toList());
    }

    /**
     * Parses MiniMessage or legacy color coded text into an Adventure component.
     */
    public static @NotNull Component toComponent(@NotNull String text) {
        return MINI_MESSAGE.deserialize(color(text));
    }

    public static @NotNull String plainText(@NotNull String text) {
        return PLAIN_TEXT.serialize(toComponent(text));
    }
}
