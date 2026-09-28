package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixitem.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Тягун: трезубец, притягивающий бросившего его игрока к той сущности,
 * в которую он попал (мобы, игроки — любые живые существа).
 */
public final class TyagunListener implements Listener {

    /** Сколько миллисекунд запись о летящем трезубце считается актуальной. */
    private static final long FLIGHT_TIMEOUT = 30_000L;

    private final Main plugin;
    private final NamespacedKey key;
    private final Map<UUID, Long> flying = new HashMap<>();

    public TyagunListener(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident)) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity().getShooter();
        purge();

        // В момент броска трезубец ещё в руке — проверяем обе руки
        if (isTyagun(player.getInventory().getItemInMainHand())
                || isTyagun(player.getInventory().getItemInOffHand())) {
            flying.put(event.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident)) {
            return;
        }

        Trident trident = (Trident) event.getEntity();

        Long launchedAt = flying.remove(trident.getUniqueId());
        if (launchedAt == null) {
            return;
        }

        if (!(trident.getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) trident.getShooter();
        if (!player.isOnline()) {
            return;
        }

        if (!(event.getHitEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) event.getHitEntity();
        if (target.getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        pull(player, target);
    }

    private void pull(Player player, LivingEntity target) {
        Vector direction = target.getLocation().toVector()
                .add(new Vector(0.0D, 0.4D, 0.0D))
                .subtract(player.getLocation().toVector());

        double distance = direction.length();
        if (distance < 0.5D) {
            return;
        }

        direction.normalize();

        // Чем дальше цель — тем сильнее рывок (с разумным потолком)
        double power = Math.min(0.9D + distance * 0.12D, 2.0D);
        Vector velocity = direction.multiply(power);

        // Чуть приподнимаем, чтобы игрок не врезался в землю при полёте к цели
        velocity.setY(Math.max(velocity.getY(), 0.25D));

        player.setVelocity(velocity);
        player.setFallDistance(0.0F);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0F, 1.4F);
    }

    private boolean isTyagun(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT || !item.hasItemMeta()) {
            return false;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return Tyagun.ID.equals(id);
    }

    private void purge() {
        long now = System.currentTimeMillis();
        flying.values().removeIf(launchedAt -> now - launchedAt > FLIGHT_TIMEOUT);
    }
}
