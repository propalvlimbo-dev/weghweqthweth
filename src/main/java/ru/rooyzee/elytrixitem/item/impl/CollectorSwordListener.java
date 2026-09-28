package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;

public final class CollectorSwordListener implements Listener {

    private final Main plugin;
    private final NamespacedKey key;

    public CollectorSwordListener(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Игнорируем убийство игроков, только мобы
        if (event.getEntity() instanceof Player) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        if (mainHand == null || !mainHand.hasItemMeta()) {
            return;
        }

        // Проверяем наличие нашего скрытого тега, чтобы предмет работал даже после наковальни
        String id = mainHand.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (CollectorSword.ID.equals(id)) {
            // Удваиваем выпадающий опыт
            int currentExp = event.getDroppedExp();
            if (currentExp > 0) {
                event.setDroppedExp(currentExp * 2);
            }
        }
    }
}