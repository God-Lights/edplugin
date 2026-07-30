package com.godlights.edplugin.waystone;

import com.godlights.edplugin.economy.EconomyHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class WaystoneCommand implements CommandExecutor {

    private final WaystoneManager manager;
    private final EconomyHook economy;
    private final FileConfiguration config;

    public WaystoneCommand(WaystoneManager manager, EconomyHook economy, FileConfiguration config) {
        this.manager = manager;
        this.economy = economy;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            WaystoneGUI.open(player, manager, "웨이스톤 - 이동할 곳을 선택하세요", this::handleTeleport);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!player.hasPermission("edplugin.waystone.create")) {
                    player.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("사용법: /waystone create <이름>", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                if (manager.exists(name)) {
                    player.sendMessage(Component.text("이미 존재하는 웨이스톤 이름입니다.", NamedTextColor.RED));
                    return true;
                }
                double cost = config.getDouble("waystone.create-cost");
                if (cost > 0 && economy.isAvailable() && !economy.withdraw(player, cost)) {
                    player.sendMessage(Component.text(
                            "돈이 부족합니다. (필요: " + economy.format(cost) + ")", NamedTextColor.RED));
                    return true;
                }
                manager.create(name, player.getLocation(), player.getName());
                player.sendMessage(Component.text("웨이스톤 '" + name + "'을(를) 생성했습니다.", NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (!player.hasPermission("edplugin.waystone.create")) {
                    player.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("사용법: /waystone remove <이름>", NamedTextColor.RED));
                    return true;
                }
                if (manager.remove(args[1])) {
                    player.sendMessage(Component.text("웨이스톤 '" + args[1] + "'을(를) 삭제했습니다.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("해당 이름의 웨이스톤이 없습니다.", NamedTextColor.RED));
                }
            }
            case "list" -> WaystoneGUI.open(player, manager, "웨이스톤 - 이동할 곳을 선택하세요", this::handleTeleport);
            default -> player.sendMessage(Component.text("사용법: /waystone [create|remove|list] <이름>", NamedTextColor.RED));
        }
        return true;
    }

    private void handleTeleport(Player player, Waystone waystone) {
        double cost = config.getDouble("waystone.teleport-cost");
        if (cost > 0 && economy.isAvailable() && !economy.withdraw(player, cost)) {
            player.sendMessage(Component.text(
                    "돈이 부족합니다. (필요: " + economy.format(cost) + ")", NamedTextColor.RED));
            return;
        }
        player.teleport(waystone.location());
        player.sendMessage(Component.text("웨이스톤 '" + waystone.name() + "'(으)로 이동했습니다.", NamedTextColor.GREEN));
    }
}
