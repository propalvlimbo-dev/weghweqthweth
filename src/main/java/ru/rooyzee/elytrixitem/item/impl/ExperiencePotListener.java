package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.rooyzee.elytrixitem.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExperiencePotListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> releaseBypassUntil = new HashMap<>();

    public ExperiencePotListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExpChange(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) {
            return;
        }

        Player player = event.getPlayer();

        if (isBypassActive(player.getUniqueId())) {
            return;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!ExperiencePot.isItem(plugin, offHand)) {
            return;
        }

        int stored = ExperiencePot.getStoredExperience(plugin, offHand);
        int max = ExperiencePot.getMaxStoredExperience();

        if (stored >= max) {
            return;
        }

        int absorb = Math.min(event.getAmount(), max - stored);
        if (absorb <= 0) {
            return;
        }

        ExperiencePot.setStoredExperience(plugin, offHand, stored + absorb);
        player.getInventory().setItemInOffHand(offHand);
        event.setAmount(event.getAmount() - absorb);
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

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean isMainPot = ExperiencePot.isItem(plugin, mainHand);
        boolean isOffPot = ExperiencePot.isItem(plugin, offHand);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (isMainPot || isOffPot)) {
            event.setCancelled(true);
        }

        if (!player.isSneaking()) {
            return;
        }

        if (!isMainPot) {
            return;
        }

        event.setCancelled(true);

        if (player.hasCooldown(Material.FLOWER_POT)) {
            return;
        }

        int stored = ExperiencePot.getStoredExperience(plugin, mainHand);
        if (stored <= 0) {
            player.setCooldown(Material.FLOWER_POT, 10);
            return;
        }

        int give = Math.max(1, (int) Math.floor(stored * 0.03D));
        give = Math.min(give, stored);

        ExperiencePot.setStoredExperience(plugin, mainHand, stored - give);
        player.getInventory().setItemInMainHand(mainHand);
        player.setCooldown(Material.FLOWER_POT, 10);

        releaseBypassUntil.put(player.getUniqueId(), System.currentTimeMillis() + 1000L);

        ExperienceOrb orb = player.getWorld().spawn(player.getLocation().add(0.0D, 0.5D, 0.0D), ExperienceOrb.class);
        orb.setExperience(give);

        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (ExperiencePot.isItem(plugin, item)) {
            event.setCancelled(true);
        }
    }

    private boolean isBypassActive(UUID uniqueId) {
        Long until = releaseBypassUntil.get(uniqueId);

        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() > until) {
            releaseBypassUntil.remove(uniqueId);
            return false;
        }

        return true;
    }
}