package com.godlights.edplugin;

import com.godlights.edplugin.bounty.BountyCommand;
import com.godlights.edplugin.bounty.BountyListener;
import com.godlights.edplugin.bounty.BountyManager;
import com.godlights.edplugin.death.DeathMessageListener;
import com.godlights.edplugin.economy.EconomyHook;
import com.godlights.edplugin.economy.VaultEconomyBridge;
import com.godlights.edplugin.jobs.JobsCommand;
import com.godlights.edplugin.jobs.JobsListener;
import com.godlights.edplugin.jobs.JobsManager;
import com.godlights.edplugin.respawn.RespawnManager;
import com.godlights.edplugin.shop.ShopCommand;
import com.godlights.edplugin.shop.ShopListener;
import com.godlights.edplugin.shop.ShopManager;
import com.godlights.edplugin.waystone.WaystoneCommand;
import com.godlights.edplugin.waystone.WaystoneGUI;
import com.godlights.edplugin.waystone.WaystoneManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class EdPlugin extends JavaPlugin {

    private EconomyHook economy;
    private WaystoneManager waystoneManager;
    private JobsManager jobsManager;
    private ShopManager shopManager;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economy = new EconomyHook(this);
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getServer().getServicesManager().register(
                    Economy.class, new VaultEconomyBridge(economy), this, ServicePriority.Normal);
            getLogger().info("Vault 경제 서비스에 자체 경제 시스템을 등록했습니다.");
        }

        waystoneManager = new WaystoneManager(this);
        jobsManager = new JobsManager(this);
        shopManager = new ShopManager(this);
        bountyManager = new BountyManager(this, economy, jobsManager);
        RespawnManager respawnManager = new RespawnManager(this, waystoneManager);

        getServer().getPluginManager().registerEvents(new WaystoneGUI(), this);
        getServer().getPluginManager().registerEvents(new JobsListener(this, jobsManager, economy), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, economy, getConfig()), this);
        getServer().getPluginManager().registerEvents(new DeathMessageListener(getConfig()), this);
        getServer().getPluginManager().registerEvents(respawnManager, this);
        getServer().getPluginManager().registerEvents(new BountyListener(bountyManager), this);

        getCommand("waystone").setExecutor(new WaystoneCommand(waystoneManager, economy, getConfig()));
        getCommand("jobs").setExecutor(new JobsCommand(jobsManager));
        getCommand("shop").setExecutor(new ShopCommand(shopManager));
        getCommand("bounty").setExecutor(new BountyCommand(bountyManager, economy));
        getCommand("edplugin").setExecutor(new EdPluginCommand(this, waystoneManager, jobsManager, shopManager, economy));

        getLogger().info("EdPlugin이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (economy != null) {
            economy.save();
        }
        if (waystoneManager != null) {
            waystoneManager.save();
        }
        if (jobsManager != null) {
            jobsManager.save();
        }
        if (shopManager != null) {
            shopManager.save();
        }
        getLogger().info("EdPlugin이 비활성화되었습니다.");
    }
}
