package com.godlights.edplugin.bounty;

import com.godlights.edplugin.economy.EconomyHook;
import com.godlights.edplugin.jobs.JobsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class BountyManager {

    private final JavaPlugin plugin;
    private final EconomyHook economy;
    private final JobsManager jobs;
    private final File file;
    private final Map<UUID, Bounty> bounties = new HashMap<>();
    private final Map<UUID, Long> lastWarned = new HashMap<>();

    private final double baseAmount;
    private final double victimBalanceCut;
    private final double perJobLevelBonus;
    private final double minimumAmount;
    private final double maximumAmount;
    private final long expireMillis;
    private final double warningRadius;
    private final long warningCooldownMillis;
    private final boolean dragonEnabled;
    private final double dragonAmount;

    public BountyManager(JavaPlugin plugin, EconomyHook economy, JobsManager jobs) {
        this.plugin = plugin;
        this.economy = economy;
        this.jobs = jobs;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");

        var config = plugin.getConfig();
        this.baseAmount = config.getDouble("bounty.base-amount", 200.0);
        this.victimBalanceCut = config.getDouble("bounty.victim-balance-cut", 0.1);
        this.perJobLevelBonus = config.getDouble("bounty.per-job-level-bonus", 5.0);
        this.minimumAmount = config.getDouble("bounty.minimum-amount", 100.0);
        this.maximumAmount = config.getDouble("bounty.maximum-amount", 20000.0);
        this.expireMillis = config.getLong("bounty.expire-days", 30) * 24L * 60 * 60 * 1000;
        this.warningRadius = config.getDouble("bounty.warning-radius", 100.0);
        this.warningCooldownMillis = config.getLong("bounty.warning-cooldown-seconds", 60) * 1000L;
        this.dragonEnabled = config.getBoolean("bounty.ender-dragon.enabled", true);
        this.dragonAmount = config.getDouble("bounty.ender-dragon.amount", 5000.0);

        load();
        startTasks();
    }

    public boolean dragonBountyEnabled() {
        return dragonEnabled;
    }

    public double dragonBountyAmount() {
        return dragonAmount;
    }

    public void payDragonBounty(Player killer) {
        if (!dragonEnabled) {
            return;
        }
        economy.withdrawTreasury(dragonAmount);
        economy.deposit(killer, dragonAmount);
        killer.sendMessage(Component.text(
                "엔더 드래곤 처치 현상금 " + economy.format(dragonAmount) + "을(를) 획득했습니다!", NamedTextColor.LIGHT_PURPLE));
    }

    /** Handles a player-vs-player kill: pays out an existing bounty, or creates a new one on the killer. */
    public void handlePlayerKill(Player killer, Player victim) {
        Bounty victimBounty = bounties.remove(victim.getUniqueId());
        if (victimBounty != null) {
            economy.withdrawTreasury(victimBounty.amount());
            economy.deposit(killer, victimBounty.amount());
            killer.sendMessage(Component.text(
                    victim.getName() + "에게 걸린 현상금 " + economy.format(victimBounty.amount()) + "을(를) 획득했습니다!",
                    NamedTextColor.LIGHT_PURPLE));
            save();
            return;
        }

        double amount = computeAmount(victim);
        long now = System.currentTimeMillis();
        Bounty existing = bounties.get(killer.getUniqueId());
        if (existing != null) {
            existing.add(amount, now);
        } else {
            bounties.put(killer.getUniqueId(), new Bounty(killer.getUniqueId(), amount, now));
        }
        save();
        killer.sendMessage(Component.text(
                "무고한 살인으로 당신에게 현상금이 걸렸습니다! (총 " + economy.format(bounties.get(killer.getUniqueId()).amount()) + ")",
                NamedTextColor.RED));
        Bukkit.broadcast(Component.text(
                killer.getName() + "에게 현상금이 걸렸습니다: " + economy.format(bounties.get(killer.getUniqueId()).amount()),
                NamedTextColor.RED));
    }

    private double computeAmount(Player victim) {
        double balance = economy.isAvailable() ? economy.getBalance(victim) : 0.0;
        int levels = jobs.sumOfAllJobLevels(victim.getUniqueId());
        double amount = baseAmount + (balance * victimBalanceCut) + (levels * perJobLevelBonus);
        return Math.max(minimumAmount, Math.min(maximumAmount, amount));
    }

    public boolean hasBounty(UUID uuid) {
        return bounties.containsKey(uuid);
    }

    public Map<UUID, Bounty> getAll() {
        return bounties;
    }

    private void startTasks() {
        // Purge expired bounties periodically.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::purgeExpired, 20L * 60, 20L * 60 * 10);
        // Warn nearby players when a bounty target is within range.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkProximity, 100L, 100L);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        boolean changed = bounties.values().removeIf(b -> b.isExpired(now, expireMillis));
        if (changed) {
            save();
        }
    }

    private void checkProximity() {
        if (bounties.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (UUID targetId : bounties.keySet()) {
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) {
                continue;
            }
            double amount = bounties.get(targetId).amount();
            for (Player viewer : target.getWorld().getPlayers()) {
                if (viewer.getUniqueId().equals(targetId)) {
                    continue;
                }
                if (viewer.getLocation().distanceSquared(target.getLocation()) > warningRadius * warningRadius) {
                    continue;
                }
                long last = lastWarned.getOrDefault(viewer.getUniqueId(), 0L);
                if (now - last < warningCooldownMillis) {
                    continue;
                }
                lastWarned.put(viewer.getUniqueId(), now);
                viewer.sendMessage(Component.text(
                        "근처에 현상금 대상이 있습니다: " + target.getName() + " (" + economy.format(amount) + ")",
                        NamedTextColor.RED));
            }
        }
    }

    private void load() {
        bounties.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double amount = yaml.getDouble(key + ".amount");
                long placedAt = yaml.getLong(key + ".placed-at");
                bounties.put(uuid, new Bounty(uuid, amount, placedAt));
            } catch (IllegalArgumentException ignored) {
                // malformed entry, skip
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Bounty bounty : bounties.values()) {
            String key = bounty.target().toString();
            yaml.set(key + ".amount", bounty.amount());
            yaml.set(key + ".placed-at", bounty.placedAt());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save bounties.yml", e);
        }
    }

    public long expireMillis() {
        return expireMillis;
    }

    public OfflinePlayer offlineOf(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
