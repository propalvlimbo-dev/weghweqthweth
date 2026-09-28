package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.rooyzee.elytrixitem.Main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AntiPearlListener implements Listener {

    private final Main plugin;
    private final NamespacedKey key;
    private final List<AntiPearlZone> zones = new CopyOnWriteArrayList<>();

    public AntiPearlListener(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
        startTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.ENDER_EYE || !item.hasItemMeta()) {
            return;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (!AntiPearl.ID.equals(id)) {
            return;
        }

        event.setCancelled(true);

        if (player.hasCooldown(Material.ENDER_EYE)) {
            return;
        }

        player.setCooldown(Material.ENDER_EYE, AntiPearl.USE_COOLDOWN_TICKS);

        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item);

        zones.add(new AntiPearlZone(
                player.getUniqueId(),
                player.getLocation().clone(),
                System.currentTimeMillis() + AntiPearl.DURATION_TICKS * 50L
        ));

        player.updateInventory();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<AntiPearlZone> iterator = zones.iterator();
                List<AntiPearlZone> toRemove = new ArrayList<>();

                while (iterator.hasNext()) {
                    AntiPearlZone zone = iterator.next();

                    if (zone.isExpired(now)) {
                        toRemove.add(zone);
                        continue;
                    }

                    drawCube(zone.getCenter(), AntiPearl.RADIUS);
                    applyCooldownToPlayers(zone);
                }

                zones.removeAll(toRemove);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void applyCooldownToPlayers(AntiPearlZone zone) {
        Location center = zone.getCenter();
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        double radiusSquared = AntiPearl.RADIUS * AntiPearl.RADIUS;

        for (Player online : world.getPlayers()) {
            UUID uniqueId = online.getUniqueId();

            if (uniqueId.equals(zone.getOwner())) {
                continue;
            }

            if (online.getLocation().distanceSquared(center) > radiusSquared) {
                continue;
            }

            online.setCooldown(Material.ENDER_PEARL, 40);
        }
    }

    private void drawCube(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double minX = center.getX() - radius;
        double maxX = center.getX() + radius;
        double minY = center.getY() - radius;
        double maxY = center.getY() + radius;
        double minZ = center.getZ() - radius;
        double maxZ = center.getZ() + radius;

        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 81, 150), 1.4F);
        double step = 0.75D;

        for (double x = minX; x <= maxX; x += step) {
            spawnDust(world, x, minY, minZ, dust);
            spawnDust(world, x, minY, maxZ, dust);
            spawnDust(world, x, maxY, minZ, dust);
            spawnDust(world, x, maxY, maxZ, dust);
        }

        for (double z = minZ; z <= maxZ; z += step) {
            spawnDust(world, minX, minY, z, dust);
            spawnDust(world, maxX, minY, z, dust);
            spawnDust(world, minX, maxY, z, dust);
            spawnDust(world, maxX, maxY, z, dust);
        }

        for (double y = minY; y <= maxY; y += step) {
            spawnDust(world, minX, y, minZ, dust);
            spawnDust(world, maxX, y, minZ, dust);
            spawnDust(world, minX, y, maxZ, dust);
            spawnDust(world, maxX, y, maxZ, dust);
        }
    }

    private void spawnDust(World world, double x, double y, double z, Particle.DustOptions dust) {
        world.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust, true);
    }
}