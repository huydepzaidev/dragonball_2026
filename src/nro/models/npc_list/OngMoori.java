package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.services.HomeNpcGemService;
import nro.models.services.MailboxService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services_func.Input;

public class OngMoori extends OngGohan {

    public OngMoori(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (openHomeMenuWithRubyGuide(player,
                    \u0022Con cố gắng theo Trưởng Lão Guru học thành tài, đừng lo lắng cho ta.\u0022)) {
                return;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Con cố gắng theo Trưởng Lão Guru học thành tài, đừng lo lắng cho ta.",
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
                                    "Con cố gắng theo Trưởng Lão Guru học thành tài, đừng lo lắng cho ta.");
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
}
