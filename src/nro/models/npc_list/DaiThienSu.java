package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.Service;

/**
 *
 * @author By Mr Blue
 * 
 */

public class DaiThienSu extends Npc {

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
        if (canOpenNpc(player) && player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 -> Service.gI().showListTop(player, Manager.loadTopPower());
                case 1 -> Service.gI().showListTop(player, Manager.loadTopTask());
                case 2 -> Service.gI().showListTop(player, Manager.loadTopEvent());
                case 3 -> Service.gI().showListTop(player, Manager.loadTopBoss());
                default -> {
                }
            }
        }
    }
}
