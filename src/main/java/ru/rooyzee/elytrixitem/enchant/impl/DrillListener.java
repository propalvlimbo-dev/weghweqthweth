package ru.rooyzee.elytrixitem.enchant.impl;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.util.ColorUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DrillListener implements Listener {

    private static final Set<Material> BLACKLIST = EnumSet.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.NETHER_PORTAL,
            Material.JIGSAW
    );

    private static final Set<Material> PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    );

    private final Main plugin;
    private final NamespacedKey stateKey;
    private final Set<UUID> processing = new HashSet<>();

    public DrillListener(Main plugin) {
        this.plugin = plugin;
        this.stateKey = new NamespacedKey(plugin, "drill_enabled");
        startEffectTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshEffect(event.getPlayer());
    }

    @EventHandler
    public void onSwap(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> refreshEffect(player));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggle(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !PICKAXES.contains(item.getType())) {
            return;
        }

        DrillEnchant enchant = getEnchant();
        if (enchant == null || !enchant.has(item)) {
            return;
        }

        event.setCancelled(true);

        boolean currentState = readEnabledFlag(item);
        boolean enabled = !currentState;
        setEnabled(item, enabled);
        player.getInventory().setItemInMainHand(item);

        String text = enabled
                ? ColorUtil.color("&fБур: &aВключен")
                : ColorUtil.color("&fБур: &cОтключен");

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));

        refreshEffect(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (processing.contains(player.getUniqueId())) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool == null || !PICKAXES.contains(tool.getType())) {
            return;
        }

        DrillEnchant enchant = getEnchant();
        if (enchant == null || !enchant.has(tool)) {
            return;
        }

        if (!isEnabled(tool)) {
            return;
        }

        Block origin = event.getBlock();
        BlockFace face = detectFace(player, origin);

        boolean creative = player.getGameMode() == GameMode.CREATIVE;

        processing.add(player.getUniqueId());
        try {
            for (Block block : collectArea(origin, face)) {
                if (block.equals(origin)) {
                    continue;
                }

                Material type = block.getType();

                if (type.isAir()) {
                    continue;
                }

                if (BLACKLIST.contains(type)) {
                    continue;
                }

                if (type.getHardness() < 0 || block.getState() instanceof org.bukkit.block.Container) {
                    continue;
                }

                BlockBreakEvent secondaryBreak = new BlockBreakEvent(block, player);
                Bukkit.getPluginManager().callEvent(secondaryBreak);
                if (!secondaryBreak.isCancelled()) {
                    breakBlockManually(player, block, tool, creative);
                }

                if (!creative) {
                    damageTool(player, tool);
                }
            }
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    private BlockFace detectFace(Player player, Block origin) {
        Location eye = player.getEyeLocation();
        double dx = origin.getX() + 0.5D - eye.getX();
        double dy = origin.getY() + 0.5D - eye.getY();
        double dz = origin.getZ() + 0.5D - eye.getZ();

        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);

        if (ay >= ax && ay >= az) {
            return dy > 0 ? BlockFace.DOWN : BlockFace.UP;
        }

        if (ax >= az) {
            return dx > 0 ? BlockFace.WEST : BlockFace.EAST;
        }

        return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    private void breakBlockManually(Player player, Block block, ItemStack tool, boolean creative) {
        Collection<ItemStack> drops = creative ? Collections.emptyList() : block.getDrops(tool, player);
        int exp = creative ? 0 : calcExperienceForBlock(block.getType());

        block.setType(Material.AIR);

        if (!creative) {
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.5D, 0.5D), drop);
            }

            if (exp > 0) {
                ExperienceOrb orb = block.getWorld().spawn(block.getLocation().add(0.5D, 0.5D, 0.5D), ExperienceOrb.class);
                orb.setExperience(exp);
            }
        }
    }

    private int calcExperienceForBlock(Material material) {
        switch (material) {
            case COAL_ORE:
                return ThreadLocalRandom.current().nextInt(0, 3);
            case DIAMOND_ORE:
            case EMERALD_ORE:
                return ThreadLocalRandom.current().nextInt(3, 8);
            case LAPIS_ORE:
            case NETHER_QUARTZ_ORE:
                return ThreadLocalRandom.current().nextInt(2, 6);
            case REDSTONE_ORE:
                return ThreadLocalRandom.current().nextInt(1, 6);
            case NETHER_GOLD_ORE:
                return ThreadLocalRandom.current().nextInt(0, 2);
            default:
                return 0;
        }
    }

    private void damageTool(Player player, ItemStack tool) {
        if (tool.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return;
        }

        Damageable damageable = (Damageable) meta;
        damageable.setDamage(damageable.getDamage() + 1);
        tool.setItemMeta(meta);

        if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(tool);
        }
    }

    private Set<Block> collectArea(Block origin, BlockFace face) {
        Set<Block> result = new HashSet<>();

        switch (face) {
            case UP:
            case DOWN:
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        result.add(origin.getRelative(x, 0, z));
                    }
                }
                break;
            case NORTH:
            case SOUTH:
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        result.add(origin.getRelative(x, y, 0));
                    }
                }
                break;
            case EAST:
            case WEST:
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        result.add(origin.getRelative(0, y, z));
                    }
                }
                break;
            default:
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        result.add(origin.getRelative(x, 0, z));
                    }
                }
                break;
        }

        return result;
    }

    private boolean isEnabled(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (!item.getItemMeta().getPersistentDataContainer().has(stateKey, PersistentDataType.BYTE)) {
            setEnabled(item, true);
            return true;
        }

        Byte value = item.getItemMeta().getPersistentDataContainer().get(stateKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private boolean readEnabledFlag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (!item.getItemMeta().getPersistentDataContainer().has(stateKey, PersistentDataType.BYTE)) {
            return true;
        }

        Byte value = item.getItemMeta().getPersistentDataContainer().get(stateKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private void setEnabled(ItemStack item, boolean enabled) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(stateKey, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
        item.setItemMeta(meta);
    }

    private DrillEnchant getEnchant() {
        return (DrillEnchant) plugin.getEnchantRegistry().get(DrillEnchant.ID);
    }

    private void startEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    refreshEffect(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void refreshEffect(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        DrillEnchant enchant = getEnchant();

        boolean shouldSlow = item != null
                && PICKAXES.contains(item.getType())
                && enchant != null
                && enchant.has(item)
                && isEnabled(item);

        if (shouldSlow) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 1, true, false, false));
        } else {
            PotionEffect current = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
            if (current != null && current.getAmplifier() == 1 && current.getDuration() <= 60 && !current.hasParticles()) {
                player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            }
        }
    }
}