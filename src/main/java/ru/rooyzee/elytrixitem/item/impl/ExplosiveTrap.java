package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ColorUtil;
import ru.rooyzee.elytrixitem.util.ItemStackUtil;

import java.util.Arrays;

public final class ExplosiveTrap implements CustomItem {
    public static final String ID = "explosive_trap";
    public static final int DURATION_TICKS = 20 * 20;
    public static final int COOLDOWN_TICKS = 45 * 20;
    private final NamespacedKey key;

    public ExplosiveTrap(Main plugin) {
        key = new NamespacedKey(plugin, "custom_item_id");
    }

    @Override
    public String getId() { return ID; }

    @Override
    public boolean isStackable() { return false; }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &#FF6B6BВзрывная ловушка &7»"));
            meta.setLore(Arrays.asList(
                    ColorUtil.color("&#F8BEFB&l┃ &fТип: &#FF6B6BАртефакт"),
                    ColorUtil.color("&#F8BEFB&l┃ &fДействует: &#FF6B6B20 секунд"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fСоздаёт ловушку под игроком"),
                    ColorUtil.color("&7● &fНе позволяет выбраться из неё"),
                    ColorUtil.color("&7● &fНажмите ПКМ для активации")
            ));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ID);
            item.setItemMeta(meta);
        }
        ItemStackUtil.hideAllFlags(item);
        return item;
    }
}
