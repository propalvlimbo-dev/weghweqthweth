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

public final class ExplosiveArrowBook implements CustomItem {

    public static final String ID = "enchant_explosive_arrow";

    private final Main plugin;

    public ExplosiveArrowBook(Main plugin) {
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
            meta.setDisplayName(ColorUtil.color("&7« &cВзрывная стрела &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&cВзрывная стрела I"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fОписание: &#F8BEFBВзрыв 2 блока при попадании"),
                    ColorUtil.color("&#F8BEFB&l┃ &fПредмет: &#F8BEFBЛук, Арбалет"),
                    ColorUtil.color("&#F8BEFB&l┃ &fШанс: &#F8BEFB40%"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fПоложите в наковальню с луком")
            );

            meta.setLore(lore);
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);

        ExplosiveArrowEnchant enchant = (ExplosiveArrowEnchant) plugin.getEnchantRegistry().get(ExplosiveArrowEnchant.ID);
        if (enchant != null) {
            enchant.apply(item);
        }

        return item;
    }
}