package ru.rooyzee.elytrixitem.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemNameUtil {

    private ItemNameUtil() {
    }

    public static String getDisplayName(ItemStack itemStack, String fallbackId) {
        if (itemStack != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }

        return prettifyId(fallbackId);
    }

    public static String prettifyId(String id) {
        if (id == null || id.isEmpty()) {
            return "Unknown";
        }

        String[] parts = id.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }
}