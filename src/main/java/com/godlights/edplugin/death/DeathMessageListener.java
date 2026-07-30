package com.godlights.edplugin.death;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class DeathMessageListener implements Listener {

    private final FileConfiguration config;

    public DeathMessageListener(FileConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!config.getBoolean("death-messages.enabled", true)) {
            return;
        }
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        List<String> templates = config.getStringList("death-messages.templates");
        if (templates.isEmpty()) {
            return;
        }
        String template = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        String message = template
                .replace("%victim%", victim.getName())
                .replace("%killer%", killer.getName())
                .replace("%weapon%", weaponName(killer));
        event.deathMessage(Component.text(message));
    }

    private String weaponName(Player killer) {
        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return "맨손";
        }
        if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            return hand.getItemMeta().getDisplayName();
        }
        return hand.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
