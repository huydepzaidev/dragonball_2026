package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.server.TopRankingInfoService;
import nro.models.services.Service;

/**
 * Đại Thiên Sứ: tra cứu bảng xếp hạng và phần thưởng hiện đang cấu hình.
 */
public class DaiThienSu extends Npc {

    private static final int MENU_TOP_POWER_INFO = 210820260;
    private static final int MENU_TOP_TASK_INFO = 210820261;
    private static final int MENU_TOP_SUMMER_INFO = 210820262;
    private static final int MENU_TOP_BOSS_INFO = 210820263;

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta lưu giữ thành tích của mọi chiến binh trong vũ trụ.",
                    "Top\nsức mạnh",
                    "Top\nnhiệm vụ",
                    "Top\nsự kiện hè",
                    "Top\nsăn Boss",
                    "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 -> openTopInfo(player, MENU_TOP_POWER_INFO, TopRankingInfoService.TOP_POWER);
                case 1 -> openTopInfo(player, MENU_TOP_TASK_INFO, TopRankingInfoService.TOP_TASK);
                case 2 -> openTopInfo(player, MENU_TOP_SUMMER_INFO, TopRankingInfoService.TOP_SUMMER);
                case 3 -> openTopInfo(player, MENU_TOP_BOSS_INFO, TopRankingInfoService.TOP_BOSS);
                default -> {
                }
            }
            return;
        }
        if (select != 0) {
            return;
        }
        switch (player.idMark.getIndexMenu()) {
            case MENU_TOP_POWER_INFO -> Service.gI().showListTop(player, Manager.loadTopPower());
            case MENU_TOP_TASK_INFO -> Service.gI().showListTop(player, Manager.loadTopTask());
            case MENU_TOP_SUMMER_INFO -> Service.gI().showListTop(player, Manager.loadTopEvent());
            case MENU_TOP_BOSS_INFO -> Service.gI().showListTop(player, Manager.loadTopBoss());
            default -> {
            }
        }
    }

    private void openTopInfo(Player player, int menuId, String rankingKey) {
        createOtherMenu(player, menuId, TopRankingInfoService.gI().buildInfo(rankingKey),
                "Xem xếp\nhạng", "Đóng");
    }
}
