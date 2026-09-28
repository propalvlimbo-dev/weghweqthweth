package ru.rooyzee.elytrixitem.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;
import ru.rooyzee.elytrixitem.util.ItemNameUtil;

import java.util.Map;

public final class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        int slot = event.getRawSlot();

        if (slot == 45) {
            plugin.getMenuManager().open(player, holder.getPage() - 1);
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            plugin.getMenuManager().open(player, holder.getPage() + 1);
            return;
        }

        String itemId = holder.getItemId(slot);
        if (itemId == null) {
            return;
        }

        CustomItem customItem = plugin.getItemRegistry().get(itemId);
        if (customItem == null) {
            plugin.getMessages().send(player, "item-not-found");
            return;
        }

        ItemStack itemStack = customItem.createItem();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);

        if (!leftovers.isEmpty()) {
            plugin.getMessages().send(player, "inventory-full");
            return;
        }

        plugin.getMessages().send(player, "item-taken",
                "{item}", ItemNameUtil.getDisplayName(itemStack, customItem.getId()));
        player.updateInventory();

        // Повторная синхронизация следующим тиком: в креативе клиент может
        // "перекрыть" слоты своим состоянием, из-за чего выданный предмет выглядит пропавшим.
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}