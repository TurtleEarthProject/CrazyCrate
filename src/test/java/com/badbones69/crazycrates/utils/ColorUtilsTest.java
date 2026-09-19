package com.badbones69.crazycrates.utils;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ColorUtilsTest {

    public static void main(String[] args) {
        assert ColorUtils.color("&cRed").equals("<reset><red>Red");
        assert ColorUtils.color("§aGreen").equals("<reset><green>Green");
        assert ColorUtils.color("&#12abEFHex").equals("<reset><#12abEF>Hex");
        assert ColorUtils.color("§#12abEFHex").equals("<reset><#12abEF>Hex");
        assert ColorUtils.color("&x&1&2&a&b&E&FHex").equals("<reset><#12abEF>Hex");
        assert PlainTextComponentSerializer.plainText().serialize(
                ColorUtils.toComponent("<gradient:red:blue>Tag</gradient> &lLegacy"))
                .equals("Tag Legacy");
    }
}
