package com.godlights.edplugin.waystone;

import com.godlights.edplugin.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WaystoneGUI implements Listener {

    public interface SelectionHandler {
        void onSelect(Player player, Waystone waystone);
    }

    public static void open(Player player, WaystoneManager manager, String title, SelectionHandler handler) {
        List<Waystone> list = new ArrayList<>(manager.getAll().values());
        int rows = Math.max(1, Math.min(6, (list.size() / 9) + 1));
        Holder holder = new Holder(handler, list);
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Component.text(title));
        holder.setInventory(inventory);

        for (Waystone ws : list) {
            var loc = ws.location();
            inventory.addItem(new ItemBuilder(Material.LODESTONE)
                    .name(ws.name())
                    .lore(List.of(
                            String.format(Locale.ROOT, "좌표: %s, %d, %d, %d",
                                    loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                            "생성자: " + ws.creator()))
                    .build());
        }

        player.openInventory(inventory);
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private final SelectionHandler handler;
        private final List<Waystone> order;

        private Holder(SelectionHandler handler, List<Waystone> order) {
            this.handler = handler;
            this.order = order;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= holder.order.size()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Waystone waystone = holder.order.get(slot);
        player.closeInventory();
        holder.handler.onSelect(player, waystone);
    }
}
