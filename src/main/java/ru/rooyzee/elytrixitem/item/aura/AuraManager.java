package ru.rooyzee.elytrixitem.item.aura;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.item.CustomItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AuraManager {

    private static final int LOCKED_COOLDOWN_TICKS = 200;

    private final Main plugin;
    private final Map<UUID, ActiveAura> active = new HashMap<>();
    private final Map<UUID, Set<String>> knownAuras = new HashMap<>();

    public AuraManager(Main plugin) {
        this.plugin = plugin;
        startTask();
    }

    public boolean isAuraActive(Player player, String auraId) {
        ActiveAura activeAura = active.get(player.getUniqueId());
        return activeAura != null && activeAura.id.equals(auraId);
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    update(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void update(Player player) {
        UUID uniqueId = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();

        Map<String, Integer> foundOrder = new HashMap<>();
        Set<String> currentIds = new LinkedHashSet<>();
        Map<String, ItemStack> firstStackById = new HashMap<>();

        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];

            if (!AuraItem.isAura(plugin, item)) {
                continue;
            }

            String id = AuraItem.getAuraId(plugin, item);
            if (id == null) {
                continue;
            }

            if (!foundOrder.containsKey(id)) {
                foundOrder.put(id, slot);
                firstStackById.put(id, item);
                currentIds.add(id);
            }
        }

        Set<String> known = knownAuras.computeIfAbsent(uniqueId, k -> new LinkedHashSet<>());
        Set<String> newlyAppeared = new LinkedHashSet<>();

        for (String id : currentIds) {
            if (!known.contains(id)) {
                newlyAppeared.add(id);
            }
        }

        known.retainAll(currentIds);

        ActiveAura current = active.get(uniqueId);

        if (current != null && !currentIds.contains(current.id)) {
            active.remove(uniqueId);
            current = null;
        }

        if (current == null) {
            String chosen = pickNext(currentIds, newlyAppeared);
            if (chosen != null) {
                active.put(uniqueId, new ActiveAura(chosen));
                known.add(chosen);
            }
        }

        for (String id : newlyAppeared) {
            if (!known.contains(id)) {
                known.add(id);
            }
        }

        applyCooldowns(player, currentIds, firstStackById);
        updateLore(player, contents);
    }

    private String pickNext(Set<String> currentIds, Set<String> newlyAppeared) {
        if (!newlyAppeared.isEmpty()) {
            return newlyAppeared.iterator().next();
        }

        if (!currentIds.isEmpty()) {
            return currentIds.iterator().next();
        }

        return null;
    }

    private void applyCooldowns(Player player, Set<String> currentIds, Map<String, ItemStack> firstStackById) {
        ActiveAura activeAura = active.get(player.getUniqueId());
        String activeId = activeAura != null ? activeAura.id : null;

        Set<Material> lockedMaterials = new HashSet<>();
        Set<Material> activeMaterials = new HashSet<>();

        for (String id : currentIds) {
            ItemStack stack = firstStackById.get(id);
            if (stack == null) {
                continue;
            }

            Material material = stack.getType();

            if (id.equals(activeId)) {
                activeMaterials.add(material);
            } else {
                lockedMaterials.add(material);
            }
        }

        for (Material material : activeMaterials) {
            if (player.hasCooldown(material)) {
                player.setCooldown(material, 0);
            }
        }

        for (Material material : lockedMaterials) {
            player.setCooldown(material, LOCKED_COOLDOWN_TICKS);
        }
    }

    private void updateLore(Player player, ItemStack[] contents) {
        ActiveAura activeAura = active.get(player.getUniqueId());
        String activeId = activeAura != null ? activeAura.id : null;

        for (ItemStack item : contents) {
            if (!AuraItem.isAura(plugin, item)) {
                continue;
            }

            String id = AuraItem.getAuraId(plugin, item);
            if (id == null) {
                continue;
            }

            CustomItem registered = plugin.getItemRegistry().get(id);
            if (!(registered instanceof AuraItem)) {
                continue;
            }

            AuraItem aura = (AuraItem) registered;
            aura.updateState(item, id.equals(activeId));
        }
    }

    public void clear(UUID uniqueId) {
        active.remove(uniqueId);
        knownAuras.remove(uniqueId);
    }

    private static final class ActiveAura {

        private final String id;

        private ActiveAura(String id) {
            this.id = id;
        }
    }
}