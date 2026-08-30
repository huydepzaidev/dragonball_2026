package nro.models.matches.dai_hoi_vo_thuat;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.consts.ConstTournament;
import java.util.ArrayList;

import nro.models.matches.giai_dau.WorldMartialArtsTournamentManager;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.TimeUtil;

public class WorldMartialArtsTournamentService extends ConstTournament {

    public static boolean isTournamentHour(int hour) {
        for (int h : TOURNAMENT_HOURS) {
            if (h == hour) {
                return true;
            }
        }
        return false;
    }

    public static int getTournament() {
        int hours = TimeUtil.getCurrHour();
        if (isTournamentHour(hours)) {
            return NGOAI_HANG;
        }
        return -1;
    }

    public static int getNextTournamentTime() {
        int hour = TimeUtil.getCurrHour();
        int minute = TimeUtil.getCurrMin();
        for (int h : TOURNAMENT_HOURS) {
            if (h > hour || (h == hour && minute < MINS_MAX_CAN_REG)) {
                return h;
            }
        }
        return TOURNAMENT_HOURS[0]; // 09:00 sáng hôm sau
    }

    public static String sayText() {
        return WorldMartialArtsTournamentManager.gI().canReg
                ? "Chào mừng bạn đến với đại hội võ thuật\nGiải Ngoại Hạng đang có "
                + WorldMartialArtsTournamentManager.gI().getRegisteredCount() + " người đăng ký thi đấu"
                : "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau vào lúc " + getNextTournamentTime() + "h";
    }

    public static void menu(Npc npc, Player player) {
        if (WorldMartialArtsTournamentManager.gI().round != 0 && WorldMartialArtsTournamentManager.gI().checkPlayer(player.id)) {
            NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                    "Bạn được vào vòng " + (WorldMartialArtsTournamentManager.gI().round + 1) + "\nTrận tiếp theo sắp diễn ra, hãy đợi tại đây");
            return;
        }
        boolean canReg = WorldMartialArtsTournamentManager.gI().canReg;
        int tour = getTournament();
        ArrayList<String> menu = new ArrayList<>();
        menu.add("Thông tin\nChi tiết");
        if (canReg && tour != -1) {
            if (!regCheck(player)) {
                menu.add("Đăng kí\n(100 ngọc)");
            } else {
                menu.add("Hủy\nđăng kí");
            }
            menu.add("Giải\nSiêu Hạng");
            menu.add("Đại Hội\nVõ Thuật\nLần thứ\n23");
        } else {
            menu.add("Giải\nSiêu Hạng");
            menu.add("Đại Hội\nVõ Thuật\nLần thứ\n23");
            menu.add("Đóng");
        }
        npc.createOtherMenu(player, ConstNpc.BASE_MENU, sayText(), menu.toArray(String[]::new));
    }

    public static void confirm(Npc npc, Player player, int select) {
        boolean canReg = WorldMartialArtsTournamentManager.gI().canReg;
        int tour = getTournament();

        switch (player.idMark.getIndexMenu()) {
            case ConstNpc.DANGKYDHVT_CONFIRM -> {
                if (select == 0 && canReg && tour != -1) {
                    dangky_huy(npc, player);
                }
            }
            case ConstNpc.BASE_MENU -> {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, npc.tempId, npc.avartar, ConstNpc.THONG_TIN_DAI_HOI_VO_THUAT);
                    case 1 -> {
                        if (canReg && tour != -1) {
                            if (!regCheck(player)) {
                                ArrayList<String> menu = new ArrayList<>();
                                menu.add("Giải\nNgoại Hạng\n(100 Hồng Ngọc)");
                                menu.add("Từ chối");
                                npc.createOtherMenu(player, ConstNpc.DANGKYDHVT_CONFIRM,
                                        "Hiện đang mở đăng ký Giải Ngoại Hạng (Phí 100 Hồng Ngọc, Yêu cầu xong Rambo & Sức mạnh >= 40 tỷ), bạn có muốn đăng ký không?",
                                        menu.toArray(String[]::new));
                            } else {
                                dangky_huy(npc, player);
                            }
                            break;
                        }
                        ChangeMapService.gI().changeMapNonSpaceship(player, 113, player.location.x, 360);
                    }
                    case 2 -> {
                        if (canReg && tour != -1) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 113, player.location.x, 360);
                            break;
                        }
                        ChangeMapService.gI().changeMapNonSpaceship(player, 129, player.location.x, 360);
                    }
                    case 3 -> {
                        if (canReg && tour != -1) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 129, player.location.x, 360);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static boolean regCheck(Player player) {
        return WorldMartialArtsTournamentManager.gI().isRegistered(player.id);
    }

    public static void dangky_huy(Npc npc, Player player) {
        if (player == null) {
            return;
        }
        synchronized (player) {
            if (WorldMartialArtsTournamentManager.gI().listChamp.contains(player.name)) {
                NpcService.gI().createTutorial(player, npc.tempId, npc.avartar, TEXT_DA_VO_DICH);
                return;
            }
            if (!WorldMartialArtsTournamentManager.gI().isRegistered(player.id)) {
                if (!WorldMartialArtsTournamentManager.gI().canReg || getTournament() == -1) {
                    NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                            "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau vào lúc " + getNextTournamentTime() + "h");
                    return;
                }

                // 1. Kiểm tra nhiệm vụ tiêu diệt Rambo
                if (TaskService.gI().getIdTask(player) < ConstTask.TASK_20_0) {
                    NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                            "Bạn cần hoàn thành nhiệm vụ tiêu diệt Rambo trước khi tham gia Giải Ngoại Hạng.");
                    return;
                }

                // 2. Kiểm tra sức mạnh >= 40 tỷ
                if (player.nPoint.power < REQUIRED_POWER) {
                    NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                            "Sức mạnh của bạn chưa đủ 40 tỷ để tham gia Giải Ngoại Hạng.");
                    return;
                }

                // 3. Kiểm tra phí 100 Hồng Ngọc
                synchronized (player) {
                    if (player.inventory.ruby < REG_RUBY_COST) {
                        NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                                "Bạn không đủ Hồng Ngọc, còn thiếu " + (REG_RUBY_COST - player.inventory.ruby) + " Hồng Ngọc nữa.");
                        return;
                    }

                    // Trừ phí đăng ký
                    player.inventory.ruby -= REG_RUBY_COST;
                    Service.gI().sendMoney(player);
                }

                // Ghi nhận đăng ký
                WorldMartialArtsTournamentManager.gI().registerPlayer(player);

                String minStr = String.format("%02d", TimeUtil.getCurrMin());
                NpcService.gI().createTutorial(player, npc.tempId, npc.avartar,
                        ConstTournament.TEXT_DANG_KY_THANH_CONG.replaceAll("%1", TimeUtil.getCurrHour() + "")
                                .replaceAll("%2", TimeUtil.getCurrHour() + "h" + minStr));
            } else {
                // Hủy đăng ký
                WorldMartialArtsTournamentManager.gI().unregisterPlayer(player);
                NpcService.gI().createTutorial(player, npc.tempId, npc.avartar, ConstTournament.TEXT_HUY_DANG_KY);
            }
        }
    }
}
