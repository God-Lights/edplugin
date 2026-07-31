package com.godlights.edplugin.grave;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class GraveManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Grave> graves = new LinkedHashMap<>();

    public GraveManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "graves.yml");
        load();
    }

    public Grave create(Location location, UUID owner, String originalBlockData, long expiresAt) {
        Grave grave = new Grave(location.clone(), owner, originalBlockData, expiresAt);
        graves.put(Grave.key(location), grave);
        save();
        return grave;
    }

    public Optional<Grave> get(Location location) {
        return Optional.ofNullable(graves.get(Grave.key(location)));
    }

    public void remove(Location location) {
        if (graves.remove(Grave.key(location)) != null) {
            save();
        }
    }

    public Map<String, Grave> getAll() {
        return graves;
    }

    public void load() {
        graves.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            String path = key + ".";
            String worldName = yaml.getString(path + "world");
            World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            Location location = new Location(world, yaml.getInt(path + "x"), yaml.getInt(path + "y"), yaml.getInt(path + "z"));
            UUID owner;
            try {
                owner = UUID.fromString(yaml.getString(path + "owner", ""));
            } catch (IllegalArgumentException e) {
                continue;
            }
            String originalBlockData = yaml.getString(path + "original-block-data", "minecraft:air");
            long expiresAt = yaml.getLong(path + "expires-at");
            graves.put(key, new Grave(location, owner, originalBlockData, expiresAt));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Grave> entry : graves.entrySet()) {
            String path = entry.getKey() + ".";
            Grave grave = entry.getValue();
            Location loc = grave.location();
            yaml.set(path + "world", loc.getWorld().getName());
            yaml.set(path + "x", loc.getBlockX());
            yaml.set(path + "y", loc.getBlockY());
            yaml.set(path + "z", loc.getBlockZ());
            yaml.set(path + "owner", grave.owner().toString());
            yaml.set(path + "original-block-data", grave.originalBlockData());
            yaml.set(path + "expires-at", grave.expiresAt());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save graves.yml", e);
        }
    }
}
