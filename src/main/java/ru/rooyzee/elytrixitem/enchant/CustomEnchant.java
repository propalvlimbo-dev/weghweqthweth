package ru.rooyzee.elytrixitem.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class CustomEnchant {

    protected final Main plugin;
    protected final NamespacedKey key;

    protected CustomEnchant(Main plugin, String id) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "enchant_" + id);
    }

    public abstract String getId();

    public abstract String getDisplayName();

    public abstract boolean canEnchant(ItemStack item);

    public NamespacedKey getKey() {
        return key;
    }

    public boolean has(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Byte value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public void apply(ItemStack item) {
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String line = ColorUtil.color(getDisplayName());
        lore.removeIf(existing -> existing != null && existing.equals(line));
        lore.add(0, line);

        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);

        item.setItemMeta(meta);
    }
}