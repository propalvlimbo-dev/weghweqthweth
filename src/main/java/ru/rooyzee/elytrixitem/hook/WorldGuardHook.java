package ru.rooyzee.elytrixitem.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World;

public final class WorldGuardHook {
    private WorldGuardHook() {
    }

    public static boolean canBuild(Player player, Location location) {
        if (WorldGuardPlugin.inst() == null) {
            return true;
        }
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testState(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
    }

    /**
     * Проверяет, пересекает ли прямоугольник ловушки регион или находится ли
     * ближе указанного количества блоков к его границе.
     */
    public static boolean isNearRegion(World world, int minX, int maxX, int minY, int maxY,
                                       int minZ, int maxZ, int margin) {
        if (WorldGuardPlugin.inst() == null || world == null) {
            return false;
        }

        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return false;
        }

        int expandedMinX = minX - margin;
        int expandedMaxX = maxX + margin;
        int expandedMinY = minY - margin;
        int expandedMaxY = maxY + margin;
        int expandedMinZ = minZ - margin;
        int expandedMaxZ = maxZ + margin;

        for (ProtectedRegion region : manager.getRegions().values()) {
            if ("__global__".equalsIgnoreCase(region.getId())) {
                continue;
            }
            BlockVector3 regionMin = region.getMinimumPoint();
            BlockVector3 regionMax = region.getMaximumPoint();
            if (regionMin.getX() <= expandedMaxX && regionMax.getX() >= expandedMinX
                    && regionMin.getY() <= expandedMaxY && regionMax.getY() >= expandedMinY
                    && regionMin.getZ() <= expandedMaxZ && regionMax.getZ() >= expandedMinZ) {
                return true;
            }
        }
        return false;
    }
}
