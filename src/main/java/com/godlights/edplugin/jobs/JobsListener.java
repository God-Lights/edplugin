package com.godlights.edplugin.jobs;

import com.godlights.edplugin.economy.EconomyHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class JobsListener implements Listener {

    private final JobsManager jobs;
    private final EconomyHook economy;
    // Blocks a player placed this session are tracked here so breaking them
    // right back doesn't farm job pay/exp. In-memory only (reset on restart).
    private final Set<String> playerPlacedBlocks = ConcurrentHashMap.newKeySet();
    private final Map<JobType, Double> basePay = new EnumMap<>(JobType.class);
    private final Map<JobType, Double> baseExp = new EnumMap<>(JobType.class);

    public JobsListener(JavaPlugin plugin, JobsManager jobs, EconomyHook economy) {
        this.jobs = jobs;
        this.economy = economy;
        FileConfiguration config = plugin.getConfig();
        for (JobType job : JobType.values()) {
            basePay.put(job, config.getDouble("jobs.base-pay." + job.name(), 1.0));
            baseExp.put(job, config.getDouble("jobs.base-exp." + job.name(), 1.0));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        playerPlacedBlocks.add(key(event.getBlock().getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        boolean wasPlayerPlaced = playerPlacedBlocks.remove(key(block.getLocation()));
        if (wasPlayerPlaced) {
            return;
        }

        Optional<JobType> jobType = jobTypeFor(block.getType());
        if (jobType.isEmpty()) {
            return;
        }
        if (jobType.get() == JobType.FARMER && !isFullyGrown(block)) {
            return;
        }

        reward(event.getPlayer(), jobType.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Optional<JobType> jobType = jobTypeFor(event.getBlockState().getType());
        if (jobType.isEmpty()) {
            return;
        }
        double chance = jobs.bonusDropChance(event.getPlayer().getUniqueId(), jobType.get());
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        event.getItems().forEach(item -> {
            var stack = item.getItemStack();
            int doubled = Math.min(stack.getAmount() * 2, stack.getMaxStackSize());
            stack.setAmount(doubled);
            item.setItemStack(stack);
        });
        event.getPlayer().sendMessage(Component.text("보너스 드랍!", NamedTextColor.AQUA));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        reward(killer, JobType.HUNTER);
    }

    private void reward(Player player, JobType job) {
        double pay = basePay.getOrDefault(job, 0.0) * jobs.payMultiplier(player.getUniqueId(), job);
        double expGain = baseExp.getOrDefault(job, 0.0);

        if (economy.isAvailable() && pay > 0) {
            economy.deposit(player, pay);
        }
        boolean leveledUp = jobs.addExp(player.getUniqueId(), job, expGain);

        if (leveledUp) {
            int level = jobs.getLevel(player.getUniqueId(), job);
            player.sendMessage(Component.text(
                    "[" + job.displayName() + "] 레벨이 " + level + "(으)로 올랐습니다!", NamedTextColor.GOLD));
        }
    }

    private Optional<JobType> jobTypeFor(Material material) {
        String name = material.name();
        if (name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS) {
            return Optional.of(JobType.MINER);
        }
        if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) {
            return Optional.of(JobType.LUMBERJACK);
        }
        if (switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            default -> false;
        }) {
            return Optional.of(JobType.FARMER);
        }
        return Optional.empty();
    }

    private boolean isFullyGrown(Block block) {
        return block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }

    private String key(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
