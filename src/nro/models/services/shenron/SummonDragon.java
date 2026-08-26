package nro.models.services.shenron;

import java.util.HashMap;
import java.util.Map;
import nro.models.item.Item;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.item.Item.ItemOption;
import nro.models.map.Zone;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.network.Message;
import nro.models.services.ItemService;
import nro.models.services.InventoryService;
import nro.models.utils.Logger;
import java.util.List;
import nro.models.consts.ConstTaskBadges;
import nro.models.server.Maintenance;
import nro.models.task.BadgesTaskService;

/**
 *
 * @author By Mr Blue
 *
 */
public class SummonDragon {

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;

    public static final byte DRAGON_SHENRON = 0;
    public static final byte DRAGON_PORUNGA = 1;

    public static final short NGOC_RONG_1_SAO = 14;
    public static final short NGOC_RONG_2_SAO = 15;
    public static final short NGOC_RONG_3_SAO = 16;
    public static final short NGOC_RONG_4_SAO = 17;
    public static final short NGOC_RONG_5_SAO = 18;
    public static final short NGOC_RONG_6_SAO = 19;
    public static final short NGOC_RONG_7_SAO = 20;

    public static final String SUMMON_SHENRON_TUTORIAL
            = "Hiện chỉ có thể gọi rồng thần từ ngọc rồng 1 sao\n"
            + "Gọi rồng từ ngọc 2 sao và 3 sao đã tạm tắt\n"
            + "Để gọi rồng 1 sao cần ngọc từ 1 sao đến 7 sao\n"
            + "Các điều ước hồng ngọc, ngọc và vàng đã tắt\n"
            + "Rồng 1 sao vẫn có các phần thưởng đặc biệt khác\n"
            + "Ngọc rồng sẽ mất ngay khi gọi rồng dù bạn có ước hay không\n"
            + "Quá 5 phút nếu không ước rồng thần sẽ bay mất";
    public static final String SHENRON_SAY
            = "Ta sẽ ban cho người 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ trước khi quyết định";

    public static final String[] SHENRON_1_STAR_WISHES_1
            = new String[]{"Găng tay\nđang mang\nlên 1 cấp", "Chí mạng\nGốc +2%",
                "Thay\nChiêu 2-3\nĐệ tử", "Điều ước\nkhác"};
    public static final String[] SHENRON_1_STAR_WISHES_2
            = new String[]{"Đẹp trai\nnhất\nVũ trụ", "Găng tay đệ\nđang mang\nlên 1 cấp",
                "Điều ước\nkhác"};
    public static final String[] SHENRON_1_STAR_WISHES_3
            = new String[]{"Quần\nđang mang\nlên 1 cấp", "Quần đệ\nđang mang\nlên 1 cấp",
                "Điều ước\nkhác"};
    public static final String[] SHENRON_2_STARS_BlackGokuHES
            = new String[0];
    public static final String[] SHENRON_3_STARS_BlackGokuHES
            = new String[0];
    //--------------------------------------------------------------------------
    private static SummonDragon instance;
    private final Map pl_dragonStar;
    private long lastTimeShenronAppeared;
    private long lastTimeShenronWait;
    private final int timeResummonShenron = 600000;
    private boolean isShenronAppear;
    private final int timeShenronWait = 300000;

    private final Thread update;
    private boolean active;

    public boolean isPlayerDisconnect;
    public Player playerSummonShenron;
    private int playerSummonShenronId;
    private Zone mapShenronAppear;
    private byte shenronStar;
    private int menuShenron;
    private byte select;

    private SummonDragon() {
        this.pl_dragonStar = new HashMap<>();
        this.update = new Thread(() -> {
            while (active && !Maintenance.isRunning) {
                try {
                    if (isShenronAppear) {
                        if (isPlayerDisconnect) {
                            List<Player> players = mapShenronAppear.getPlayers();
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonShenronId) {
                                    playerSummonShenron = plMap;
                                    reSummonShenron();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }

                        }
                        if (Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                            shenronLeave(playerSummonShenron, TIME_UP);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Logger.logException(SummonDragon.class, e);
                }
            }
        });
        this.active();
    }

    private void active() {
        if (!active) {
            active = true;
            this.update.start();
        }
    }

    public static SummonDragon gI() {
        if (instance == null) {
            instance = new SummonDragon();
        }
        return instance;
    }

    public void openMenuSummonShenron(Player pl, byte dragonBallStar) {
        if (!isSummonEnabled(dragonBallStar)) {
            Service.gI().sendThongBao(pl,
                    "Gọi rồng thần từ ngọc " + dragonBallStar + " sao hiện đang tắt");
            return;
        }
        this.pl_dragonStar.put(pl, dragonBallStar);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON, -1, "Bạn muốn gọi rồng thần ?",
                "Hướng\ndẫn thêm\n(mới)", "Gọi\nRồng Thần\n" + dragonBallStar + " Sao");
    }

