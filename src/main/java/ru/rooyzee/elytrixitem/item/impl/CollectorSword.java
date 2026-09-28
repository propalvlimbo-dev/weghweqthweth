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

public final class CollectorSword implements CustomItem {

    public static final String ID = "collector_sword";
    private final NamespacedKey key;

    public CollectorSword(Main plugin) {
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &#A8FF78Меч Сбора &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fТип: &#A8FF78Артефакт"),
                    ColorUtil.color("&#F8BEFB&l┃ &fСвойство: &#F8BEFBУдваивает выпадение опыта"),
                    ColorUtil.color("&#F8BEFB&l┃ &fЦель: &#F8BEFBТолько мобы"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fРаботает при добивании моба")
            );
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ID);
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);
        return item;
    }
}