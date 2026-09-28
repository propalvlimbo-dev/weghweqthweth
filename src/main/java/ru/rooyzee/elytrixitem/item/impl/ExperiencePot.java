package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ColorUtil;
import ru.rooyzee.elytrixitem.util.ExperienceUtil;
import ru.rooyzee.elytrixitem.util.ItemStackUtil;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ExperiencePot implements CustomItem {

    public static final String ID = "experience_pot";
    private static final String ITEM_KEY = "custom_item_id";
    private static final String STORED_KEY = "experience_pot_stored_xp";
    private static final String INSTANCE_KEY = "custom_item_instance";
    private static final int MAX_LEVEL = 100;
    private static final int MAX_STORED_EXPERIENCE = ExperienceUtil.getTotalExperienceForLevel(MAX_LEVEL);

    private final Main plugin;

    public ExperiencePot(Main plugin) {
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
        ItemStack item = new ItemStack(Material.FLOWER_POT, 1);
        applyMeta(plugin, item, 0, true);
        return item;
    }

    public static boolean isItem(Main plugin, ItemStack item) {
        if (item == null || item.getType() != Material.FLOWER_POT || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ITEM_KEY), PersistentDataType.STRING);
        return ID.equals(id);
    }

    public static int getStoredExperience(Main plugin, ItemStack item) {
        if (!isItem(plugin, item)) {
            return 0;
        }

        Integer value = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, STORED_KEY), PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, Math.min(value, MAX_STORED_EXPERIENCE));
    }

    public static void setStoredExperience(Main plugin, ItemStack item, int storedExperience) {
        int value = Math.max(0, Math.min(storedExperience, MAX_STORED_EXPERIENCE));
        applyMeta(plugin, item, value, false);
    }

    public static int getMaxStoredExperience() {
        return MAX_STORED_EXPERIENCE;
    }

    private static void applyMeta(Main plugin, ItemStack item, int storedExperience, boolean createInstance) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.setDisplayName(ColorUtil.color("&7« &#8B7D6BГоршочек с опытом &7»"));
        meta.setLore(buildLore(storedExperience));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, ITEM_KEY), PersistentDataType.STRING, ID);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, STORED_KEY), PersistentDataType.INTEGER, storedExperience);

        NamespacedKey instanceKey = new NamespacedKey(plugin, INSTANCE_KEY);
        String instance = meta.getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);

        if (createInstance || instance == null || instance.isEmpty()) {
            meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        item.setItemMeta(meta);
        ItemStackUtil.hideAllFlags(item);
    }

    private static List<String> buildLore(int storedExperience) {
        int level = ExperienceUtil.getLevelFromTotalExperience(storedExperience);

        return Arrays.asList(
                ColorUtil.color("&#F8BEFB&l┃ "),
                ColorUtil.color("&#F8BEFB&l┃ &fТип: &#8B7D6BАртефакт"),
                ColorUtil.color("&#F8BEFB&l┃ &fОпыт: &#F8BEFB" + level + "∫100"),
                ColorUtil.color("&#F8BEFB&l┃ &fХранение: &#F8BEFBДо 100 уровней"),
                ColorUtil.color("&#F8BEFB&l┃ "),
                ColorUtil.color("&7● &fДержите во второй руке для сбора"),
                ColorUtil.color("&7● &fSHIFT + ПКМ в основной руке для выдачи")
        );
    }
}