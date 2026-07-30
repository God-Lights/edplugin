package com.godlights.edplugin.waystone;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class WaystoneManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Waystone> waystones = new LinkedHashMap<>();

    public WaystoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waystones.yml");
        load();
    }

    public void load() {
        waystones.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String name : yaml.getKeys(false)) {
            String path = name + ".";
            String worldName = yaml.getString(path + "world");
            World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Skipping waystone '" + name + "': world '" + worldName + "' not loaded.");
                continue;
            }
            Location loc = new Location(world,
                    yaml.getDouble(path + "x"),
                    yaml.getDouble(path + "y"),
                    yaml.getDouble(path + "z"),
                    (float) yaml.getDouble(path + "yaw"),
                    (float) yaml.getDouble(path + "pitch"));
            String creator = yaml.getString(path + "creator", "unknown");
            waystones.put(name.toLowerCase(), new Waystone(name, loc, creator));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Waystone ws : waystones.values()) {
            String path = ws.name() + ".";
            Location loc = ws.location();
            yaml.set(path + "world", loc.getWorld().getName());
            yaml.set(path + "x", loc.getX());
            yaml.set(path + "y", loc.getY());
            yaml.set(path + "z", loc.getZ());
            yaml.set(path + "yaw", (double) loc.getYaw());
            yaml.set(path + "pitch", (double) loc.getPitch());
            yaml.set(path + "creator", ws.creator());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save waystones.yml", e);
        }
    }

    public boolean exists(String name) {
        return waystones.containsKey(name.toLowerCase());
    }

    public Optional<Waystone> get(String name) {
        return Optional.ofNullable(waystones.get(name.toLowerCase()));
    }

    public Map<String, Waystone> getAll() {
        return waystones;
    }

    public void create(String name, Location location, String creator) {
        waystones.put(name.toLowerCase(), new Waystone(name, location.clone(), creator));
        save();
    }

    public boolean remove(String name) {
        boolean removed = waystones.remove(name.toLowerCase()) != null;
        if (removed) {
            save();
        }
        return removed;
    }
}
