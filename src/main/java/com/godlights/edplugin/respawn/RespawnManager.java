package com.godlights.edplugin.respawn;

import com.godlights.edplugin.waystone.WaystoneGUI;
import com.godlights.edplugin.waystone.WaystoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Forces a short spectator "death cam" before a player can act again.
 * The player's location is pinned (no noclip/flight, so no wall-peeking)
 * while vanilla spectator target-follow still lets them look at other
 * players. Once the timer ends they pick a waystone to respawn at.
 *
 * The penalty (death location + when it ends) is persisted, so quitting
 * mid-penalty and rejoining resumes the lock instead of skipping it, and
 * gamemode changes away from SPECTATOR are blocked while it's active -
 * including ones from an operator's /gamemode command.
 */
public final class RespawnManager implements Listener {

    private record Pending(Location location, long endsAt) {
    }

    private final JavaPlugin plugin;
    private final WaystoneManager waystones;
    private final File file;
    private final int delaySeconds;

    private final Map<UUID, Location> deathLocations = new HashMap<>();
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    public RespawnManager(JavaPlugin plugin, WaystoneManager waystones) {
        this.plugin = plugin;
        this.waystones = waystones;
        this.file = new File(plugin.getDataFolder(), "respawn_penalty.yml");
        this.delaySeconds = Math.max(1, plugin.getConfig().getInt("respawn.delay-seconds", 10));
        load();
        forceImmediateRespawn();
    }

    private void forceImmediateRespawn() {
        for (World world : plugin.getServer().getWorlds()) {
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
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
        deathLocations.remove(uuid);
        long endsAt = System.currentTimeMillis() + delaySeconds * 1000L;
        lockPlayer(player, player.getLocation().clone(), endsAt);
    }

    /** Puts (or resumes putting) a player into the frozen-spectator penalty. */
    private void lockPlayer(Player player, Location location, long endsAt) {
        UUID uuid = player.getUniqueId();
        pending.put(uuid, new Pending(location.clone(), endsAt));
        save();

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(location);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            long remainingMillis = endsAt - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                finishCountdown(player);
                return;
            }
            long remainingSeconds = (remainingMillis + 999) / 1000;
            player.sendActionBar(Component.text(
                    "부활까지 " + remainingSeconds + "초... (관전 중)", NamedTextColor.AQUA));
        }, 0L, 20L);
        countdownTasks.put(uuid, task);
    }

    private void finishCountdown(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTask(uuid);
        pending.remove(uuid);
        save();

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
        Pending p = pending.get(event.getPlayer().getUniqueId());
        if (p == null || event.getTo() == null) {
            return;
        }
        Location frozen = p.location();
        Location to = event.getTo();
        if (to.getX() != frozen.getX() || to.getY() != frozen.getY() || to.getZ() != frozen.getZ()) {
            event.setTo(new Location(frozen.getWorld(), frozen.getX(), frozen.getY(), frozen.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }

    /** Blocks any gamemode change away from SPECTATOR while the penalty is active - including /gamemode from an op. */
    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (pending.containsKey(event.getPlayer().getUniqueId()) && event.getNewGameMode() != GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Keep the pending penalty on disk; only stop the local countdown task
        // since there is no one online to show it to. Do NOT change gamemode
        // or teleport here - that used to let players skip the penalty by
        // simply disconnecting and reconnecting.
        cancelTask(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Pending p = pending.get(player.getUniqueId());
        if (p == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            long remaining = p.endsAt() - System.currentTimeMillis();
            if (remaining <= 0) {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(p.location());
                finishCountdown(player);
            } else {
                lockPlayer(player, p.location(), p.endsAt());
            }
        });
    }

    private void cancelTask(UUID uuid) {
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void load() {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String worldName = yaml.getString(key + ".world");
                World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
                if (world == null) {
                    continue;
                }
                Location location = new Location(world,
                        yaml.getDouble(key + ".x"),
                        yaml.getDouble(key + ".y"),
                        yaml.getDouble(key + ".z"),
                        (float) yaml.getDouble(key + ".yaw"),
                        (float) yaml.getDouble(key + ".pitch"));
                long endsAt = yaml.getLong(key + ".ends-at");
                pending.put(uuid, new Pending(location, endsAt));
            } catch (IllegalArgumentException ignored) {
                // malformed entry, skip
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Pending> entry : pending.entrySet()) {
            String key = entry.getKey().toString();
            Location loc = entry.getValue().location();
            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x", loc.getX());
            yaml.set(key + ".y", loc.getY());
            yaml.set(key + ".z", loc.getZ());
            yaml.set(key + ".yaw", (double) loc.getYaw());
            yaml.set(key + ".pitch", (double) loc.getPitch());
            yaml.set(key + ".ends-at", entry.getValue().endsAt());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save respawn_penalty.yml", e);
        }
    }
}
