package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.map.service.NpcService;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.services.Service;
import nro.models.shop.ShopService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Bill extends Npc {

    private static final int DESTROY_SHOP_CONFIRM_MENU = 100;
    private static final int DESTROY_SHOP_REQUIREMENTS_MENU = 101;

    public Bill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            TaskService.gI().checkDoneTaskTalkNpc(player, this);
            player.idMark.setIndexMenu(ConstNpc.BASE_MENU);

            if (mapId == 154) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "...",
                        "Về\nthánh địa\nKaio", "Từ chối");
            } else {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chưa tới giờ thi đấu, xem hướng dẫn để biết thêm chi tiết",
                        "Nói\nchuyện", "Hướng\ndẫn\nthêm", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (mapId) {
            case 48 -> {
                switch (player.idMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU -> {
                        switch (select) {
                            case 0 -> {
                                if (InventoryService.gI().canOpenBillShop(player)) {
                                    createOtherMenu(player, DESTROY_SHOP_CONFIRM_MENU,
                                            "Mỗi món đồ Hủy Diệt cần 1 món đồ Thần cùng loại đang mặc,\n"
                                            + "99 phần thức ăn và 500 Hồng Ngọc.\n"
                                            + "Nếu tâm trạng ta vui ngươi có thể nhận trang bị tăng đến 15%.",
                                            "OK", "Từ chối");
                                } else {
                                    createOtherMenu(player, DESTROY_SHOP_REQUIREMENTS_MENU,
                                            "Ngươi phải mặc đủ bộ 5 món trang bị Thần\n"
                                            + "và mang 99 phần của một loại thức ăn tới đây...\n"
                                            + "rồi ta nói chuyện tiếp.",
                                            "OK");
                                }
                            }
                            case 1 ->
                                NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_BILL);
                        }
                    }
                    case DESTROY_SHOP_CONFIRM_MENU -> {
                        if (select == 0) {
                            if (InventoryService.gI().canOpenBillShop(player)) {
                                ShopService.gI().opendShop(player, "BILL", true);
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Bạn phải mặc đủ 5 món đồ Thần và có 99 phần của một loại thức ăn");
                            }
                        }
                    }
                }
            }

            case 154 -> {
                if (select == 0) {
                    ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                }
            }
        }
    }

}
