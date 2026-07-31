package com.godlights.edplugin;

import com.godlights.edplugin.economy.EconomyHook;
import com.godlights.edplugin.jobs.JobsManager;
import com.godlights.edplugin.shop.ShopManager;
import com.godlights.edplugin.util.Manual;
import com.godlights.edplugin.waystone.WaystoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class EdPluginCommand implements CommandExecutor {

    private final EdPlugin plugin;
    private final WaystoneManager waystones;
    private final JobsManager jobs;
    private final ShopManager shops;
    private final EconomyHook economy;

    public EdPluginCommand(EdPlugin plugin, WaystoneManager waystones, JobsManager jobs, ShopManager shops,
                            EconomyHook economy) {
        this.plugin = plugin;
        this.waystones = waystones;
        this.jobs = jobs;
        this.shops = shops;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("사용법: /edplugin <helper|reload>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "helper" -> sendManual(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(Component.text("사용법: /edplugin <helper|reload>", NamedTextColor.RED));
        }
        return true;
    }

    private void sendManual(CommandSender sender) {
        if (!sender.hasPermission("edplugin.helper")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        for (String line : Manual.lines(plugin.getConfig(), economy)) {
            sender.sendMessage(Manual.legacyLine(line));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("edplugin.admin")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        plugin.reloadConfig();
        waystones.load();
        jobs.load();
        shops.load();
        economy.load();
        sender.sendMessage(Component.text("EdPlugin 설정과 데이터를 다시 불러왔습니다.", NamedTextColor.GREEN));
    }
}
