package ru.rooyzee.elytrixitem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
    }

    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String color = net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(color));
        }

        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}