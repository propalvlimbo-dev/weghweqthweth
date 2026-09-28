package ru.rooyzee.elytrixitem.item.aura;

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

import java.util.ArrayList;
import java.util.List;

public abstract class AuraItem implements CustomItem {

    public static final String EFFECT_PLACEHOLDER = "{effect}";

    protected final Main plugin;
    protected final NamespacedKey key;

    protected AuraItem(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    public abstract Material getMaterial();

    public abstract String getDisplayName();

    public abstract String getActiveEffect();

    public abstract List<String> getLoreTemplate();

    @Override
    public boolean isStackable() {
        return false;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(getDisplayName()));
            meta.setLore(buildLore(true));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(meta);
        }

        ItemStackUtil.hideAllFlags(item);
        return item;
    }

    public List<String> buildLore(boolean active) {
        String effect = active ? getActiveEffect() : "&cНеактивна";
        List<String> result = new ArrayList<>();

        for (String line : getLoreTemplate()) {
            result.add(ColorUtil.color(line.replace(EFFECT_PLACEHOLDER, effect)));
        }

        return result;
    }

    public void updateState(ItemStack item, boolean active) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setLore(buildLore(active));
        item.setItemMeta(meta);
    }

    public static boolean isAura(Main plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "custom_item_id"),
                PersistentDataType.STRING
        );

        if (id == null) {
            return false;
        }

        return plugin.getItemRegistry().get(id) instanceof AuraItem;
    }

    public static String getAuraId(Main plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "custom_item_id"),
                PersistentDataType.STRING
        );
    }
}