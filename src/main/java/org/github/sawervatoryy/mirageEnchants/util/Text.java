package org.github.sawervatoryy.mirageEnchants.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** MirageStudio | sawervatoryy */
public final class Text {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private Text() {}

    public static Component c(String s) {
        return LEGACY.deserialize(s == null ? "" : s)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> c(List<String> list) {
        List<Component> out = new ArrayList<>();
        for (String s : list) out.add(c(s));
        return out;
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    public static String strip(String s) {
        return plain(LEGACY.deserialize(s == null ? "" : s));
    }

    public static String roman(int n) {
        if (n <= 0 || n >= 4000) return String.valueOf(n);
        int[] v = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] r = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            while (n >= v[i]) { sb.append(r[i]); n -= v[i]; }
        }
        return sb.toString();
    }
}
