package com.godlights.edplugin.shop;

import com.godlights.edplugin.economy.EconomyHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class ShopListener implements Listener {

    private final ShopManager shops;
    private final EconomyHook economy;
    private final double taxRate;

    public ShopListener(ShopManager shops, EconomyHook economy, FileConfiguration config) {
        this.shops = shops;
        this.economy = economy;
        this.taxRate = config.getDouble("shop.tax-rate", 0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Optional<Shop> shopOpt = shops.get(event.getBlock().getLocation());
        if (shopOpt.isEmpty()) {
            return;
        }
        Shop shop = shopOpt.get();
        Player player = event.getPlayer();
        if (!shop.owner().equals(player.getUniqueId()) && !player.hasPermission("edplugin.admin")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("이 상점은 다른 사람 소유입니다.", NamedTextColor.RED));
            return;
        }
        shops.remove(shop.chestLocation());
        player.sendMessage(Component.text("상점을 제거했습니다.", NamedTextColor.YELLOW));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Chest chest)) {
            return;
        }
        Optional<Shop> shopOpt = shops.get(block.getLocation());
        if (shopOpt.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Shop shop = shopOpt.get();
        Player player = event.getPlayer();

        if (player.getUniqueId().equals(shop.owner()) && player.isSneaking()) {
            player.openInventory(chest.getInventory());
            return;
        }

        if (player.isSneaking()) {
            handleSell(player, shop, chest.getInventory());
        } else {
            handleBuy(player, shop, chest.getInventory());
        }
    }

    private void handleBuy(Player player, Shop shop, Inventory chestInventory) {
        if (shop.buyPrice() <= 0) {
            player.sendMessage(Component.text("이 상점은 이 아이템을 판매하지 않습니다.", NamedTextColor.RED));
            return;
        }
        if (!chestInventory.containsAtLeast(new ItemStack(shop.material()), 1)) {
            player.sendMessage(Component.text("상점 재고가 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!economy.isAvailable()) {
            player.sendMessage(Component.text("경제 플러그인(Vault)이 연결되어 있지 않습니다.", NamedTextColor.RED));
            return;
        }
        if (!economy.withdraw(player, shop.buyPrice())) {
            player.sendMessage(Component.text(
                    "돈이 부족합니다. (필요: " + economy.format(shop.buyPrice()) + ")", NamedTextColor.RED));
            return;
        }
        chestInventory.removeItem(new ItemStack(shop.material(), 1));
        player.getInventory().addItem(new ItemStack(shop.material(), 1))
                .values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.owner());
        economy.deposit(owner, shop.buyPrice() * (1 - taxRate));
        player.sendMessage(Component.text(
                shop.material() + " 1개를 " + economy.format(shop.buyPrice()) + "에 구매했습니다.", NamedTextColor.GREEN));
    }

    private void handleSell(Player player, Shop shop, Inventory chestInventory) {
        if (shop.sellPrice() <= 0) {
            player.sendMessage(Component.text("이 상점은 이 아이템을 매입하지 않습니다.", NamedTextColor.RED));
            return;
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType() != shop.material() || inHand.getAmount() < 1) {
            player.sendMessage(Component.text("판매할 아이템을 손에 들어야 합니다: " + shop.material(), NamedTextColor.RED));
            return;
        }
        if (chestInventory.firstEmpty() == -1 && !chestInventory.containsAtLeast(new ItemStack(shop.material()), 1)) {
            player.sendMessage(Component.text("상점 창고가 가득 찼습니다.", NamedTextColor.RED));
            return;
        }
        if (!economy.isAvailable()) {
            player.sendMessage(Component.text("경제 플러그인(Vault)이 연결되어 있지 않습니다.", NamedTextColor.RED));
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.owner());
        if (!economy.withdraw(owner, shop.sellPrice())) {
            player.sendMessage(Component.text("상점 주인의 잔액이 부족해 거래할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        inHand.setAmount(inHand.getAmount() - 1);
        chestInventory.addItem(new ItemStack(shop.material(), 1));
        economy.deposit(player, shop.sellPrice() * (1 - taxRate));
        player.sendMessage(Component.text(
                shop.material() + " 1개를 " + economy.format(shop.sellPrice()) + "에 판매했습니다.", NamedTextColor.GREEN));
    }
}
