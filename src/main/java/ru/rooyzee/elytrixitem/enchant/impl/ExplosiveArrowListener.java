package ru.rooyzee.elytrixitem.enchant.impl;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;

import java.util.concurrent.ThreadLocalRandom;

public final class ExplosiveArrowListener implements Listener {

    private static final double EXPLOSION_RADIUS = 2.0D;
    private static final double EXPLOSION_DAMAGE = 4.0D;
    private static final double TRIGGER_CHANCE = 0.40D;

    private final Main plugin;
    private final NamespacedKey markerKey;

    public ExplosiveArrowListener(Main plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "explosive_arrow_marker");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        ItemStack bow = event.getBow();
        if (bow == null) {
            return;
        }

        ExplosiveArrowEnchant enchant = getEnchant();
        if (enchant == null || !enchant.has(bow)) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > TRIGGER_CHANCE) {
            return;
        }

        Entity projectile = event.getProjectile();
        if (!(projectile instanceof AbstractArrow)) {
            return;
        }

        projectile.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) {
            return;
        }

        Projectile projectile = (Projectile) event.getDamager();

        Byte marker = projectile.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity hit = (LivingEntity) event.getEntity();
        Location center = hit.getLocation();

        Player shooter = null;
        if (projectile.getShooter() instanceof Player) {
            shooter = (Player) projectile.getShooter();
        }

        center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center, 1);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (!(nearby instanceof LivingEntity)) {
                continue;
            }

            if (nearby.equals(shooter)) {
                continue;
            }

            LivingEntity target = (LivingEntity) nearby;
            target.damage(EXPLOSION_DAMAGE, shooter);
        }

        projectile.remove();
    }

    private ExplosiveArrowEnchant getEnchant() {
        return (ExplosiveArrowEnchant) plugin.getEnchantRegistry().get(ExplosiveArrowEnchant.ID);
    }
}