    public void summonShenron(Player pl) {
        byte dragonStar;
        try {
            dragonStar = Byte.parseByte(String.valueOf(pl_dragonStar.get(pl)));
        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Lựa chọn gọi rồng không hợp lệ");
            return;
        }
        if (!isSummonEnabled(dragonStar)) {
            Service.gI().sendThongBao(pl,
                    "Gọi rồng thần từ ngọc " + dragonStar + " sao hiện đang tắt");
            return;
        }
        if (pl.zone.map.mapId == 0 || pl.zone.map.mapId == 7 || pl.zone.map.mapId == 14) {
            if (checkShenronBall(pl)) {
                if (isShenronAppear) {
                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    return;
                }

                if (Util.canDoWithTime(lastTimeShenronAppeared, timeResummonShenron)) {
                    //gọi rồng
                    playerSummonShenron = pl;
                    playerSummonShenronId = (int) pl.id;
                    mapShenronAppear = pl.zone;
                    int begin = NGOC_RONG_1_SAO;
                    for (int i = begin; i <= NGOC_RONG_7_SAO; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, i), 1);
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                    sendNotifyShenronAppear();
                    activeShenron(pl, true, SummonDragon.DRAGON_SHENRON);
                    sendBlackGokuhesShenron(pl);
                } else {
                    int timeLeft = (int) ((timeResummonShenron - (System.currentTimeMillis() - lastTimeShenronAppeared)) / 1000);
                    Service.gI().sendThongBao(pl, "Vui lòng đợi " + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(pl, "Chỉ được gọi rồng thần ở ngôi làng trước nhà");
        }
    }

    private void reSummonShenron() {
        activeShenron(playerSummonShenron, true, SummonDragon.DRAGON_SHENRON);
        sendBlackGokuhesShenron(playerSummonShenron);
    }

    private void sendBlackGokuhesShenron(Player pl) {
        byte dragonStar;
        try {
            dragonStar = (byte) pl_dragonStar.get(pl);
            this.shenronStar = dragonStar;
        } catch (Exception e) {
            dragonStar = this.shenronStar;
        }
        switch (dragonStar) {
            case 1:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case 2:
                Service.gI().sendThongBao(pl, "Điều ước rồng 2 sao hiện đang tắt");
                shenronLeave(pl, TIME_UP);
                break;
            case 3:
                Service.gI().sendThongBao(pl, "Điều ước rồng 3 sao hiện đang tắt");
                shenronLeave(pl, TIME_UP);
                break;
        }
    }

    public static boolean isSummonEnabled(byte dragonStar) {
        return dragonStar == 1;
    }

    private void activeShenron(Player pl, boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.TRUM_UOC_RONG, 1);
                msg.writer().writeShort(pl.zone.map.mapId);
                msg.writer().writeShort(pl.zone.map.bgId);
                msg.writer().writeByte(pl.zone.zoneId);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeUTF("null");
                msg.writer().writeShort(pl.location.x);
                msg.writer().writeShort(pl.location.y);
                msg.writer().writeByte(type);
                lastTimeShenronWait = System.currentTimeMillis();
                isShenronAppear = true;
                pl.idMark.setShenronType(-1);
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
        }
    }

