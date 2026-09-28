package ru.rooyzee.elytrixitem.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.util.ItemNameUtil;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Запрет использования всех предметов плагина в мире спавна.
 *
 * Предметы не тратятся: при попытке нажать ПКМ использование отменяется,
 * на предмет вешается бесконечная перезарядка (наглядный запрет),
 * а игроку отправляется сообщение, что на спавне нельзя пользоваться предметами.
 * При выходе из мира спавна перезарядка снимается.
 */
public final class SpawnItemListener implements Listener {

    /** "Бесконечная" перезарядка в тиках (~1.5 года). */
    private static final int INFINITE_COOLDOWN_TICKS = 1_000_000_000;

    /** Порог, по которому отличаем нашу бесконечную перезарядку от обычной. */
    private static final int INFINITE_COOLDOWN_THRESHOLD = 100_000_000;

    /** Антиспам сообщений. */
    private static final long MESSAGE_THROTTLE_MILLIS = 2000L;

    private final Main plugin;
    private final NamespacedKey key;
    private final Map<UUID, Set<Material>> blockedCooldowns = new HashMap<>();
    private final Map<UUID, Long> lastNotify = new HashMap<>();

    public SpawnItemListener(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "custom_item_id");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isSpawnWorld(player.getWorld())) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isCustomItem(item) || isExplosiveTrap(item)) {
            return;
        }

        event.setCancelled(true);

        Material material = item.getType();
        if (!player.hasCooldown(material)) {
            player.setCooldown(material, INFINITE_COOLDOWN_TICKS);
        }
        trackBlockedMaterial(player, material);

        long now = System.currentTimeMillis();
        Long last = lastNotify.get(player.getUniqueId());
        if (last == null || now - last >= MESSAGE_THROTTLE_MILLIS) {
            lastNotify.put(player.getUniqueId(), now);

            String itemName = ItemNameUtil.getDisplayName(item, item.getType().name());
            plugin.getMessages().send(player, "spawn-blocked-item", "{item}", itemName);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (isSpawnWorld(player.getWorld())) {
            applyInfiniteCooldowns(player);
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (isSpawnWorld(player.getWorld())) {
            applyInfiniteCooldowns(player);
            return;
        }

        if (isSpawnWorld(event.getFrom())) {
            clearInfiniteCooldowns(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uniqueId = event.getPlayer().getUniqueId();
        blockedCooldowns.remove(uniqueId);
        lastNotify.remove(uniqueId);
    }

    /** Вешает бесконечную перезарядку на все предметы плагина в инвентаре. */
    private void applyInfiniteCooldowns(Player player) {
        for (ItemStack content : player.getInventory().getContents()) {
            if (isCustomItem(content) && !isExplosiveTrap(content) && !player.hasCooldown(content.getType())) {
                player.setCooldown(content.getType(), INFINITE_COOLDOWN_TICKS);
                trackBlockedMaterial(player, content.getType());
            }
        }
    }

    /** Снимает бесконечную перезарядку после выхода из мира спавна. */
    private void clearInfiniteCooldowns(Player player) {
        Set<Material> materials = blockedCooldowns.remove(player.getUniqueId());
        if (materials == null) {
            return;
        }

        for (Material material : materials) {
            if (player.getCooldown(material) > INFINITE_COOLDOWN_THRESHOLD) {
                player.setCooldown(material, 0);
            }
        }
    }

    private void trackBlockedMaterial(Player player, Material material) {
        blockedCooldowns.computeIfAbsent(player.getUniqueId(), uniqueId -> EnumSet.noneOf(Material.class)).add(material);
    }

    private boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    private boolean isExplosiveTrap(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return "explosive_trap".equals(item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING));
    }

    private boolean isSpawnWorld(World world) {
        Object configured = plugin.getSettings().get("spawn-world", "spawn");

        if (configured instanceof Iterable) {
            for (Object entry : (Iterable<?>) configured) {
                if (entry != null && world.getName().equalsIgnoreCase(String.valueOf(entry))) {
                    return true;
                }
            }

            return false;
        }

        return world.getName().equalsIgnoreCase(String.valueOf(configured));
    }
}
