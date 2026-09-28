package ru.rooyzee.elytrixitem.item.aura;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.aura.impl.CrystalProtectionAura;
import ru.rooyzee.elytrixitem.item.aura.impl.FallProtectionAura;

public final class AuraListener implements Listener {

    private final Main plugin;
    private final AuraManager auraManager;

    public AuraListener(Main plugin, AuraManager auraManager) {
        this.plugin = plugin;
        this.auraManager = auraManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        auraManager.update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        auraManager.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (auraManager.isAuraActive(player, FallProtectionAura.ID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!(event.getDamager() instanceof EnderCrystal)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (auraManager.isAuraActive(player, CrystalProtectionAura.ID)) {
            event.setDamage(event.getDamage() * 0.9D);
        }
    }
}