package com.godlights.edplugin;

import com.godlights.edplugin.jobs.JobsManager;
import com.godlights.edplugin.shop.ShopManager;
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

    public EdPluginCommand(EdPlugin plugin, WaystoneManager waystones, JobsManager jobs, ShopManager shops) {
        this.plugin = plugin;
        this.waystones = waystones;
        this.jobs = jobs;
        this.shops = shops;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("edplugin.admin")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("사용법: /edplugin reload", NamedTextColor.RED));
            return true;
        }
        plugin.reloadConfig();
        waystones.load();
        jobs.load();
        shops.load();
        sender.sendMessage(Component.text("EdPlugin 설정과 데이터를 다시 불러왔습니다.", NamedTextColor.GREEN));
        return true;
    }
}
