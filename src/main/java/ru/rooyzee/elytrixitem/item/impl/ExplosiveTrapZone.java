package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Location;
import org.bukkit.block.BlockState;

import java.util.List;
import java.util.UUID;

public final class ExplosiveTrapZone {
    private final UUID owner;
    private final Location center;
    private final long expiresAt;
    private final List<BlockState> originalBlocks;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private boolean ejecting;
    private long ejectionStartedAt;
    private boolean ejectionApplied;

    public ExplosiveTrapZone(UUID owner, Location center, long expiresAt, List<BlockState> originalBlocks) {
        this(owner, center, expiresAt, originalBlocks,
                center.getBlockX() - 8, center.getBlockX() + 8,
                center.getBlockY() - 4, center.getBlockY() + 8,
                center.getBlockZ() - 8, center.getBlockZ() + 8);
    }

    public ExplosiveTrapZone(UUID owner, Location center, long expiresAt, List<BlockState> originalBlocks,
                             int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        this.owner = owner;
        this.center = center;
        this.expiresAt = expiresAt;
        this.originalBlocks = originalBlocks;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public UUID getOwner() { return owner; }
    public Location getCenter() { return center; }
    public long getExpiresAt() { return expiresAt; }
    public List<BlockState> getOriginalBlocks() { return originalBlocks; }

    /** Запоминает исходное состояние блока, добавленного уже после установки ловушки. */
    public void rememberOriginal(org.bukkit.block.Block block) {
        for (BlockState state : originalBlocks) {
            if (state.getWorld() == block.getWorld()
                    && state.getX() == block.getX()
                    && state.getY() == block.getY()
                    && state.getZ() == block.getZ()) {
                return;
            }
        }
        originalBlocks.add(block.getState());
    }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public boolean isExpired(long now) { return now >= expiresAt; }

    public void startEjection(long now) {
        ejecting = true;
        ejectionStartedAt = now;
    }

    public boolean isEjecting() { return ejecting; }
    public long getEjectionStartedAt() { return ejectionStartedAt; }
    public boolean isEjectionApplied() { return ejectionApplied; }
    public void markEjectionApplied() { ejectionApplied = true; }
}