    private boolean checkShenronBall(Player pl) {
        byte dragonStar = (byte) this.pl_dragonStar.get(pl);
        if (dragonStar == 1) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_2_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 2 sao");
                return false;
            }
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        } else if (dragonStar == 2) {
            if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_3_SAO)) {
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 3 sao");
                return false;
            }
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_4_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 4 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_5_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 5 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_6_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 6 sao");
            return false;
        }
        if (!InventoryService.gI().isExistItemBag(pl, NGOC_RONG_7_SAO)) {
            Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên ngọc rồng 7 sao");
            return false;
        }
        return true;
    }

    private void sendNotifyShenronAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name + " vừa gọi rồng thần tại "
                    + playerSummonShenron.zone.map.mapName + " khu vực " + playerSummonShenron.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmWish() {
        switch (this.menuShenron) {
            case ConstNpc.SHENRON_1_1:
                switch (this.select) {
                    case 0: //găng tay đang đeo lên 1 cấp
                        Item item = this.playerSummonShenron.inventory.itemsBody.get(2);
                        if (item.isNotNullItem()) {
                            int level = 0;
                            for (ItemOption io : item.itemOptions) {
                                if (io.optionTemplate.id == 72) {
                                    level = io.param;
                                    if (level < 7) {
                                        io.param++;
                                    }
                                    break;
                                }
                            }
                            if (level < 7) {
                                if (level == 0) {
                                    item.itemOptions.add(new ItemOption(72, 1));
                                }
                                for (ItemOption io : item.itemOptions) {
                                    if (io.optionTemplate.id == 0) {
                                        io.param += (io.param * 10 / 100);
                                        break;
                                    }
                                }
                                InventoryService.gI().sendItemBody(playerSummonShenron);
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Găng tay của ngươi đã đạt cấp tối đa");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi hiện tại có đeo găng đâu");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 1: //chí mạng +2%
                        if (this.playerSummonShenron.nPoint.critdragon < 9) {
                            this.playerSummonShenron.nPoint.critdragon += 2;
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Điều ước này đã quá sức với ta, ta sẽ cho ngươi chọn lại");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 2: //thay chiêu 2-3 đệ tử
                        if (playerSummonShenron.pet != null) {
                            if (playerSummonShenron.pet.playerSkill.skills.get(1).skillId != -1) {
                                playerSummonShenron.pet.openSkill2();
                                if (playerSummonShenron.pet.playerSkill.skills.get(2).skillId != -1) {
                                    playerSummonShenron.pet.openSkill3();
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi làm gì có đệ tử?");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                }
                break;
            case ConstNpc.SHENRON_1_2:
                switch (this.select) {
                    case 0: //đẹp trai nhất vũ trụ
                        if (InventoryService.gI().getCountEmptyBag(playerSummonShenron) > 0) {
                            byte gender = this.playerSummonShenron.gender;
                            Item avtVip = ItemService.gI().createNewItem((short) (gender == ConstPlayer.TRAI_DAT ? 227
                                    : gender == ConstPlayer.NAMEC ? 228 : 229));
                            avtVip.itemOptions.add(new ItemOption(97, Util.nextInt(5, 10)));
                            avtVip.itemOptions.add(new ItemOption(77, Util.nextInt(10, 20)));
                            InventoryService.gI().addItemBag(playerSummonShenron, avtVip);
                            InventoryService.gI().sendItemBags(playerSummonShenron);
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Hành trang đã đầy");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                    case 1: //găng tay đệ lên 1 cấp
                        if (this.playerSummonShenron.pet != null) {
                            Item item = this.playerSummonShenron.pet.inventory.itemsBody.get(2);
                            if (item.isNotNullItem()) {
                                int level = 0;
                                for (ItemOption io : item.itemOptions) {
                                    if (io.optionTemplate.id == 72) {
                                        level = io.param;
                                        if (level < 7) {
                                            io.param++;
                                        }
                                        break;
                                    }
                                }
                                if (level < 7) {
                                    if (level == 0) {
                                        item.itemOptions.add(new ItemOption(72, 1));
                                    }
                                    for (ItemOption io : item.itemOptions) {
                                        if (io.optionTemplate.id == 0) {
                                            io.param += (io.param * 10 / 100);
                                            break;
                                        }
                                    }
                                    Service.gI().point(playerSummonShenron);
                                } else {
                                    Service.gI().sendThongBao(playerSummonShenron, "Găng tay của đệ ngươi đã đạt cấp tối đa");
                                    reOpenShenronWishes(playerSummonShenron);
                                    return;
                                }
                            } else {
                                Service.gI().sendThongBao(playerSummonShenron, "Đệ ngươi hiện tại có đeo găng đâu");
                                reOpenShenronWishes(playerSummonShenron);
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(playerSummonShenron, "Ngươi đâu có đệ tử");
                            reOpenShenronWishes(playerSummonShenron);
                            return;
                        }
                        break;
                }
                break;
            case ConstNpc.SHENRON_2:
            case ConstNpc.SHENRON_3:
                Service.gI().sendThongBao(this.playerSummonShenron,
                        "Điều ước rồng " + this.shenronStar + " sao hiện đang tắt");
                return;
        }
        shenronLeave(this.playerSummonShenron, WISHED);
    }

    public void showConfirmShenron(Player pl, int menu, byte select) {
        this.menuShenron = menu;
        this.select = select;
        String wish = null;
        switch (menu) {
            case ConstNpc.SHENRON_1_1:
                if (select >= 0 && select < SHENRON_1_STAR_WISHES_1.length) {
                    wish = SHENRON_1_STAR_WISHES_1[select];
                } else {
                    Service.gI().sendThongBao(pl, "Lựa chọn không hợp lệ.");
                    return;
                }
                break;
            case ConstNpc.SHENRON_1_2:
                if (select >= 0 && select < SHENRON_1_STAR_WISHES_2.length) {
                    wish = SHENRON_1_STAR_WISHES_2[select];
                } else {
                    Service.gI().sendThongBao(pl, "Lựa chọn không hợp lệ.");
                    return;
                }
                break;
            case ConstNpc.SHENRON_1_3:
                if (select >= 0 && select < SHENRON_1_STAR_WISHES_3.length) {
                    wish = SHENRON_1_STAR_WISHES_3[select];
                } else {
                    Service.gI().sendThongBao(pl, "Lựa chọn không hợp lệ.");
                    return;
                }
                break;
            case ConstNpc.SHENRON_2:
                if (select >= 0 && select < SHENRON_2_STARS_BlackGokuHES.length) {
                    wish = SHENRON_2_STARS_BlackGokuHES[select];
                } else {
                    Service.gI().sendThongBao(pl, "Lựa chọn không hợp lệ.");
                    return;
                }
                break;
            case ConstNpc.SHENRON_3:
                if (select >= 0 && select < SHENRON_3_STARS_BlackGokuHES.length) {
                    wish = SHENRON_3_STARS_BlackGokuHES[select];
                } else {
                    Service.gI().sendThongBao(pl, "Lựa chọn không hợp lệ.");
                    return;
                }
                break;
        }
        NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_CONFIRM, "Ngươi có chắc muốn ước?", wish, "Từ chối");
    }

    public void reOpenShenronWishes(Player pl) {
        switch (menuShenron) {
            case ConstNpc.SHENRON_1_1:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                break;
            case ConstNpc.SHENRON_1_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_2, SHENRON_SAY, SHENRON_1_STAR_WISHES_2);
                break;
            case ConstNpc.SHENRON_1_3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_1_3, SHENRON_SAY, SHENRON_1_STAR_WISHES_3);
                break;
            case ConstNpc.SHENRON_2:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_2, SHENRON_SAY, SHENRON_2_STARS_BlackGokuHES);
                break;
            case ConstNpc.SHENRON_3:
                NpcService.gI().createMenuRongThieng(pl, ConstNpc.SHENRON_3, SHENRON_SAY, SHENRON_3_STARS_BlackGokuHES);
                break;
        }
    }

    public void shenronLeave(Player pl, byte type) {
        if (type == WISHED) {
            NpcService.gI().createTutorial(pl, 0, "Điều ước của ngươi đã trở thành sự thật\nHẹn gặp ngươi lần sau, ta đi ngủ đây, bái bai");
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.IGNORE_MENU, "Ta buồn ngủ quá rồi\nHẹn gặp ngươi lần sau, ta đi đây, bái bai");
        }
        activeShenron(pl, false, SummonDragon.DRAGON_SHENRON);
        this.isShenronAppear = false;
        this.menuShenron = -1;
        this.select = -1;
        this.playerSummonShenron = null;
        this.playerSummonShenronId = -1;
        this.shenronStar = -1;
        this.mapShenronAppear = null;
        lastTimeShenronAppeared = System.currentTimeMillis();
    }

}
