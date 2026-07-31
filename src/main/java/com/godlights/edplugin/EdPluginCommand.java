package com.godlights.edplugin;

import com.godlights.edplugin.economy.EconomyHook;
import com.godlights.edplugin.jobs.JobsManager;
import com.godlights.edplugin.shop.ShopManager;
import com.godlights.edplugin.waystone.WaystoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class EdPluginCommand implements CommandExecutor {

    private static final List<String> MANUAL = List.of(
            "&6=== EdPlugin 게임 설명서 ===",
            "&e[웨이스톤]",
            "&7/waystone create <이름> &f- 지금 위치에 웨이스톤 생성",
            "&7/waystone 또는 /waystone list &f- 목록 GUI에서 골라서 이동",
            "&7/waystone remove <이름> &f- 웨이스톤 삭제",
            "&e[직업]",
            "&f채굴, 벌목, 농사, 사냥을 하면 자동으로 돈과 경험치를 얻습니다.",
            "&f레벨이 오르면 지급액과 보너스 드랍 확률이 함께 올라갑니다.",
            "&7/jobs &f- 내 직업 레벨과 경험치 확인",
            "&e[개인 상점]",
            "&f상자를 바라본 채로 파는 아이템을 손에 들고 명령어를 입력하세요.",
            "&7/shop create <구매가> <판매가> &f- 상점 생성 (0을 넣으면 그 방향 비활성화)",
            "&f상자 우클릭 = 구매, 웅크리고 우클릭 = 판매",
            "&7/shop price <구매가> <판매가> &f- 가격 변경 (주인만)",
            "&7/shop remove &f- 상점 삭제 (주인만)",
            "&e[사망과 부활]",
            "&f죽으면 10초간 그 자리에서 관전 모드가 됩니다 (위치 고정, 벽 너머는 못 봄).",
            "&f10초가 지나면 웨이스톤을 골라서 그 자리에서 부활합니다.",
            "&e[현상금]",
            "&f다른 플레이어를 죽이면 죽인 사람에게 현상금이 걸립니다 (국고 지급).",
            "&f이미 현상금이 걸린 사람을 죽이면 새 현상금 대신 그 현상금을 받습니다.",
            "&f현상금 대상이 100블록 이내로 오면 근처 플레이어에게 경고가 뜹니다.",
            "&f현상금은 30일 뒤 만료되고, 엔더 드래곤에게는 영구 현상금이 걸려 있습니다.",
            "&7/bounty &f- 현재 걸린 현상금 목록 확인",
            "&e[경제]",
            "&f처음 잔액을 확인하면 기본 지급액으로 시작합니다.",
            "&f직업 활동, 상점 거래, 현상금 등으로 잔액이 오르내립니다.");

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
        for (String line : MANUAL) {
            sender.sendMessage(legacy(line));
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

    private Component legacy(String line) {
        Component result = Component.empty().decoration(TextDecoration.ITALIC, false);
        for (String part : line.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            NamedTextColor color = switch (part.charAt(0)) {
                case '6' -> NamedTextColor.GOLD;
                case 'e' -> NamedTextColor.YELLOW;
                case '7' -> NamedTextColor.GRAY;
                default -> NamedTextColor.WHITE;
            };
            result = result.append(Component.text(part.substring(1), color));
        }
        return result;
    }
}
