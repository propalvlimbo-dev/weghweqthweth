package ru.rooyzee.elytrixitem.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public final class MenuManager {

    private static final int SIZE = 54;
    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private static final int[] CONTENT_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            46, 47, 48, 50, 51, 52
    };

    private final Main plugin;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        player.openInventory(build(page));
    }

    private Inventory build(int page) {
        List<CustomItem> items = new ArrayList<>(plugin.getItemRegistry().getAll());
        int pages = Math.max(1, (int) Math.ceil(items.size() / (double) CONTENT_SLOTS.length));
        int currentPage = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder(currentPage);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, getTitle(currentPage, pages));
        holder.setInventory(inventory);

        int start = currentPage * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, items.size());

        if (items.isEmpty()) {
            inventory.setItem(22, buildEmptyItem());
        } else {
            int index = 0;
            for (int i = start; i < end; i++) {
                int slot = CONTENT_SLOTS[index++];
                CustomItem customItem = items.get(i);
                inventory.setItem(slot, customItem.getMenuItem());
                holder.setItemSlot(slot, customItem.getId());
            }
        }

        if (currentPage > 0) {
            inventory.setItem(PREVIOUS_SLOT, buildButton(Material.BLACK_DYE, "previous"));
        }

        inventory.setItem(CLOSE_SLOT, buildButton(Material.RED_DYE, "close"));

        if (currentPage + 1 < pages) {
            inventory.setItem(NEXT_SLOT, buildButton(Material.BLACK_DYE, "next"));
        }

        return inventory;
    }

    private String getTitle(int page, int pages) {
        String title = plugin.getMenuConfig().getString("title", "&#F8BEFBПредметы");
        title = title.replace("%page%", String.valueOf(page + 1));
        title = title.replace("%pages%", String.valueOf(pages));
        return ColorUtil.color(title);
    }

    private ItemStack buildEmptyItem() {
        FileConfiguration config = plugin.getMenuConfig();
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(config.getString("empty.display_name", "&#F8BEFBСписок предметов")));
            meta.setLore(colorList(config.getStringList("empty.lore")));
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private ItemStack buildButton(Material material, String key) {
        FileConfiguration config = plugin.getMenuConfig();
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(config.getString("buttons." + key + ".display_name", " ")));
            meta.setLore(colorList(config.getStringList("buttons." + key + ".lore")));
            meta.addItemFlags(ItemFlag.values());
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private List<String> colorList(List<String> source) {
        List<String> result = new ArrayList<>();
        for (String line : source) {
            result.add(ColorUtil.color(line));
        }
        return result;
    }
}