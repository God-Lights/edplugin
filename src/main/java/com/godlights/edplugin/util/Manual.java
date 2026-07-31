package com.godlights.edplugin.util;

import com.godlights.edplugin.economy.EconomyHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared source for the game manual - used by /edplugin helper and the first-join guide book. */
public final class Manual {

    private Manual() {
    }

    /** Built fresh from the live config each time, so numbers stay accurate after /edplugin reload. */
    public static List<String> lines(FileConfiguration c, EconomyHook economy) {
        List<String> lines = new ArrayList<>();

        lines.add("&6=== EdPlugin 게임 설명서 ===");

        lines.add("&e[웨이스톤]");
        lines.add("&7/waystone create <이름> &f- 지금 위치에 웨이스톤 생성 (비용: " + economy.format(c.getDouble("waystone.create-cost")) + ")");
        lines.add("&7/waystone 또는 /waystone list &f- 목록 GUI에서 골라서 이동 (비용: " + economy.format(c.getDouble("waystone.teleport-cost")) + ")");
        lines.add("&7/waystone remove <이름> &f- 웨이스톤 삭제");

        lines.add("&e[직업]");
        lines.add("&f채굴(광부), 벌목(벌목꾼), 수확(농부), 몹 사냥(사냥꾼)을 하면 자동으로 돈과 경험치를 얻습니다.");
        lines.add("&f레벨당 지급액 +" + percent(c.getDouble("jobs.pay-bonus-per-level"))
                + ", 보너스 드랍 확률 +" + percent(c.getDouble("jobs.bonus-drop-chance-per-level"))
                + " (최대 레벨 " + c.getInt("jobs.max-level") + ")");
        lines.add("&7/jobs &f- 내 직업 레벨과 경험치 확인");

        lines.add("&e[상점]");
        lines.add("&f상자를 바라본 채로 파는 아이템을 손에 들고 명령어를 입력하세요.");
        lines.add("&7/shop create <구매가> <판매가> &f- 상점 생성 (0을 넣으면 그 방향 비활성화)");
        lines.add("&f상자 우클릭 = 구매, 웅크리고 우클릭 = 판매 (거래 수수료 " + percent(c.getDouble("shop.tax-rate")) + ", 국고로 들어감)");
        lines.add("&f본인 상점에서는 사고팔 수 없습니다. (웅크리고 우클릭하면 재고 확인/보충)");
        lines.add("&7/shop price <구매가> <판매가> &f- 가격 변경 (주인만)");
        lines.add("&7/shop remove &f- 상점 삭제 (주인만)");
        lines.add("&f관리자가 만든 서버 상점은 재고가 마르지 않고, 거래 대금이 전부 국고로 오갑니다.");

        lines.add("&e[사망과 부활]");
        lines.add("&f죽으면 " + c.getInt("respawn.delay-seconds") + "초간 그 자리에서 관전 모드가 됩니다 (위치 고정, 벽 너머는 못 봄).");
        lines.add("&f서버를 나갔다 들어오거나 게임모드를 바꾸려 해도 이 시간은 계속 흐르고 관전 상태가 유지됩니다.");
        lines.add("&f시간이 지나면 웨이스톤을 골라서 그 자리에서 부활합니다.");

        if (c.getBoolean("grave.enabled", true)) {
            lines.add("&e[무덤]");
            lines.add("&f죽으면 아이템이 바닥에 흩어지는 대신 그 자리에 무덤(상자)이 생겨서 보관됩니다.");
            lines.add("&f무덤은 본인만 열 수 있고, " + c.getInt("grave.expire-minutes")
                    + "분 안에 찾아가지 않으면 원래 블록으로 되돌아가며 남은 아이템은 바닥에 떨어집니다.");
        }

        lines.add("&e[현상금]");
        lines.add("&f다른 플레이어를 죽이면 죽인 사람에게 현상금이 걸립니다 (국고 지급).");
        lines.add("&f기본 " + economy.format(c.getDouble("bounty.base-amount")) + " + 피해자 잔액의 "
                + percent(c.getDouble("bounty.victim-balance-cut")) + " + 직업 레벨 1당 "
                + economy.format(c.getDouble("bounty.per-job-level-bonus"))
                + " (최소 " + economy.format(c.getDouble("bounty.minimum-amount"))
                + " ~ 최대 " + economy.format(c.getDouble("bounty.maximum-amount")) + ")");
        lines.add("&f이미 현상금이 걸린 사람을 죽이면 새 현상금 대신 그 현상금을 받습니다.");
        lines.add("&f현상금 대상이 " + (int) c.getDouble("bounty.warning-radius") + "블록 이내로 오면 근처 플레이어에게 경고가 뜹니다.");
        lines.add("&f현상금은 " + c.getInt("bounty.expire-days") + "일 뒤 만료되고, 엔더 드래곤에게는 "
                + economy.format(c.getDouble("bounty.ender-dragon.amount")) + " 영구 현상금이 걸려 있습니다.");
        lines.add("&7/bounty &f- 현재 걸린 현상금 목록 확인");

        lines.add("&e[경제]");
        lines.add("&f처음 잔액을 확인하면 " + economy.format(c.getDouble("economy.starting-balance")) + "으로 시작합니다.");
        lines.add("&f직업 활동, 상점 거래, 현상금 등으로 잔액이 오르내립니다.");
        lines.add("&7/balance (또는 /bal, /money) &f- 내 잔액 확인, /balance <이름>으로 다른 사람도 확인 가능");
        lines.add("&7/balance treasury &f- 서버 국고 잔액 확인 (웨이스톤 비용, 상점 수수료가 여기로 모임)");

        lines.add("&e[관리자]");
        lines.add("&7/edplugin reload &f- 설정과 데이터 다시 불러오기");
        lines.add("&7/shop create <구매가> <판매가> server &f- 국고 연동 서버 상점 생성");

        return lines;
    }

    private static String percent(double fraction) {
        return String.format(Locale.KOREA, "%.1f%%", fraction * 100);
    }

    public static Component legacyLine(String line) {
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
