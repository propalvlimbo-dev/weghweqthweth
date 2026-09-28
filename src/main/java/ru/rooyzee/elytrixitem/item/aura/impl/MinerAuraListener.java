package ru.rooyzee.elytrixitem.item.aura.impl;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.aura.AuraManager;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MinerAuraListener implements Listener {

    private static final int MIN_REWARD = 100;
    private static final int MAX_REWARD = 1000;
    private static final double CHANCE = 0.20D;

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.REDSTONE_ORE
    );

    private final Main plugin;
    private final AuraManager auraManager;

    public MinerAuraListener(Main plugin, AuraManager auraManager) {
        this.plugin = plugin;
        this.auraManager = auraManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!ORES.contains(block.getType())) {
            return;
        }

        block.getChunk().getPersistentDataContainer().set(
                buildBlockKey(block),
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            cleanup(event.getBlock());
            return;
        }

        Block block = event.getBlock();
        if (!ORES.contains(block.getType())) {
            return;
        }

        NamespacedKey key = buildBlockKey(block);
        Byte placed = block.getChunk().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        block.getChunk().getPersistentDataContainer().remove(key);

        if (placed != null && placed == (byte) 1) {
            return;
        }

        if (!auraManager.isAuraActive(player, MinerAura.ID)) {
            return;
        }

        if (!plugin.getEconomyHook().isEnabled()) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() > CHANCE) {
            return;
        }

        int reward = ThreadLocalRandom.current().nextInt(MIN_REWARD, MAX_REWARD + 1);
        boolean success = plugin.getEconomyHook().deposit(player, reward);

        if (success) {
            plugin.getMessages().send(player, "miner-aura-reward",
                    "{amount}", String.valueOf(reward));
        }
    }

    private void cleanup(Block block) {
        block.getChunk().getPersistentDataContainer().remove(buildBlockKey(block));
    }

    private NamespacedKey buildBlockKey(Block block) {
        return new NamespacedKey(plugin, "placed_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }
}