package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.util.ElytraBoostUtil;

public final class InfiniteFireworkListener implements Listener {

    private final Main plugin;
    private final NamespacedKey key;

    public InfiniteFireworkListener(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET || !item.hasItemMeta()) {
            return;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (!InfiniteFirework.ID.equals(id)) {
            return;
        }

        Player player = event.getPlayer();

        // Всегда отменяем событие: ваниль не должна трогать инвентарь ни в одном режиме игры.
        event.setCancelled(true);

        if (player.hasCooldown(Material.FIREWORK_ROCKET)) {
            return;
        }

        if (!player.isGliding()) {
            player.setCooldown(Material.FIREWORK_ROCKET, 100);
            plugin.getMessages().send(player, "firework-not-flying");
            return;
        }

        player.setCooldown(Material.FIREWORK_ROCKET, 100);

        // Использование полностью отменено выше, поэтому серверный инвентарь уже не
        // должен изменяться. Нельзя восстанавливать слот клоном: игрок может успеть
        // перенести ракету в другой слот, после чего восстановление создавало дюп.
        ElytraBoostUtil.boost(plugin, player);

        // Обновляем клиент после обработки пакетов Creative/UseItem, но никогда не
        // записываем предмет обратно в слот и не создаём его копию.
        for (long delay : new long[]{1L, 2L, 4L, 7L, 12L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.updateInventory();
                }
            }, delay);
        }
    }
}
