package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import ru.rooyzee.elytrixitem.Main;
import ru.rooyzee.elytrixitem.hook.WorldGuardHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ExplosiveTrapListener implements Listener {
    private static final String SCHEMATIC_PATH = "schem/trapa.schem";
    private static final long RESTORE_DELAY_MILLIS = 500L;
    private static final int CLEAR_ABOVE_BLOCKS = 8;
    private static final int MAX_CLEAR_ABOVE_BLOCKS = 24;
    private static final double PIT_RADIUS = 3.15D;
    private final org.bukkit.NamespacedKey key;
    private final Main plugin;
    private final List<ExplosiveTrapZone> zones = new CopyOnWriteArrayList<>();

    public ExplosiveTrapListener(Main plugin) {
        this.plugin = plugin;
        key = new org.bukkit.NamespacedKey(plugin, "custom_item_id");
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

        // Origin схематики должен находиться на блоке-опоре под ногами игрока.
        // Раньше использовался playerY, из-за чего вся конструкция оказывалась на
        // один блок выше и foundation-поиск дополнительно поднимал её на неровном рельефе.
        // Origin схематики: на два блока ниже прежнего уровня опоры.
        Location requestedCenter = player.getLocation().getBlock().getLocation().add(0.5D, -3.0D, 0.5D);
        SchematicPlacement placement = pasteSchematic(requestedCenter, player);
        if (placement == null) {
            notifySpawnBlocked(player);
            return;
        }

        Location center = placement.center;
        consumeTrapItem(player);
        player.setCooldown(Material.FIRE_CHARGE, ExplosiveTrap.COOLDOWN_TICKS);
        zones.add(new ExplosiveTrapZone(player.getUniqueId(), center,
                System.currentTimeMillis() + ExplosiveTrap.DURATION_TICKS * 50L,
                placement.originalBlocks, placement.minX, placement.maxX,
                placement.minY, placement.maxY, placement.minZ, placement.maxZ));

        World world = center.getWorld();
        if (world != null) {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.8F);
            world.spawnParticle(Particle.EXPLOSION_HUGE, center.clone().add(0.0D, 1.0D, 0.0D), 1);
            world.spawnParticle(Particle.SMOKE_LARGE, center.clone().add(0.0D, 1.0D, 0.0D),
                    18, 2.0D, 0.4D, 2.0D, 0.04D);
        }
    }

    private void notifySpawnBlocked(Player player) {
        plugin.getMessages().send(player, "trap-spawn-blocked");
    }

    private boolean hasNearbyLiquid(Player player, Block support) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return true;
        }
        return location.getBlock().isLiquid()
                || location.clone().add(0.0D, 1.0D, 0.0D).getBlock().isLiquid()
                || support.isLiquid();
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

   private SchematicPlacement pasteSchematic(Location requestedCenter, Player player) {
        Clipboard clipboard;
        try { clipboard = loadSchematic(); }
        catch (IOException | RuntimeException exception) {
            plugin.getLogger().severe("Не удалось загрузить schem/trapa.schem: " + exception.getMessage());
            plugin.getMessages().send(player, "trap-schematic-missing");
            return null;
        }
        if (clipboard == null || requestedCenter.getWorld() == null) return null;
        World world = requestedCenter.getWorld();
        BlockVector3 origin = clipboard.getOrigin();
        List<LocalSchematicBlock> locals = new ArrayList<>();
        try {
            for (BlockVector3 point : clipboard.getRegion()) {
                locals.add(new LocalSchematicBlock(point.getX() - origin.getX(), point.getY() - origin.getY(),
                        point.getZ() - origin.getZ(), BukkitAdapter.adapt(clipboard.getBlock(point))));
            }
        } catch (RuntimeException exception) { return null; }
        if (locals.isEmpty()) return null;
        // У новой схемы снизу может присутствовать технический воздушный слой.
        // Убираем его переносом локальных координат, не заполняя воздух блоками.
        int lowestOccupiedY = locals.stream()
                .filter(local -> !local.data.getMaterial().isAir())
                .mapToInt(local -> local.y)
                .min().orElse(0);
        if (lowestOccupiedY != 0) {
            List<LocalSchematicBlock> normalized = new ArrayList<>(locals.size());
            for (LocalSchematicBlock local : locals) {
                normalized.add(new LocalSchematicBlock(local.x, local.y - lowestOccupiedY,
                        local.z, local.data));
            }
            locals = normalized;
        }
        int baseX = requestedCenter.getBlockX(), baseZ = requestedCenter.getBlockZ();
        // Origin схематики привязан к блоку под ногами игрока. Высота никогда
        // не подбирается автоматически и не корректируется по рельефу.
        int originY = requestedCenter.getBlockY();
        for (int attempt = 0; attempt < 1; attempt++) {
            Map<Block, BlockData> plan = new LinkedHashMap<>();
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (LocalSchematicBlock local : locals) {
                if (local.data.getMaterial().isAir()) {
                    continue;
                }
                Block block = world.getBlockAt(baseX + local.x, originY + local.y, baseZ + local.z);
                plan.put(block, local.data);
                minX = Math.min(minX, block.getX()); maxX = Math.max(maxX, block.getX());
                minY = Math.min(minY, block.getY()); maxY = Math.max(maxY, block.getY());
                minZ = Math.min(minZ, block.getZ()); maxZ = Math.max(maxZ, block.getZ());
            }
            // Схема является единственным источником формы ловушки. Не меняем
            // её пол, стены и пустоты постобработкой.
            if (plan.isEmpty()) {
                return null;
            }
            if (minY < world.getMinHeight() || maxY >= world.getMaxHeight()
                    || hasPlannedBlockInPlayerSpace(player, plan)
                    || !hasSafeTrapSpace(player, world, minX, maxX, minY, maxY, minZ, maxZ, plan)) continue;
            boolean blocked = false;
            for (Block block : plan.keySet()) if (isProtectedBlock(block) || isTrapBlock(block.getLocation()) ||
                    (Bukkit.getPluginManager().getPlugin("WorldGuard") != null && !WorldGuardHook.canBuild(player, block.getLocation()))) { blocked = true; break; }
            if (blocked) continue;
            List<BlockState> originals = new ArrayList<>(plan.size());
            for (Block block : plan.keySet()) originals.add(block.getState());
            try { for (Map.Entry<Block, BlockData> entry : plan.entrySet()) entry.getKey().setBlockData(entry.getValue(), false); }
            catch (RuntimeException exception) { for (BlockState state : originals) state.update(true, false); continue; }
            Location actualCenter = new Location(world, baseX + 0.5D, originY, baseZ + 0.5D);
            return new SchematicPlacement(originals, minX, maxX, minY, maxY, minZ, maxZ, actualCenter);
        }
        return null;
    }

   private boolean addFoundation(World world, Map<Block, BlockData> plan, List<LocalSchematicBlock> locals,
                                  int baseX, int baseZ, int originY) {
        Map<Long, Integer> lowestSolidByColumn = new LinkedHashMap<>();
        for (LocalSchematicBlock local : locals) {
            if (!local.data.getMaterial().isSolid()) continue;
            long column = (((long) (baseX + local.x)) << 32) ^ ((baseZ + local.z) & 0xffffffffL);
            lowestSolidByColumn.merge(column, originY + local.y, Math::min);
        }
        for (Map.Entry<Long, Integer> entry : lowestSolidByColumn.entrySet()) {
            long packed = entry.getKey();
            int x = (int) (packed >> 32), z = (int) packed;
            int targetY = entry.getValue();
            int floor = targetY - 1;
            while (floor >= world.getMinHeight() && !world.getBlockAt(x, floor, z).getType().isSolid()) floor--;
            if (floor < world.getMinHeight() || targetY - floor > 12) return false;
            Block floorBlock = world.getBlockAt(x, floor, z);
            Material material = floorBlock.getType();
            if (floorBlock.isLiquid()) return false;
            for (int y = floor + 1; y < targetY; y++) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isAir() && !block.isPassable()) return false;
                plan.putIfAbsent(block, Bukkit.createBlockData(material));
            }
        }
        return true;
    }
    /**
     * Форма ловушки полностью берётся из trapa.schem. Пустые участки схемы
     * намеренно не заменяются блоками из окружающего мира.
     */

    private static final class LocalSchematicBlock {
        private final int x, y, z;
        private final BlockData data;
        private LocalSchematicBlock(int x, int y, int z, BlockData data) { this.x = x; this.y = y; this.z = z; this.data = data; }
    }
    private Clipboard loadSchematic() throws IOException {
        File pluginSchematic = new File(plugin.getDataFolder(), SCHEMATIC_PATH);
        if (pluginSchematic.isFile()) {
            ClipboardFormat format = ClipboardFormats.findByFile(pluginSchematic);
            if (format == null) {
                throw new IOException("Р¤РѕСЂРјР°С‚ .schem РЅРµ СЂР°СЃРїРѕР·РЅР°РЅ");
            }
            try (InputStream input = new FileInputStream(pluginSchematic);
                 ClipboardReader reader = format.getReader(input)) {
                return reader.read();
            }
        }

        try (InputStream input = plugin.getResource(SCHEMATIC_PATH)) {
            if (input != null) {
                ClipboardFormat format = ClipboardFormats.findByAlias("sponge");
                if (format == null) {
                    format = ClipboardFormats.findByAlias("schem");
                }
                if (format == null) {
                    throw new IOException("WorldEdit РЅРµ Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°Р» Sponge Schematic format");
                }
                try (ClipboardReader reader = format.getReader(input)) {
                    return reader.read();
                }
            }
        }

        File projectSchematic = new File("schem/trapa.schem");
        if (projectSchematic.isFile()) {
            ClipboardFormat format = ClipboardFormats.findByFile(projectSchematic);
            if (format == null) {
                throw new IOException("Р¤РѕСЂРјР°С‚ .schem РЅРµ СЂР°СЃРїРѕР·РЅР°РЅ");
            }
            try (InputStream input = new FileInputStream(projectSchematic);
                 ClipboardReader reader = format.getReader(input)) {
                return reader.read();
            }
        }
        return null;
    }

    /** Р—Р°РїРѕР»РЅСЏРµС‚ РЅРµР±РѕР»СЊС€РёРµ РЅРµСЂРѕРІРЅРѕСЃС‚Рё РїРѕРґ РєСЂР°РµРј РїРѕСЃС‚СЂРѕР№РєРё, РјР°С‚РµСЂРёР°Р» Р±РµСЂС‘С‚ СЃ СЃРѕСЃРµРґРЅРµРіРѕ РіСЂСѓРЅС‚Р°. */
    private void addPerimeterSupports(World world, int minX, int maxX, int minY, int maxY,
                                      int minZ, int maxZ, Location center,
                                      Map<Block, BlockData> plannedBlocks) {
        Set<Long> columns = new LinkedHashSet<>();
        for (Map.Entry<Block, BlockData> entry : plannedBlocks.entrySet()) {
            Block block = entry.getKey();
            if (!entry.getValue().getMaterial().isSolid() || !isPerimeterColumn(block.getX(), block.getZ(), minX, maxX, minZ, maxZ)) {
                continue;
            }
            columns.add((((long) block.getX()) << 32) ^ (block.getZ() & 0xffffffffL));
        }
        for (long column : columns) {
            int x = (int) (column >> 32);
            int z = (int) column;
            int bottomSolidY = minY;
            while (bottomSolidY <= maxY && !isPlannedSolid(plannedBlocks, world, x, bottomSolidY, z)) {
                bottomSolidY++;
            }
            if (bottomSolidY > maxY) {
                continue;
            }
            int searchY = bottomSolidY - 1;
            int floorY = searchY;
            while (floorY >= Math.max(world.getMinHeight(), bottomSolidY - 5)
                    && !world.getBlockAt(x, floorY, z).getType().isSolid()) {
                floorY--;
            }
            if (floorY < bottomSolidY - 4 || !world.getBlockAt(x, floorY, z).getType().isSolid()) {
                continue;
            }
            Material fillMaterial = world.getBlockAt(x, floorY, z).getType();
            for (int y = floorY + 1; y < bottomSolidY; y++) {
                Block gap = world.getBlockAt(x, y, z);
                if (!isInsideTrapInterior(gap, center) && gap.isPassable() && !plannedBlocks.containsKey(gap)) {
                    plannedBlocks.put(gap, Bukkit.createBlockData(fillMaterial));
                }
            }
        }
    }

    /** Р—Р°РїРѕР»РЅСЏРµС‚ РІРЅРµС€РЅРёРµ РїСѓСЃС‚РѕС‚С‹ Сѓ СЃС‚РµРЅ СЃС…РµРјР°С‚РёРєРё Р±Р»РѕРєР°РјРё РёР· Р±Р»РёР¶Р°Р№С€РµРіРѕ РѕРєСЂСѓР¶РµРЅРёСЏ. */
    private void addAdjacentVoidFills(World world, int minX, int maxX, int minY, int maxY,
                                      int minZ, int maxZ, Location center,
                                      Map<Block, BlockData> plannedBlocks) {
        // Несколько проходов позволяют заполнителю распространиться от земли
        // через несколько блоков пустоты до стенки схематики.
        // Только один проход: нельзя распространять заполнение через цепочку пустых блоков.
        {
            List<Block> candidates = new ArrayList<>();
            for (int x = minX - 1; x <= maxX + 1; x++) {
                for (int y = Math.max(world.getMinHeight(), minY);
                     y <= Math.min(world.getMaxHeight() - 1, maxY); y++) {
                    for (int z = minZ - 1; z <= maxZ + 1; z++) {
                        if (x != minX - 1 && x != maxX + 1 && z != minZ - 1 && z != maxZ + 1) {
                            continue;
                        }
                        Block candidate = world.getBlockAt(x, y, z);
                        if (!isInsideTrapInterior(candidate, center)
                                && !plannedBlocks.containsKey(candidate) && candidate.isPassable()
                                && hasAdjacentSolidInWorld(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
            if (candidates.isEmpty()) {
                return;
            }
            for (Block candidate : candidates) {
                BlockData fill = findNearbyFillData(candidate, plannedBlocks);
                if (fill != null) {
                    plannedBlocks.put(candidate, fill);
                }
            }
        }
    }

    private void clearTrapInterior(World world, int minX, int maxX, int minY, int maxY,
                                   int minZ, int maxZ, Map<Block, BlockData> plannedBlocks) {
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    // Вся середина открыта: здесь не должно оставаться ни пола,
                    // ни потолка, ни случайных блоков из schematic.
                    plannedBlocks.put(world.getBlockAt(x, y, z), Bukkit.createBlockData(Material.AIR));
                }
            }
        }
    }

    /** Убирает пол и верхние слои закрытой схемы, превращая её в открытую яму. */
    private void clearTrapFloorAndOpening(World world, int minX, int maxX, int minY, int maxY,
                                          int minZ, int maxZ, Map<Block, BlockData> plannedBlocks) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                plannedBlocks.put(world.getBlockAt(x, minY, z), Bukkit.createBlockData(Material.AIR));
                plannedBlocks.put(world.getBlockAt(x, maxY, z), Bukkit.createBlockData(Material.AIR));
                if (maxY - minY >= 2) {
                    plannedBlocks.put(world.getBlockAt(x, maxY - 1, z), Bukkit.createBlockData(Material.AIR));
                }
            }
        }
    }

    private boolean isSchematicFloor(Block block, Set<Block> schematicBlocks,
                                      Map<Block, BlockData> plannedBlocks) {
        BlockData data = plannedBlocks.get(block);
        if (!schematicBlocks.contains(block) || data == null || !data.getMaterial().isSolid()) {
            return false;
        }
        for (int y = block.getY() - 1; y >= block.getWorld().getMinHeight(); y--) {
            Block below = block.getWorld().getBlockAt(block.getX(), y, block.getZ());
            if (schematicBlocks.contains(below)) {
                BlockData belowData = plannedBlocks.get(below);
                if (belowData != null && belowData.getMaterial().isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInsideTrapInterior(Block block, Location center) {
        if (block.getWorld() != center.getWorld()) {
            return false;
        }
        double dx = block.getX() + 0.5D - center.getX();
        double dz = block.getZ() + 0.5D - center.getZ();
        double y = block.getY() + 0.5D;
        return dx * dx + dz * dz <= PIT_RADIUS * PIT_RADIUS
                && y >= center.getY() - 3.5D
                && y <= center.getY() + 4.0D;
    }

    private boolean hasAdjacentSolidInWorld(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                BlockFace.WEST, BlockFace.DOWN}) {
            if (block.getRelative(face).getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private BlockData findNearbyFillData(Block block, Map<Block, BlockData> plannedBlocks) {
        for (BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST, BlockFace.UP}) {
            Block adjacent = block.getRelative(face);
            BlockData planned = plannedBlocks.get(adjacent);
            if (planned != null && planned.getMaterial().isSolid()) {
                return planned.clone();
            }
            if (planned == null && adjacent.getType().isSolid()) {
                return adjacent.getBlockData().clone();
            }
        }
        return null;
    }

    private boolean isPlannedSolid(Map<Block, BlockData> plannedBlocks, World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        BlockData planned = plannedBlocks.get(block);
        return planned != null && planned.getMaterial().isSolid();
    }

    private boolean isPerimeterColumn(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return x == minX || x == maxX || z == minZ || z == maxZ;
    }

    /** Не трогаем блоки с данными/инвентарями: это предотвращает потерю
     * сундуков, спавнеров, табличек и других важных объектов при очистке
     * воздухом из схемы. Обычный природный рельеф (включая траву) разрешён. */
    private boolean isProtectedBlock(Block block) {
        return block.getState() instanceof org.bukkit.block.TileState;
    }

    private boolean hasSafeTrapSpace(Player player, World world, int minX, int maxX, int minY, int maxY,
                                     int minZ, int maxZ, Map<Block, BlockData> plan) {
        Block support = world.getBlockAt(player.getLocation().getBlockX(),
                player.getLocation().getBlockY() - 1, player.getLocation().getBlockZ());
        if (!support.getType().isSolid()) {
            return false;
        }
        // Ловушка не активируется в пещерах: над игроком и над всей площадью
        // до верхней точки схемы должен быть открытый воздух, плюс 3 блока
        // запаса для безопасного выброса.
        int fromY = player.getLocation().getBlockY();
        int toY = Math.min(world.getMaxHeight() - 1, Math.max(maxY + 3, fromY + 3));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = fromY; y <= toY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir() && !plan.containsKey(block)) {
                        return false;
                    }
                }
            }
        }
        for (Block block : plan.keySet()) {
            if (block.getType().isSolid() && !block.isPassable()
                    && block.getY() >= player.getLocation().getBlockY()
                    && block.getY() <= player.getLocation().getBlockY() + 2) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPlannedBlockInPlayerSpace(Player player, Map<Block, BlockData> plannedBlocks) {
        Location location = player.getLocation();
        for (Map.Entry<Block, BlockData> entry : plannedBlocks.entrySet()) {
            if (!entry.getValue().getMaterial().isSolid()) {
                continue;
            }
            Block block = entry.getKey();
            if (block.getY() < location.getBlockY() || block.getY() > location.getBlockY() + 1) {
                continue;
            }
            if (Math.abs(block.getX() + 0.5D - location.getX()) < 0.8D
                    && Math.abs(block.getZ() + 0.5D - location.getZ()) < 0.8D) {
                return true;
            }
        }
        return false;
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

        private SchematicPlacement(List<BlockState> originalBlocks,
                                   int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                   Location center) {
            this.originalBlocks = originalBlocks;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.center = center;
        }
    }

    /** РС‰РµС‚ Р±Р»РёР¶Р°Р№С€РёР№ С‚РІС‘СЂРґС‹Р№ Р±Р»РѕРє СЃРЅРёР·Сѓ, РїРѕСЌС‚РѕРјСѓ РїСЂС‹Р¶РѕРє РЅРµ СЃРѕР·РґР°С‘С‚ РєСЂР°С‚РµСЂ РІ РІРѕР·РґСѓС…Рµ. */
    private Block findSupportBlock(Player player) {
        Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        int lowestY = Math.max(0, block.getY() - 8);
        while (block.getY() >= lowestY) {
            if (block.getType().isSolid()) {
                return block;
            }
            block = block.getRelative(BlockFace.DOWN);
        }
        return null;
    }

    /**
     * РќРµ Р°РєС‚РёРІРёСЂСѓРµРј Р»РѕРІСѓС€РєСѓ РІ РЅРёР·РєРѕР№ РїРµС‰РµСЂРµ: РїРѕСЃР»Рµ РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ РїРѕС‚РѕР»РѕРє РЅРµ РґРѕР»Р¶РµРЅ
     * РІРµСЂРЅСѓС‚СЊ РёРіСЂРѕРєР° РІРЅСѓС‚СЂСЊ Р±Р»РѕРєР°. Р’ РѕС‚РєСЂС‹С‚РѕРј РјРёСЂРµ РІСЃРµ РїСЂРѕРІРµСЂСЏРµРјС‹Рµ Р±Р»РѕРєРё РѕР±С‹С‡РЅРѕ passable.
     */
    private boolean hasVerticalClearance(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int fromY = location.getBlockY();
        int toY = Math.min(world.getMaxHeight() - 1, fromY + 8);
        for (int y = fromY; y <= toY; y++) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return false;
            }
        }
        return true;
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

    private boolean isEjecting(Player player) {
        ExplosiveTrapZone zone = findZone(player.getLocation());
        return zone != null && zone.isEjecting();
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
                // Восстановление могло вернуть блок в точку игрока.
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
            // Схематика может иметь угловые воздушные клетки. Каждый тик
            // проверяем их заново и закрываем блоком из ближайшего окружения.
            repairCornerGaps(zone, world);
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

    /** Постоянно восстанавливает угловые блоки, если их сломали/заменили. */
    private void repairCornerGaps(ExplosiveTrapZone zone, World world) {
        int[][] corners = {{zone.getMinX(), zone.getMinZ()}, {zone.getMinX(), zone.getMaxZ()},
                {zone.getMaxX(), zone.getMinZ()}, {zone.getMaxX(), zone.getMaxZ()}};
        for (int[] corner : corners) {
            for (int y = zone.getMinY(); y <= zone.getMaxY(); y++) {
                Block block = world.getBlockAt(corner[0], y, corner[1]);
                if (!block.isPassable()) {
                    continue;
                }
                BlockData fill = findNearbyFillData(block, new LinkedHashMap<>());
                if (fill == null) {
                    continue;
                }
                zone.rememberOriginal(block);
                block.setBlockData(fill, false);
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
        int minY = Math.max(world.getMinHeight() + 1, Math.max(from.getBlockY(), zone.getMaxY() + 1));
        int maxY = Math.min(world.getMaxHeight() - 2, zone.getMaxY() + 16);

        // Ищем место сначала рядом с игроком, а не только в центре ловушки.
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
