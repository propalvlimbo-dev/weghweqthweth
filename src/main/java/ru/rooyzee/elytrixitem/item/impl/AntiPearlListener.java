package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.rooyzee.elytrixitem.Main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AntiPearlListener implements Listener {

    private static final long UPDATE_INTERVAL_TICKS = 20L;
    private static final double PARTICLE_STEP = 3.0D;
    private static final Particle.DustOptions ZONE_DUST = new Particle.DustOptions(Color.fromRGB(140, 81, 150), 1.4F);

    private final Main plugin;
    private final NamespacedKey key;
    /** Bukkit events and this task run on the main thread, so a plain list is sufficient. */
    private final List<AntiPearlZone> zones = new ArrayList<>();

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

    /**
     * The old implementation searched every player in the world twice a second
     * merely to keep an item cooldown active. Block only the actual pearl use
     * instead; this is both immediate and proportional to pearl attempts.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderPearlUse(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) {
            return;
        }
        Player player = event.getPlayer();
        if (!isPearlBlocked(player)) {
            return;
        }
        event.setCancelled(true);
        player.setCooldown(Material.ENDER_PEARL, 20);
    }

    /** Extra protection for pearls launched by another plugin or mechanism. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderPearlLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() == EntityType.ENDER_PEARL
                && event.getEntity().getShooter() instanceof Player
                && isPearlBlocked((Player) event.getEntity().getShooter())) {
            event.setCancelled(true);
        }
    }

    private boolean isPearlBlocked(Player player) {
        Location location = player.getLocation();
        for (AntiPearlZone zone : zones) {
            if (player.getUniqueId().equals(zone.getOwner())) {
                continue;
            }
            Location center = zone.getCenter();
            if (location.getWorld() == center.getWorld()
                    && location.distanceSquared(center) <= AntiPearl.RADIUS * AntiPearl.RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<AntiPearlZone> iterator = zones.iterator();
                while (iterator.hasNext()) {
                    AntiPearlZone zone = iterator.next();
                    if (zone.isExpired(now)) {
                        iterator.remove();
                        continue;
                    }
                    drawCube(zone.getCenter(), AntiPearl.RADIUS);
                }
            }
        }.runTaskTimer(plugin, 0L, UPDATE_INTERVAL_TICKS);
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

        for (double x = minX; x <= maxX; x += PARTICLE_STEP) {
            spawnDust(world, x, minY, minZ);
            spawnDust(world, x, minY, maxZ);
            spawnDust(world, x, maxY, minZ);
            spawnDust(world, x, maxY, maxZ);
        }

        for (double z = minZ; z <= maxZ; z += PARTICLE_STEP) {
            spawnDust(world, minX, minY, z);
            spawnDust(world, maxX, minY, z);
            spawnDust(world, minX, maxY, z);
            spawnDust(world, maxX, maxY, z);
        }

        for (double y = minY; y <= maxY; y += PARTICLE_STEP) {
            spawnDust(world, minX, y, minZ);
            spawnDust(world, maxX, y, minZ);
            spawnDust(world, minX, y, maxZ);
            spawnDust(world, maxX, y, maxZ);
        }
    }

    private void spawnDust(World world, double x, double y, double z) {
        // Do not force packets to distant clients. Players near the zone still
        // see the border, while busy worlds avoid broadcasting every dot.
        world.spawnParticle(Particle.REDSTONE, x, y, z, 1,
                0.0D, 0.0D, 0.0D, 0.0D, ZONE_DUST, false);
    }
}