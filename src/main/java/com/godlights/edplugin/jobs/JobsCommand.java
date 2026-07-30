package com.godlights.edplugin.jobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JobsCommand implements CommandExecutor {

    private final JobsManager jobs;

    public JobsCommand(JobsManager jobs) {
        this.jobs = jobs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        player.sendMessage(Component.text("=== 내 직업 현황 ===", NamedTextColor.GOLD));
        for (JobType job : JobType.values()) {
            int level = jobs.getLevel(player.getUniqueId(), job);
            double exp = jobs.getExp(player.getUniqueId(), job);
            player.sendMessage(Component.text(
                    job.displayName() + " - 레벨 " + level + " (누적 exp " + (int) exp + ")",
                    NamedTextColor.YELLOW));
        }
        return true;
    }
}
