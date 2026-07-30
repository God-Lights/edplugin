package com.godlights.edplugin.bounty;

import com.godlights.edplugin.economy.EconomyHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class BountyCommand implements CommandExecutor {

    private final BountyManager bounties;
    private final EconomyHook economy;

    public BountyCommand(BountyManager bounties, EconomyHook economy) {
        this.bounties = bounties;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text("=== 현상금 목록 ===", NamedTextColor.GOLD));

        if (bounties.dragonBountyEnabled()) {
            sender.sendMessage(Component.text(
                    "엔더 드래곤 (영구) - " + economy.format(bounties.dragonBountyAmount()), NamedTextColor.LIGHT_PURPLE));
        }

        if (bounties.getAll().isEmpty()) {
            sender.sendMessage(Component.text("현재 걸린 플레이어 현상금이 없습니다.", NamedTextColor.GRAY));
            return true;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<java.util.UUID, Bounty> entry : bounties.getAll().entrySet()) {
            Bounty bounty = entry.getValue();
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            long remainingMillis = bounties.expireMillis() - (now - bounty.placedAt());
            long daysLeft = TimeUnit.MILLISECONDS.toDays(Math.max(0, remainingMillis));
            sender.sendMessage(Component.text(
                    (name == null ? entry.getKey().toString() : name) + " - " + economy.format(bounty.amount())
                            + " (만료까지 " + daysLeft + "일)",
                    NamedTextColor.YELLOW));
        }
        return true;
    }
}
