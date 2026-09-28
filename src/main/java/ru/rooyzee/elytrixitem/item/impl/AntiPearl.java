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
import java.util.List;

public final class AntiPearl implements CustomItem {

    public static final String ID = "anti_pearl";
    public static final int RADIUS = 15;
    public static final int DURATION_TICKS = 30 * 20;
    public static final int USE_COOLDOWN_TICKS = 60 * 20;

    private final NamespacedKey key;

    public AntiPearl(Main plugin) {
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &#8C5196Анти-Пёрл &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fТип: &#8C5196Артефакт"),
                    ColorUtil.color("&#F8BEFB&l┃ &fРадиус: &#F8BEFB" + RADIUS + " блоков"),
                    ColorUtil.color("&#F8BEFB&l┃ &fДлительность: &#F8BEFB30 секунд"),
                    ColorUtil.color("&#F8BEFB&l┃ &fПерезарядка: &#F8BEFB60 секунд"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fБлокирует эндер-жемчуг у чужих игроков"),
                    ColorUtil.color("&7● &fНажмите ПКМ для активации")
            );

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ID);
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);
        return item;
    }
}