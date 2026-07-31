package com.godlights.edplugin.onboarding;

import com.godlights.edplugin.economy.EconomyHook;
import com.godlights.edplugin.util.Manual;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Hands a written copy of the manual to players the first time they ever join. */
public final class GuideBookListener implements Listener {

    private static final int LINES_PER_PAGE = 10;

    private final JavaPlugin plugin;
    private final EconomyHook economy;

    public GuideBookListener(JavaPlugin plugin, EconomyHook economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("EdPlugin 안내서"));
        meta.author(Component.text("서버"));

        List<String> lines = Manual.lines(plugin.getConfig(), economy);
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
            List<String> chunk = lines.subList(i, Math.min(i + LINES_PER_PAGE, lines.size()));
            Component page = Component.empty();
            for (int j = 0; j < chunk.size(); j++) {
                if (j > 0) {
                    page = page.append(Component.newline());
                }
                page = page.append(Manual.legacyLine(chunk.get(j)));
            }
            meta.addPages(page);
        }
        book.setItemMeta(meta);

        player.getInventory().addItem(book)
                .values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
    }
}
