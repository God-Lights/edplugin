package com.godlights.edplugin.bounty;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class BountyListener implements Listener {

    private final BountyManager bounties;

    public BountyListener(BountyManager bounties) {
        this.bounties = bounties;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        bounties.handlePlayerKill(killer, victim);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        Player killer = dragon.getKiller();
        if (killer == null) {
            return;
        }
        bounties.payDragonBounty(killer);
    }
}
