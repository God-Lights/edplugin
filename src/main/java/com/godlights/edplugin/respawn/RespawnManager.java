package com.godlights.edplugin.respawn;

import com.godlights.edplugin.waystone.WaystoneGUI;
import com.godlights.edplugin.waystone.WaystoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forces a short spectator "death cam" before a player can act again.
 * The player's location is pinned (no noclip/flight, so no wall-peeking)
 * while vanilla spectator target-follow still lets them look at other
 * players. Once the timer ends they pick a waystone to respawn at.
 */
public final class RespawnManager implements Listener {

    private final JavaPlugin plugin;
    private final WaystoneManager waystones;
    private final int delaySeconds;

    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Map<UUID, Location> frozenLocations = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    public RespawnManager(JavaPlugin plugin, WaystoneManager waystones) {
        this.plugin = plugin;
        this.waystones = waystones;
        this.delaySeconds = Math.max(1, plugin.getConfig().getInt("respawn.delay-seconds", 10));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        deathLocations.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathLocations.get(player.getUniqueId());
        if (deathLocation != null && deathLocation.getWorld() != null) {
            event.setRespawnLocation(deathLocation);
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> beginSpectate(player));
    }

    private void beginSpectate(Player player) {
        UUID uuid = player.getUniqueId();
        player.setGameMode(GameMode.SPECTATOR);
        frozenLocations.put(uuid, player.getLocation().clone());

        int[] remaining = {delaySeconds};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (remaining[0] <= 0) {
                finishCountdown(player);
                return;
            }
            player.sendActionBar(Component.text(
                    "부활까지 " + remaining[0] + "초... (관전 중)", NamedTextColor.AQUA));
            remaining[0]--;
        }, 0L, 20L);
        countdownTasks.put(uuid, task);
    }

    private void finishCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTask(uuid);
        frozenLocations.remove(uuid);
        deathLocations.remove(uuid);

        if (waystones.getAll().isEmpty()) {
            finalizeRespawn(player, player.getWorld().getSpawnLocation());
            return;
        }
        WaystoneGUI.open(player, waystones, "부활할 웨이스톤을 선택하세요",
                (p, waystone) -> finalizeRespawn(p, waystone.location()));
    }

    private void finalizeRespawn(Player player, Location location) {
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(location);
        player.sendMessage(Component.text("부활했습니다.", NamedTextColor.GREEN));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location frozen = frozenLocations.get(event.getPlayer().getUniqueId());
        if (frozen == null || event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        if (to.getX() != frozen.getX() || to.getY() != frozen.getY() || to.getZ() != frozen.getZ()) {
            event.setTo(new Location(frozen.getWorld(), frozen.getX(), frozen.getY(), frozen.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!frozenLocations.containsKey(uuid) && !countdownTasks.containsKey(uuid)) {
            return;
        }
        cancelTask(uuid);
        frozenLocations.remove(uuid);
        Location fallback = deathLocations.remove(uuid);
        player.setGameMode(GameMode.SURVIVAL);
        if (fallback != null && fallback.getWorld() != null) {
            player.teleport(fallback);
        }
    }

    private void cancelTask(UUID uuid) {
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
