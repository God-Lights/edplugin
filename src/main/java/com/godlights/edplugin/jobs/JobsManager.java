package com.godlights.edplugin.jobs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class JobsManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<JobType, Double>> exp = new ConcurrentHashMap<>();

    private final double expPerLevel;
    private final int maxLevel;
    private final double bonusDropChancePerLevel;
    private final double payBonusPerLevel;

    public JobsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "jobs.yml");
        this.expPerLevel = plugin.getConfig().getDouble("jobs.exp-per-level", 100.0);
        this.maxLevel = plugin.getConfig().getInt("jobs.max-level", 50);
        this.bonusDropChancePerLevel = plugin.getConfig().getDouble("jobs.bonus-drop-chance-per-level", 0.005);
        this.payBonusPerLevel = plugin.getConfig().getDouble("jobs.pay-bonus-per-level", 0.01);
        load();
    }

    public void load() {
        exp.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String uuidStr : yaml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Map<JobType, Double> playerExp = new EnumMap<>(JobType.class);
            for (JobType job : JobType.values()) {
                double value = yaml.getDouble(uuidStr + "." + job.name(), 0.0);
                if (value > 0) {
                    playerExp.put(job, value);
                }
            }
            exp.put(uuid, playerExp);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<JobType, Double>> entry : exp.entrySet()) {
            for (Map.Entry<JobType, Double> jobEntry : entry.getValue().entrySet()) {
                yaml.set(entry.getKey() + "." + jobEntry.getKey().name(), jobEntry.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save jobs.yml", e);
        }
    }

    public double getExp(UUID uuid, JobType job) {
        return exp.getOrDefault(uuid, Map.of()).getOrDefault(job, 0.0);
    }

    public int getLevel(UUID uuid, JobType job) {
        int level = (int) (getExp(uuid, job) / expPerLevel);
        return Math.min(level, maxLevel);
    }

    /** Returns true if this action leveled the player up. */
    public boolean addExp(UUID uuid, JobType job, double amount) {
        Map<JobType, Double> playerExp = exp.computeIfAbsent(uuid, k -> new EnumMap<>(JobType.class));
        double before = playerExp.getOrDefault(job, 0.0);
        int levelBefore = Math.min((int) (before / expPerLevel), maxLevel);
        double after = before + amount;
        playerExp.put(job, after);
        int levelAfter = Math.min((int) (after / expPerLevel), maxLevel);
        return levelAfter > levelBefore;
    }

    public double payMultiplier(UUID uuid, JobType job) {
        return 1.0 + (getLevel(uuid, job) * payBonusPerLevel);
    }

    public double bonusDropChance(UUID uuid, JobType job) {
        return getLevel(uuid, job) * bonusDropChancePerLevel;
    }

    public int sumOfAllJobLevels(UUID uuid) {
        int total = 0;
        for (JobType job : JobType.values()) {
            total += getLevel(uuid, job);
        }
        return total;
    }
}
