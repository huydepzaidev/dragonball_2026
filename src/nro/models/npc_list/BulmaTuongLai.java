package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.shop.ShopService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class BulmaTuongLai extends Npc {

    public BulmaTuongLai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 104 || this.mapId == 5) {
                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "build", "Cửa hàng", "Đóng");
                }
            } else if (this.mapId == 102) {
                if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                    if (hasCompletedXenLevel5Mission(player)) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "learn", "Cửa hàng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Bạn phải xong nhiệm vụ Sên 5", "Đóng");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 104 || this.mapId == 5) {
                if (player.idMark.isBaseMenu()) {
                    if (select == 0) {
                        ShopService.gI().opendShop(player, "KARIN", true);
                    }
                }
            } else if (this.mapId == 102) {
                if (player.idMark.isBaseMenu()) {
                    if (!hasCompletedXenLevel5Mission(player)) {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Bạn phải xong nhiệm vụ Sên 5", "Đóng");
                        return;
                    }
                    if (select == 0) {
                        ShopService.gI().opendShop(player, "BUNMA_FUTURE", true);
                    }
                }
                        
                
            }
        }
    }

    private boolean hasCompletedXenLevel5Mission(Player player) {
        return TaskService.gI().getIdTask(player) >= ConstTask.TASK_26_0;
    }
}
