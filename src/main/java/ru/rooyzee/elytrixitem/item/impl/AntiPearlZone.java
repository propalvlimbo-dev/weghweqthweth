package ru.rooyzee.elytrixitem.item.impl;

import org.bukkit.Location;

import java.util.UUID;

public final class AntiPearlZone {

    private final UUID owner;
    private final Location center;
    private final long expiresAt;

    public AntiPearlZone(UUID owner, Location center, long expiresAt) {
        this.owner = owner;
        this.center = center;
        this.expiresAt = expiresAt;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getCenter() {
        return center;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}