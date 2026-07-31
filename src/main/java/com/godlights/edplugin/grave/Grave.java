package com.godlights.edplugin.grave;

import org.bukkit.Location;

import java.util.UUID;

public record Grave(Location location, UUID owner, String originalBlockData, long expiresAt) {

    public static String key(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
