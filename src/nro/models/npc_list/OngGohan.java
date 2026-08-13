package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.service.NpcService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.HomeNpcGemService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.MailboxService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services_func.Input;
import nro.models.shop.ShopService;
import nro.models.utils.Util;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (openHomeMenuWithRubyGuide(player,
                    \u0022Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.\u0022)) {
                return;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.",
                    "Nhiệm vụ", "Nhập mã\ngiftcode", "Nhận\nđệ tử",
                    "Nhận ngọc\nmiễn phí", "Hòm thư");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                if (select == 5) {
                    showRubyGuide(player);
                    return;
                }
                switch (select) {
                    case 0 -> {
                        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                            NpcService.gI().createTutorial(player, tempId, this.avartar,
                                    "Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.");
                        }
                    }
                    case 1 -> {
                        Input.gI().createFormGiftCode(player);
                    }
                    case 2 -> {
                        receiveDisciple(player);
                    }
                    case 3 -> {
                        HomeNpcGemService.claimFreeGems(player);
                    }
                    case 4 -> {
                        MailboxService.openMailbox(this, player);
                    }
                }
            } else {
                if (player.idMark.getIndexMenu() == ConstNpc.HOME_RUBY_GUIDE_MENU) {
                    openBaseMenu(player);
                    return;
                }
                MailboxService.handleMenu(this, player, select);
            }
        }
    }

    protected boolean openHomeMenuWithRubyGuide(Player player, String introText) {
        createOtherMenu(player, ConstNpc.BASE_MENU, introText,
                \u0022Nhiệm vụ\u0022,
                \u0022Nhập mã\ngiftcode\u0022,
                \u0022Nhận\nđệ tử\u0022,
                \u0022Nhận ngọc\nmiễn phí\u0022,
                \u0022Hòm thư\u0022,
                \u0022Cách kiếm\nhồng ngọc\u0022);
        return true;
    }

    protected void showRubyGuide(Player player) {
        createOtherMenu(player, ConstNpc.HOME_RUBY_GUIDE_MENU,
                rubyGuideText(), \u0022Đóng\u0022);
    }

    static String rubyGuideText() {
        return \u0022Các boss có thể rơi Capsule hồng ngọc:\n\n\u0022
                + \u0022- Tiểu đội sát thủ Namek: Số 1 Namek, Số 2 Namek, Số 3 Namek, Số 4 Namek, Tiểu đội trưởng Namek (10 capsule mỗi boss).\n\u0022
                + \u0022- Tiểu đội Bojack Trái Đất: Bojack, Bujin, Kogu, Bido, Zangya, Siêu Bojack (10 capsule mỗi boss).\n\u0022
                + \u0022- Boss mini: Ăn Trộm, Mặt Trời, Ở Dơ (ngẫu nhiên 1-5 capsule mỗi boss).\n\n\u0022
                + \u0022Mở mỗi capsule nhận từ 1 đến 100 hồng ngọc.\u0022;
    }

    protected void receiveDisciple(Player player) {
        if (player.pet != null) {
            Service.gI().sendThongBao(player, "Con đã có đệ tử rồi!");
            return;
        }
        PetService.gI().createNormalPet(player, player.gender);
        Service.gI().sendThongBao(player, "Con đã nhận đệ tử thành công!");
    }
}
