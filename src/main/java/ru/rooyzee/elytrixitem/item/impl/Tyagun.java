package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ColorUtil;
import ru.rooyzee.elytrixitem.util.ItemStackUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class Tyagun implements CustomItem {

    public static final String ID = "tyagun";

    private final NamespacedKey key;
    private final NamespacedKey instanceKey;

    public Tyagun(Main plugin) {
        this.key = new NamespacedKey(plugin, "custom_item_id");
        this.instanceKey = new NamespacedKey(plugin, "custom_item_instance");
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
        ItemStack item = new ItemStack(Material.TRIDENT, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &#8AE6FFТягун &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fТип: &#8AE6FFРеликвия"),
                    ColorUtil.color("&#F8BEFB&l┃ &fСвойство: &#8AE6FFПритягивает вас к цели при попадании"),
                    ColorUtil.color("&#F8BEFB&l┃ &fРаботает: &#F8BEFBНа всех мобов и игроков"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fКиньте трезубец во врага, чтобы подтянуться к нему")
            );

            meta.setLore(lore);

            // Лучший набор зачарований трезубца (Тягун/Риптайд не добавляем намеренно:
            // он конфликтует с Верностью и Громовержцем и ломает механику броска)
            meta.addEnchant(Enchantment.LOYALTY, 3, true);
            meta.addEnchant(Enchantment.IMPALING, 5, true);
            meta.addEnchant(Enchantment.CHANNELING, 1, true);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);

            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ID);
            meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);
        return item;
    }
}
