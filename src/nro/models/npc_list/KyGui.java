package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.shop_ky_gui.ConsignShopService;

public class KyGui extends Npc {

    public KyGui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, 0,
                    "Cửa hàng ký gửi hiện đang tạm đóng để bảo trì.",
                    "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (canOpenNpc(pl)) {
            nro.models.services.Service.gI().sendThongBao(pl, "Chức năng ký gửi hiện đang tạm đóng");
        }
    }
}
