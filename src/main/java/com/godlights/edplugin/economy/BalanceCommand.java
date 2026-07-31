package com.godlights.edplugin.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BalanceCommand implements CommandExecutor {

    private final EconomyHook economy;

    public BalanceCommand(EconomyHook economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("사용법: /balance <플레이어>", NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text(
                    "내 잔액: " + economy.format(economy.getBalance(player)), NamedTextColor.GREEN));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        sender.sendMessage(Component.text(
                target.getName() + "의 잔액: " + economy.format(economy.getBalance(target)), NamedTextColor.GREEN));
        return true;
    }
}
