package ru.rooyzee.elytrixitem.item.impl;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.hook.WorldGuardHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates a temporary trap from {@code schem/trapa.schem}.
 *
 * <p>The old implementation positioned the schematic relative to the player's
 * Y coordinate and then performed a second, unrelated clearance check.  On a
 * perfectly flat surface this could put the construction into the ground and
 * reject the placement.  A placement is now planned before any block is
 * changed: its lowest schematic block is placed into the top terrain layer
 * across the whole schematic footprint. The schematic origin remains exactly
 * one block above that layer, i.e. at the player feet level on flat ground.</p>
 */
public final class ExplosiveTrapListener implements Listener {
    private static final String SCHEMATIC_PATH = "schem/trapa.schem";
    private static final long RESTORE_DELAY_MILLIS = 500L;
    private static final double PIT_RADIUS = 3.15D;

    /** Width of each corner area whose empty cells may receive terrain blocks. */
    private static final int CORNER_FILL_SIZE = 2;

    private final org.bukkit.NamespacedKey key;
    private final Main plugin;
    private final List<ExplosiveTrapZone> zones = new CopyOnWriteArrayList<>();

    public ExplosiveTrapListener(Main plugin) {
        this.plugin = plugin;
        this.key = new org.bukkit.NamespacedKey(plugin, "custom_item_id");

        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isTrapItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.hasCooldown(Material.FIRE_CHARGE)) {
            plugin.getMessages().send(player, "trap-cooldown");
            return;
        }

        SchematicPlacement placement = pasteSchematic(player);
        if (placement == null) {
            return;
        }

        consumeTrapItem(player);
        player.setCooldown(Material.FIRE_CHARGE, ExplosiveTrap.COOLDOWN_TICKS);
        zones.add(new ExplosiveTrapZone(player.getUniqueId(), placement.center,
                System.currentTimeMillis() + ExplosiveTrap.DURATION_TICKS * 50L,
                placement.originalBlocks, placement.minX, placement.maxX,
                placement.minY, placement.maxY, placement.minZ, placement.maxZ));

