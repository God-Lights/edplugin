package com.godlights.edplugin.grave;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * On death, drops are tucked into a chest at the death spot instead of
 * scattering on the ground, so a beginner doesn't lose everything to
 * whoever wanders by first. Only the owner (or an admin) can open it; it
 * restores the original block once emptied or after it expires.
 */
public final class GraveListener implements Listener {

    private final JavaPlugin plugin;
    private final GraveManager graves;
    private final boolean enabled;
    private final long expireMillis;

    public GraveListener(JavaPlugin plugin, GraveManager graves, FileConfiguration config) {
        this.plugin = plugin;
        this.graves = graves;
        this.enabled = config.getBoolean("grave.enabled", true);
        this.expireMillis = config.getLong("grave.expire-minutes", 30) * 60_000L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::purgeExpired, 20L * 60, 20L * 60);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!enabled || event.getDrops().isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        Location location = player.getLocation().getBlock().getLocation();
        Block block = location.getBlock();

        String originalData = block.getBlockData().getAsString();
        block.setType(Material.CHEST, false);
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }
        Inventory inventory = chest.getInventory();

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        List<ItemStack> overflow = new ArrayList<>(inventory.addItem(drops.toArray(new ItemStack[0])).values());
        chest.update();

        for (ItemStack leftover : overflow) {
            location.getWorld().dropItemNaturally(location, leftover);
        }

        long expiresAt = System.currentTimeMillis() + expireMillis;
        graves.create(location, player.getUniqueId(), originalData, expiresAt);

        player.sendMessage(Component.text(
                "무덤이 (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                        + ")에 생겼습니다. " + (expireMillis / 60_000) + "분 안에 찾아가세요.",
                NamedTextColor.GOLD));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Optional<Grave> graveOpt = graves.get(block.getLocation());
        if (graveOpt.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        Grave grave = graveOpt.get();
        if (!player.getUniqueId().equals(grave.owner()) && !player.hasPermission("edplugin.admin")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("이 무덤은 당신 것이 아닙니다.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest chest)) {
            return;
        }
        Location location = chest.getLocation();
        Optional<Grave> graveOpt = graves.get(location);
        if (graveOpt.isEmpty()) {
            return;
        }
        if (isEmpty(event.getInventory())) {
            restore(graveOpt.get());
        }
    }

    private boolean isEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        for (Grave grave : List.copyOf(graves.getAll().values())) {
            if (grave.isExpired(now)) {
                restore(grave);
            }
        }
    }

    private void restore(Grave grave) {
        Block block = grave.location().getBlock();
        if (block.getState() instanceof Chest chest) {
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    grave.location().getWorld().dropItemNaturally(grave.location(), item);
                }
            }
            chest.getInventory().clear();
        }
        block.setBlockData(Bukkit.createBlockData(grave.originalBlockData()), false);
        graves.remove(grave.location());
    }
}
