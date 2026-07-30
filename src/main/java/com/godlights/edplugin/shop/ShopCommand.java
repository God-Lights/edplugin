package com.godlights.edplugin.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class ShopCommand implements CommandExecutor {

    private static final int MAX_DISTANCE = 6;

    private final ShopManager shops;

    public ShopCommand(ShopManager shops) {
        this.shops = shops;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("사용법: /shop <create|price|remove> ...", NamedTextColor.RED));
            return true;
        }

        Block target = player.getTargetBlockExact(MAX_DISTANCE);
        if (target == null || !(target.getState() instanceof Chest)) {
            player.sendMessage(Component.text("상자를 바라본 상태에서 사용하세요.", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(player, target, args);
            case "price" -> updatePrice(player, target, args);
            case "remove" -> remove(player, target);
            default -> player.sendMessage(Component.text("사용법: /shop <create|price|remove> ...", NamedTextColor.RED));
        }
        return true;
    }

    private void create(Player player, Block chestBlock, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("사용법: /shop create <구매가> <판매가> (0 = 비활성화)", NamedTextColor.RED));
            return;
        }
        if (shops.get(chestBlock.getLocation()).isPresent()) {
            player.sendMessage(Component.text("이 상자는 이미 상점으로 등록되어 있습니다.", NamedTextColor.RED));
            return;
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        Material material = inHand.getType();
        if (material == Material.AIR) {
            player.sendMessage(Component.text("판매할 아이템을 손에 들고 등록하세요.", NamedTextColor.RED));
            return;
        }
        double buyPrice;
        double sellPrice;
        try {
            buyPrice = Double.parseDouble(args[1]);
            sellPrice = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("가격은 숫자로 입력하세요.", NamedTextColor.RED));
            return;
        }
        shops.create(new Shop(chestBlock.getLocation(), player.getUniqueId(), material, buyPrice, sellPrice));
        player.sendMessage(Component.text(
                material + " 상점을 생성했습니다. (구매가 " + buyPrice + " / 판매가 " + sellPrice + ")", NamedTextColor.GREEN));
    }

    private void updatePrice(Player player, Block chestBlock, String[] args) {
        Optional<Shop> shopOpt = shops.get(chestBlock.getLocation());
        if (shopOpt.isEmpty()) {
            player.sendMessage(Component.text("이 상자는 상점이 아닙니다.", NamedTextColor.RED));
            return;
        }
        Shop shop = shopOpt.get();
        if (!shop.owner().equals(player.getUniqueId()) && !player.hasPermission("edplugin.admin")) {
            player.sendMessage(Component.text("이 상점의 주인만 가격을 바꿀 수 있습니다.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("사용법: /shop price <구매가> <판매가>", NamedTextColor.RED));
            return;
        }
        try {
            shop.setBuyPrice(Double.parseDouble(args[1]));
            shop.setSellPrice(Double.parseDouble(args[2]));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("가격은 숫자로 입력하세요.", NamedTextColor.RED));
            return;
        }
        shops.create(shop);
        player.sendMessage(Component.text("가격을 변경했습니다.", NamedTextColor.GREEN));
    }

    private void remove(Player player, Block chestBlock) {
        Optional<Shop> shopOpt = shops.get(chestBlock.getLocation());
        if (shopOpt.isEmpty()) {
            player.sendMessage(Component.text("이 상자는 상점이 아닙니다.", NamedTextColor.RED));
            return;
        }
        if (!shopOpt.get().owner().equals(player.getUniqueId()) && !player.hasPermission("edplugin.admin")) {
            player.sendMessage(Component.text("이 상점의 주인만 삭제할 수 있습니다.", NamedTextColor.RED));
            return;
        }
        shops.remove(chestBlock.getLocation());
        player.sendMessage(Component.text("상점을 삭제했습니다.", NamedTextColor.YELLOW));
    }
}