        World world = placement.center.getWorld();
        if (world != null) {
            world.playSound(placement.center, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.8F);
            world.spawnParticle(Particle.EXPLOSION_HUGE, placement.center.clone().add(0.0D, 1.0D, 0.0D), 1);
            world.spawnParticle(Particle.SMOKE_LARGE, placement.center.clone().add(0.0D, 1.0D, 0.0D),
                    18, 2.0D, 0.4D, 2.0D, 0.04D);
        }
    }

    private boolean isTrapItem(ItemStack item) {
        if (item == null || item.getType() != Material.FIRE_CHARGE || !item.hasItemMeta()) {
            return false;
        }
        return ExplosiveTrap.ID.equals(item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING));
    }

    private void consumeTrapItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isTrapItem(item)) {
            return;
        }

        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }
        player.updateInventory();
    }

    /**
     * Builds the complete modification plan first and writes it only after all
     * safety/protection checks have passed.  Therefore an unsuccessful use of
     * the item never leaves half of a schematic in the world.
     */
    private SchematicPlacement pasteSchematic(Player player) {
        Clipboard clipboard;
        try {
            clipboard = loadSchematic();
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().severe("Не удалось загрузить " + SCHEMATIC_PATH + ": " + exception.getMessage());
            plugin.getMessages().send(player, "trap-schematic-missing");
            return null;
        }

        if (clipboard == null) {
            plugin.getMessages().send(player, "trap-schematic-missing");
            return null;
        }

        World world = player.getWorld();
        if (!hasSolidSupport(player)) {
            plugin.getMessages().send(player, "trap-unsafe-location");
            return null;
        }

        SchematicData schematic = readSchematic(clipboard);
        if (schematic == null) {
            plugin.getLogger().warning("Схематика ловушки не содержит блоков: " + SCHEMATIC_PATH);
            plugin.getMessages().send(player, "trap-spawn-blocked");
            return null;
        }

        // X/Z origin схематики остаётся под игроком. Высота не зависит от
        // прыжка, положения глаз или от старой искусственной поправки -3.
        int originX = player.getLocation().getBlockX();
        int originZ = player.getLocation().getBlockZ();
        int highestSurfaceY = findHighestSurfaceY(world,
                originX + schematic.minX, originX + schematic.maxX,
                originZ + schematic.minZ, originZ + schematic.maxZ);
        if (highestSurfaceY < getMinBuildHeight()) {
            plugin.getMessages().send(player, "trap-unsafe-location");
            return null;
        }

        // Origin схематики находится ровно на один блок выше поверхности.
        // Нижний занятый слой при этом заменяет верхний слой земли, поэтому
        // ловушка не висит над ландшафтом, а аккуратно уходит в него.
        int originY = highestSurfaceY - schematic.lowestOccupiedY;
        Map<Block, BlockData> plan = new LinkedHashMap<>();
        for (LocalSchematicBlock local : schematic.blocks) {
            if (local.data.getMaterial().isAir()) {
                continue;
            }
            plan.put(world.getBlockAt(originX + local.x, originY + local.y, originZ + local.z), local.data);
        }

        if (plan.isEmpty()) {
            plugin.getMessages().send(player, "trap-spawn-blocked");
            return null;
        }

        PlacementBounds schematicBounds = PlacementBounds.from(plan);
        if (!isWithinWorldHeight(world, schematicBounds)) {
            plugin.getMessages().send(player, "trap-unsafe-location");
            return null;
        }

        Location trapCenter = new Location(world, originX + 0.5D,
                schematicBounds.minY + 1.0D, originZ + 0.5D);

        // Only empty cells in four small corner areas are filled. The material
        // comes from the natural block below the same column. The centre is
        // deliberately never considered, so the pit stays empty.
        fillCornerVoids(world, plan, schematicBounds, trapCenter);
        PlacementBounds finalBounds = PlacementBounds.from(plan);
        if (!isWithinWorldHeight(world, finalBounds)) {
            plugin.getMessages().send(player, "trap-unsafe-location");
            return null;
        }

        if (!canChangeAll(player, plan)) {
            plugin.getMessages().send(player, "trap-build-blocked");
            return null;
        }

        // Игрок остаётся на месте: после замены верхнего слоя земли он
        // естественно оказывается в свободной центральной части ловушки.
        List<BlockState> originals = new ArrayList<>(plan.size());
        for (Block block : plan.keySet()) {
            originals.add(block.getState());
        }

        try {
            for (Map.Entry<Block, BlockData> entry : plan.entrySet()) {
                entry.getKey().setBlockData(entry.getValue(), false);
            }
        } catch (RuntimeException exception) {
            for (BlockState original : originals) {
                original.update(true, false);
            }
            plugin.getLogger().warning("Не удалось установить ловушку: " + exception.getMessage());
            plugin.getMessages().send(player, "trap-spawn-blocked");
            return null;
        }

        return new SchematicPlacement(originals, finalBounds, trapCenter);
    }

    private boolean hasSolidSupport(Player player) {
        Block support = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        return support.getType().isSolid() && !support.isLiquid();
    }

    private SchematicData readSchematic(Clipboard clipboard) {
        try {
            BlockVector3 origin = clipboard.getOrigin();
            List<LocalSchematicBlock> blocks = new ArrayList<>();
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int lowestOccupiedY = Integer.MAX_VALUE;

            for (BlockVector3 point : clipboard.getRegion()) {
                int x = point.getX() - origin.getX();
                int y = point.getY() - origin.getY();
                int z = point.getZ() - origin.getZ();
                BlockData data = BukkitAdapter.adapt(clipboard.getBlock(point));
                blocks.add(new LocalSchematicBlock(x, y, z, data));

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
                if (!data.getMaterial().isAir()) {
                    lowestOccupiedY = Math.min(lowestOccupiedY, y);
                }
            }

            if (blocks.isEmpty() || lowestOccupiedY == Integer.MAX_VALUE) {
                return null;
            }
            return new SchematicData(blocks, minX, maxX, minZ, maxZ, lowestOccupiedY);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Не удалось прочитать блоки схематики ловушки: " + exception.getMessage());
            return null;
        }
    }

    /**
     * The plugin targets the 1.16 API, where worlds start at Y=0. Keeping this
     * in one method avoids accidentally calling World#getMinHeight, which was
     * added only in later Bukkit APIs.
     */
    private static int getMinBuildHeight() {
        return 0;
    }

    /** Finds the topmost existing block across the whole schematic footprint. */
    private int findHighestSurfaceY(World world, int minX, int maxX, int minZ, int maxZ) {
        int highest = getMinBuildHeight() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                highest = Math.max(highest, world.getHighestBlockYAt(x, z));
            }
        }
        return highest;
    }

    /**
     * Fills only four 2x2 corner columns. In a lower corner the same natural
     * material is continued upward until the schematic, which prevents the
     * construction from looking like it hangs in the air. No central position
     * can enter this method, so the pit cannot be filled by terrain.
     */
    private void fillCornerVoids(World world, Map<Block, BlockData> plan,
                                 PlacementBounds schematicBounds, Location trapCenter) {
        int cornerWidthX = Math.min(CORNER_FILL_SIZE,
                Math.max(1, (schematicBounds.maxX - schematicBounds.minX + 1) / 2));
        int cornerWidthZ = Math.min(CORNER_FILL_SIZE,
                Math.max(1, (schematicBounds.maxZ - schematicBounds.minZ + 1) / 2));

        for (int x = schematicBounds.minX; x <= schematicBounds.maxX; x++) {
            boolean cornerX = x < schematicBounds.minX + cornerWidthX
                    || x > schematicBounds.maxX - cornerWidthX;
            if (!cornerX) {
                continue;
            }
            for (int z = schematicBounds.minZ; z <= schematicBounds.maxZ; z++) {
                boolean cornerZ = z < schematicBounds.minZ + cornerWidthZ
                        || z > schematicBounds.maxZ - cornerWidthZ;
                if (!cornerZ) {
                    continue;
                }

                BlockData terrainData = findTerrainDataBelow(world, x, z, schematicBounds.minY - 1);
                if (terrainData == null) {
                    continue;
                }
                int terrainY = findSolidYBelow(world, x, z, schematicBounds.minY - 1);
                if (terrainY < getMinBuildHeight()) {
                    continue;
                }

                for (int y = terrainY + 1; y <= schematicBounds.maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (plan.containsKey(block) || !block.isPassable() || isPitColumn(block, trapCenter)) {
                        continue;
                    }
                    plan.put(block, terrainData.clone());
                }
            }
        }
    }

    private int findSolidYBelow(World world, int x, int z, int fromY) {
        for (int y = Math.min(fromY, world.getMaxHeight() - 1); y >= getMinBuildHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid() && !block.isLiquid()) {
                return y;
            }
        }
        return getMinBuildHeight() - 1;
    }

    private BlockData findTerrainDataBelow(World world, int x, int z, int fromY) {
        int y = findSolidYBelow(world, x, z, fromY);
        return y < getMinBuildHeight() ? null : world.getBlockAt(x, y, z).getBlockData().clone();
    }

    private boolean isPitColumn(Block block, Location center) {
        if (block.getWorld() != center.getWorld()) {
            return false;
        }
        double dx = block.getX() + 0.5D - center.getX();
        double dz = block.getZ() + 0.5D - center.getZ();
        return dx * dx + dz * dz <= PIT_RADIUS * PIT_RADIUS;
    }

    private boolean isWithinWorldHeight(World world, PlacementBounds bounds) {
        return bounds.minY >= getMinBuildHeight() && bounds.maxY < world.getMaxHeight();
    }

    private boolean canChangeAll(Player player, Map<Block, BlockData> plan) {
        boolean worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        for (Block block : plan.keySet()) {
            // Нижний слой схематики ставится в верхний слой ландшафта, поэтому
            // обычные твёрдые блоки здесь разрешено заменить. Контейнеры и
            // прочие TileState по-прежнему никогда не затрагиваются.
            if (isProtectedBlock(block) || isTrapBlock(block.getLocation())) {
                return false;
            }
            if (worldGuardEnabled && !WorldGuardHook.canBuild(player, block.getLocation())) {
                return false;
            }
        }
        return true;
    }

    private boolean isProtectedBlock(Block block) {
        return block.getState() instanceof TileState;
    }

    private Clipboard loadSchematic() throws IOException {
        File pluginSchematic = new File(plugin.getDataFolder(), SCHEMATIC_PATH);
        if (pluginSchematic.isFile()) {
            return readSchematicFile(pluginSchematic);
        }

        try (InputStream input = plugin.getResource(SCHEMATIC_PATH)) {
            if (input != null) {
                ClipboardFormat format = ClipboardFormats.findByAlias("sponge");
                if (format == null) {
                    format = ClipboardFormats.findByAlias("schem");
                }
                if (format == null) {
                    throw new IOException("WorldEdit не зарегистрировал формат Sponge Schematic");
                }
                try (ClipboardReader reader = format.getReader(input)) {
                    return reader.read();
                }
            }
        }

        return null;
    }

    private Clipboard readSchematicFile(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new IOException("Формат .schem не распознан");
        }
        try (InputStream input = new FileInputStream(file);
             ClipboardReader reader = format.getReader(input)) {
            return reader.read();
        }
    }

    private static final class LocalSchematicBlock {
        private final int x;
        private final int y;
        private final int z;
        private final BlockData data;

        private LocalSchematicBlock(int x, int y, int z, BlockData data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
        }
    }

    private static final class SchematicData {
        private final List<LocalSchematicBlock> blocks;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final int lowestOccupiedY;

        private SchematicData(List<LocalSchematicBlock> blocks, int minX, int maxX,
                              int minZ, int maxZ, int lowestOccupiedY) {
            this.blocks = blocks;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.lowestOccupiedY = lowestOccupiedY;
        }
    }

    private static final class PlacementBounds {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;

        private PlacementBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        private static PlacementBounds from(Map<Block, BlockData> plan) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Block block : plan.keySet()) {
                minX = Math.min(minX, block.getX());
                maxX = Math.max(maxX, block.getX());
                minY = Math.min(minY, block.getY());
                maxY = Math.max(maxY, block.getY());
                minZ = Math.min(minZ, block.getZ());
                maxZ = Math.max(maxZ, block.getZ());
            }
            return new PlacementBounds(minX, maxX, minY, maxY, minZ, maxZ);
        }
    }

    private static final class SchematicPlacement {
        private final List<BlockState> originalBlocks;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final Location center;

        private SchematicPlacement(List<BlockState> originalBlocks, PlacementBounds bounds, Location center) {
            this.originalBlocks = originalBlocks;
            this.minX = bounds.minX;
            this.maxX = bounds.maxX;
            this.minY = bounds.minY;
            this.maxY = bounds.maxY;
            this.minZ = bounds.minZ;
            this.maxZ = bounds.maxZ;
            this.center = center;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        ExplosiveTrapZone zone = findZone(event.getFrom());
        if (zone == null) {
            zone = findZone(event.getTo());
        }
        if (zone != null && !zone.isEjecting() && !isInsideBoundary(zone, event.getTo())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isTrapBlock(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isTrapBlock(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluid(BlockFromToEvent event) {
        if (isTrapBlock(event.getToBlock().getLocation()) || isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        if (isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> isTrapBlock(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> isTrapBlock(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallingBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK
                && isTrapBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT && inTrap(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        ExplosiveTrapZone destinationZone = event.getTo() == null ? null : findZone(event.getTo());
        if (destinationZone != null && !destinationZone.isEjecting()) {
            event.setCancelled(true);
            return;
        }
        ExplosiveTrapZone sourceZone = findZone(event.getFrom());
        if (sourceZone != null && !sourceZone.isEjecting()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent event) {
        if (event.getEntityType() == EntityType.ENDER_PEARL
                && event.getEntity().getShooter() instanceof Player
                && inTrap((Player) event.getEntity().getShooter())) {
            event.setCancelled(true);
        }
    }

    private boolean isTrapBlock(Location location) {
        for (ExplosiveTrapZone zone : zones) {
            if (location.getWorld() == zone.getCenter().getWorld()
                    && location.getBlockX() >= zone.getMinX() - 1
                    && location.getBlockX() <= zone.getMaxX() + 1
                    && location.getBlockY() >= zone.getMinY() - 1
                    && location.getBlockY() <= zone.getMaxY() + 1
                    && location.getBlockZ() >= zone.getMinZ() - 1
                    && location.getBlockZ() <= zone.getMaxZ() + 1) {
                return true;
            }
        }
        return false;
    }

    private boolean inTrap(Player player) {
        for (ExplosiveTrapZone zone : zones) {
            if (isInPit(zone, player)) {
                return true;
            }
        }
        return false;
    }

    private ExplosiveTrapZone findZone(Location location) {
        for (ExplosiveTrapZone zone : zones) {
            if (isInsideBoundary(zone, location)) {
                return zone;
            }
        }
        return null;
    }

    private boolean isInsideBoundary(ExplosiveTrapZone zone, Location location) {
        Location center = zone.getCenter();
        return location.getWorld() == center.getWorld()
                && location.getX() >= zone.getMinX() - 0.75D
                && location.getX() <= zone.getMaxX() + 1.75D
                && location.getZ() >= zone.getMinZ() - 0.75D
                && location.getZ() <= zone.getMaxZ() + 1.75D
                && location.getY() >= zone.getMinY() - 1.0D
                && location.getY() <= zone.getMaxY() + 2.0D;
    }

    private boolean isInPit(ExplosiveTrapZone zone, Player player) {
        Location location = player.getLocation();
        Location center = zone.getCenter();
        return location.getWorld() == center.getWorld()
                && horizontalDistanceSquared(center, location) <= PIT_RADIUS * PIT_RADIUS
                && location.getY() >= center.getY() - 3.5D
                && location.getY() <= center.getY() + 4.0D;
    }

    private double horizontalDistanceSquared(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (ExplosiveTrapZone zone : zones) {
            World world = zone.getCenter().getWorld();
            if (world == null) {
                zones.remove(zone);
                continue;
            }
            if (!zone.isEjecting() && zone.isExpired(now)) {
                zone.startEjection(now);
                applySingleEjection(zone, world);
                world.playSound(zone.getCenter(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 0.7F);
                world.spawnParticle(Particle.EXPLOSION_LARGE,
                        zone.getCenter().clone().add(0.0D, 1.0D, 0.0D), 2);
                continue;
            }
            if (zone.isEjecting()) {
                if (now - zone.getEjectionStartedAt() < RESTORE_DELAY_MILLIS) {
                    continue;
                }
                for (Player player : world.getPlayers()) {
                    if (isInsideSolidBlock(player)) {
                        Location safe = findSafeLocation(world, player.getLocation(), zone);
                        if (safe != null) {
                            player.teleport(safe);
                        }
                    }
                }
                for (BlockState state : zone.getOriginalBlocks()) {
                    state.update(true, false);
                }
                // Restoring terrain may have returned a block into a player.
                for (Player player : world.getPlayers()) {
                    if (isInsideSolidBlock(player)) {
                        Location safe = findSafeLocation(world, player.getLocation(), zone);
                        if (safe != null) {
                            player.teleport(safe);
                        }
                    }
                }
                world.playSound(zone.getCenter(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
                zones.remove(zone);
                continue;
            }

            world.spawnParticle(Particle.SPELL_WITCH, zone.getCenter().clone().add(0.0D, 0.2D, 0.0D),
                    4, 2.5D, 0.1D, 2.5D, 0.0D);
            for (Player player : world.getPlayers()) {
                if (isInPit(zone, player)) {
                    player.damage(0.35D);
                    player.setCooldown(Material.ENDER_PEARL, 10);
                }
            }
        }
    }

    private boolean isInsideSolidBlock(Player player) {
        Block feet = player.getLocation().getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        return !feet.isPassable() || !head.isPassable();
    }

    private Location findSafeLocation(World world, Location from, ExplosiveTrapZone zone) {
        int centerX = from.getBlockX();
        int centerZ = from.getBlockZ();
        int minY = Math.max(getMinBuildHeight() + 1, Math.max(from.getBlockY(), zone.getMaxY() + 1));
        int maxY = Math.min(world.getMaxHeight() - 2, zone.getMaxY() + 16);

        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        Location candidate = new Location(world, centerX + dx + 0.5D, y,
                                centerZ + dz + 0.5D);
                        if (candidate.getBlock().isPassable()
                                && candidate.clone().add(0, 1, 0).getBlock().isPassable()
                                && candidate.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void applySingleEjection(ExplosiveTrapZone zone, World world) {
        if (zone.isEjectionApplied()) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (!isInPit(zone, player)) {
                continue;
            }
            player.setFallDistance(0.0F);
            player.setVelocity(new Vector(0.0D, 1.8D, 0.0D));
            world.spawnParticle(Particle.CLOUD, player.getLocation(), 12,
                    0.35D, 0.35D, 0.35D, 0.12D);
        }
        zone.markEjectionApplied();
    }
}
