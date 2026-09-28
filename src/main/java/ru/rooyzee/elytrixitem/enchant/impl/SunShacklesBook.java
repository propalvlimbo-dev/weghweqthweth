package ru.rooyzee.elytrixitem.enchant.impl;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ColorUtil;
import ru.rooyzee.elytrixitem.util.ItemStackUtil;

import java.util.Arrays;
import java.util.List;

public final class SunShacklesBook implements CustomItem {

    public static final String ID = "enchant_sun_shackles";

    private final Main plugin;

    public SunShacklesBook(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isStackable() {
        return false;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &6Солнечные оковы &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&6Солнечные оковы I"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fОписание: &#F8BEFBЗолотой вид + статы незерита"),
                    ColorUtil.color("&#F8BEFB&l┃ &fПредмет: &#F8BEFBАлмазный / Незеритовый шлем"),
                    ColorUtil.color("&#F8BEFB&l┃ &fПрочность: &#F8BEFBНеразрушимый"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fПоложите в наковальню со шлемом")
            );

            meta.setLore(lore);
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);

        SunShacklesEnchant enchant = (SunShacklesEnchant) plugin.getEnchantRegistry().get(SunShacklesEnchant.ID);
        if (enchant != null) {
            enchant.apply(item);
        }

        return item;
    }
}