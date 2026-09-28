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
import java.util.UUID;

public final class InfiniteFirework implements CustomItem {

    public static final String ID = "infinite_firework";

    private final NamespacedKey key;
    private final NamespacedKey instanceKey;

    public InfiniteFirework(Main plugin) {
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
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&7« &#D8A6FFБесконечный фейерверк &7»"));

            List<String> lore = Arrays.asList(
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&#F8BEFB&l┃ &fТип: &#D8A6FFАртефакт"),
                    ColorUtil.color("&#F8BEFB&l┃ &fСвойство: &#F8BEFBНе тратится при использовании"),
                    ColorUtil.color("&#F8BEFB&l┃ &fПерезарядка: &#F8BEFB5 секунд"),
                    ColorUtil.color("&#F8BEFB&l┃ "),
                    ColorUtil.color("&7● &fИспользуется только для полета на элитрах")
            );

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, ID);
            meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);
        return item;
    }

    /**
     * Возвращает экземпляр предмета, если указанный стак — наш бесконечный фейерверк,
     * иначе null. Удобно для защитных листенеров (CustomItemGuardListener и т.п.).
     */
    public static InfiniteFirework getInstance(Main plugin, ItemStack item) {
        if (plugin == null || item == null || item.getType() != Material.FIREWORK_ROCKET || !item.hasItemMeta()) {
            return null;
        }

        String id = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "custom_item_id"), PersistentDataType.STRING);

        if (!ID.equals(id)) {
            return null;
        }

        CustomItem customItem = plugin.getItemRegistry().get(ID);
        return customItem instanceof InfiniteFirework ? (InfiniteFirework) customItem : null;
    }

    /**
     * Навешивает на ракету полную идентификацию бесконечного фейерверка:
     * имя, лор и PDC-теги (custom_item_id + custom_item_instance).
     * Если ракета уже является нашим предметом — ничего не делает.
     */
    public static void applyIdentity(Main plugin, ItemStack item) {
        if (plugin == null || item == null || item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");
        String existing = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (ID.equals(existing)) {
            return;
        }

        meta.setDisplayName(ColorUtil.color("&7« &#D8A6FFБесконечный фейерверк &7»"));

        meta.setLore(Arrays.asList(
                ColorUtil.color("&#F8BEFB&l┃ "),
                ColorUtil.color("&#F8BEFB&l┃ &fТип: &#D8A6FFАртефакт"),
                ColorUtil.color("&#F8BEFB&l┃ &fСвойство: &#F8BEFBНе тратится при использовании"),
                ColorUtil.color("&#F8BEFB&l┃ &fПерезарядка: &#F8BEFB5 секунд"),
                ColorUtil.color("&#F8BEFB&l┃ "),
                ColorUtil.color("&7● &fИспользуется только для полета на элитрах")
        ));

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, ID);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_item_instance"), PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);

        ItemStackUtil.hideAllFlags(item);
    }
}