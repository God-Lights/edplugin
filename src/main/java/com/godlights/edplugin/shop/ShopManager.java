package com.godlights.edplugin.shop;

import org.bukkit.Location;
import org.bukkit.Material;
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

public final class ShopManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
        load();
    }

    public void load() {
        shops.clear();
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
            Location loc = new Location(world, yaml.getInt(path + "x"), yaml.getInt(path + "y"), yaml.getInt(path + "z"));
            UUID owner = UUID.fromString(yaml.getString(path + "owner"));
            Material material = Material.matchMaterial(yaml.getString(path + "material", "STONE"));
            if (material == null) {
                continue;
            }
            double buyPrice = yaml.getDouble(path + "buy-price");
            double sellPrice = yaml.getDouble(path + "sell-price");
            shops.put(key, new Shop(loc, owner, material, buyPrice, sellPrice));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Shop> entry : shops.entrySet()) {
            Shop shop = entry.getValue();
            String path = entry.getKey() + ".";
            Location loc = shop.chestLocation();
            yaml.set(path + "world", loc.getWorld().getName());
            yaml.set(path + "x", loc.getBlockX());
            yaml.set(path + "y", loc.getBlockY());
            yaml.set(path + "z", loc.getBlockZ());
            yaml.set(path + "owner", shop.owner().toString());
            yaml.set(path + "material", shop.material().name());
            yaml.set(path + "buy-price", shop.buyPrice());
            yaml.set(path + "sell-price", shop.sellPrice());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save shops.yml", e);
        }
    }

    public Optional<Shop> get(Location chestLocation) {
        return Optional.ofNullable(shops.get(Shop.key(chestLocation)));
    }

    public void create(Shop shop) {
        shops.put(Shop.key(shop.chestLocation()), shop);
        save();
    }

    public boolean remove(Location chestLocation) {
        boolean removed = shops.remove(Shop.key(chestLocation)) != null;
        if (removed) {
            save();
        }
        return removed;
    }
}
