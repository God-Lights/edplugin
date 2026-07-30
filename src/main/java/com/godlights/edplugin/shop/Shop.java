package com.godlights.edplugin.shop;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public final class Shop {

    private final Location chestLocation;
    private final UUID owner;
    private final Material material;
    private double buyPrice;
    private double sellPrice;

    public Shop(Location chestLocation, UUID owner, Material material, double buyPrice, double sellPrice) {
        this.chestLocation = chestLocation;
        this.owner = owner;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public Location chestLocation() {
        return chestLocation;
    }

    public UUID owner() {
        return owner;
    }

    public Material material() {
        return material;
    }

    public double buyPrice() {
        return buyPrice;
    }

    public double sellPrice() {
        return sellPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public static String key(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
