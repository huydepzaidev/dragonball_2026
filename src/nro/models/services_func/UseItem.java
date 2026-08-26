package nro.models.services_func;

import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.services.shenron.SummonDragon;
import nro.models.combine.CombineService;
import nro.models.boss.BossID;
import nro.models.consts.ConstItem;
import nro.models.radar.Card;
import nro.models.services.RadarService;
import nro.models.radar.RadarCard;
import nro.models.consts.ConstMap;
import nro.models.item.DragonBallFlagRadarPolicy;
import nro.models.item.Item;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.services.shenron.Shenron_Service;
import nro.models.item.Item.ItemOption;
import static java.awt.SystemColor.text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import nro.models.consts.ConstTaskBadges;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Inventory;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.network.Message;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.network.MySession;
import nro.models.npc.MabuEgg;
import nro.models.services.ItemService;
import nro.models.services.ItemTimeService;
import nro.models.services.PetService;
import nro.models.services.RewardService;
import nro.models.services.PlayerService;
import nro.models.services.TaskService;
import nro.models.services.InventoryService;
import nro.models.map.service.MapService;
import nro.models.map.phoban.PotaufeuPolicy;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.map.service.ItemMapService;
import nro.models.npc.DuaHauEgg;
import nro.models.server.Manager;
import static nro.models.server.Manager.isTopSukienChanged;
import nro.models.services.ChatGlobalService;
import nro.models.services.ActivationRewardService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Logger;

/**
 *
 * @author By Mr Blue
 *
 */
public class UseItem {

    private static final int ITEM_BOX_TO_BODY_OR_BAG = 0;
    private static final int ITEM_BAG_TO_BOX = 1;
    private static final int ITEM_BODY_TO_BOX = 3;
    private static final int ITEM_BAG_TO_BODY = 4;
    private static final int ITEM_BODY_TO_BAG = 5;
    private static final int ITEM_BAG_TO_PET_BODY = 6;
    private static final int ITEM_BODY_PET_TO_BAG = 7;
    private static final int ITEM_BAG_TO_RUONG = 8;

    private static final byte DO_USE_ITEM = 0;
    private static final byte DO_THROW_ITEM = 1;
    private static final byte ACCEPT_THROW_ITEM = 2;
    private static final byte ACCEPT_USE_ITEM = 3;

    static final int[] BABY_DRAGON_ITEM_IDS = {1765, 1766, 1767, 1768, 1769, 1770, 1771};
    static final int[] BABY_DRAGON_ITEM_WEIGHTS = {18, 18, 18, 18, 18, 5, 5};
    static final int BABY_DRAGON_WEIGHT_TOTAL = 100;
    static final int BABY_DRAGON_PERMANENT_RATE_PERCENT = 1;
    static final int BABY_DRAGON_MAX_EXPIRATION_DAYS = 30;
    static final int[] NARUTO_COLLAB_REWARD_ITEM_IDS = {2019, 2026, 2027, 2030, 2039};
    static final int NARUTO_COLLAB_PERMANENT_RATE_PERCENT = 5;
    static final int NARUTO_COLLAB_MAX_STAT_RATE_PERCENT = 1;
    static final int NARUTO_COLLAB_HIGH_STAT_ROLL_LIMIT_PERCENT = 15;
    static final int NARUTO_COLLAB_MAX_EXPIRATION_DAYS = 30;

    private static UseItem instance;
    private static final Random rand = new Random();

    private UseItem() {

    }

    public static UseItem gI() {
        if (instance == null) {
            instance = new UseItem();
        }
        return instance;
    }

    public void getItem(MySession session, Message msg) {
        Player player = session.player;
        if (player == null) {
            return;
        }
        TransactionService.gI().cancelTrade(player);
        try {
            int type = msg.reader().readByte();
            int index = msg.reader().readByte();
            if (index == -1) {
                return;
            }
            switch (type) {
                case ITEM_BOX_TO_BODY_OR_BAG:
                    InventoryService.gI().itemBoxToBodyOrBag(player, index);
                    TaskService.gI().checkDoneTaskGetItemBox(player);
                    break;
                case ITEM_BAG_TO_BOX:
                    InventoryService.gI().itemBagToBox(player, index);
                    break;
                case ITEM_BODY_TO_BOX:
                    InventoryService.gI().itemBodyToBox(player, index);
                    break;
                case ITEM_BAG_TO_BODY:
                    InventoryService.gI().itemBagToBody(player, index);
                    break;
                case ITEM_BODY_TO_BAG:
                    InventoryService.gI().itemBodyToBag(player, index);
                    break;
                case ITEM_BAG_TO_PET_BODY:
                    InventoryService.gI().itemBagToPetBody(player, index);
                    break;
                case ITEM_BODY_PET_TO_BAG:
                    InventoryService.gI().itemPetBodyToBag(player, index);
                    break;
            }
            if (player.setClothes != null) {
                player.setClothes.setup();
            }
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            player.setClanMember();
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
            Service.gI().sendSpeedPlayer(player, -1);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);

        }
    }

    public Item finditem(Player player, int iditem) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == iditem) {
                return item;
            }
        }
        return null;
    }

    public void doItem(Player player, Message _msg) {
        TransactionService.gI().cancelTrade(player);
        Message msg = null;
        byte type;
        try {
            type = _msg.reader().readByte();
            int where = _msg.reader().readByte();
            int index = _msg.reader().readByte();
            switch (type) {
                case DO_USE_ITEM -> {
                    if (player != null && player.inventory != null) {
                        if (index != -1) {
                            if (index < 0) {
                                return;
                            }
                            Item item = player.inventory.itemsBag.get(index);
                            if (item.isNotNullItem()) {
                                if (item.template.type == 7) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc chắn học " + player.inventory.itemsBag.get(index).template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    if (!Util.isAfterMidnight(player.lastTimeRewardWoodChest)) {
                                        Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n" + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    if (player.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() > 2) {
                                        Service.gI().sendThongBaoOK(player, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n" + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else {
                                    UseItem.gI().useItem(player, item, index);
                                }
                            }
                        } else {
                            int iditem = _msg.reader().readShort();
                            Item item = finditem(player, iditem);
                            UseItem.gI().useItem(player, item, index);
                        }
                    }
                }
                case DO_THROW_ITEM -> {
                    if (!(player.zone.map.mapId == 21 || player.zone.map.mapId == 22 || player.zone.map.mapId == 23)) {
                        Item item = null;
                        if (index < 0) {
                            return;
                        }
                        if (where == 0) {
                            item = player.inventory.itemsBody.get(index);
                        } else {
                            item = player.inventory.itemsBag.get(index);
                        }

                        if (item.isNotNullItem() && item.template.id == 570) {
                            Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                            return;
                        }
                        if (!item.isNotNullItem()) {
                            return;
                        }
                        msg = new Message(-43);
                        msg.writer().writeByte(type);
                        msg.writer().writeByte(where);
                        msg.writer().writeByte(index);
                        msg.writer().writeUTF("Bạn chắc chắn muốn vứt " + item.template.name + "?");
                        player.sendMessage(msg);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                }
                case ACCEPT_THROW_ITEM -> {
                    InventoryService.gI().throwItem(player, where, index);
                    Service.gI().point(player);
                    InventoryService.gI().sendItemBags(player);
                }
                case ACCEPT_USE_ITEM ->
                    UseItem.gI().useItem(player, player.inventory.itemsBag.get(index), index);
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void useItem(Player pl, Item item, int indexBag) {
        if (item != null && item.isNotNullItem()) {

            if (item.template.id == 570) {
                if (!Util.isAfterMidnight(pl.lastTimeRewardWoodChest)) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openRuongGo(pl);
                }
                return;
            }
            if (item.template.strRequire <= pl.nPoint.power) {
                switch (item.template.type) {
                    case 33: //card
                        UseCard(pl, item);
                        break;
                    case 7: //sách học, nâng skill
                        learnSkill(pl, item);
                        break;
                    case 6: //đậu thần
                        this.eatPea(pl);
                        break;
                    case 12: //ngọc rồng các loại
                        controllerCallRongThan(pl, item);
                        break;
                    case 23: //thú cưỡi mới
                    case 24: //thú cưỡi cũ
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    case 11: //item bag
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFlagBag(pl);
                        break;
                    case 25: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                    }
                    default:
                        switch (item.template.id) {
                            case 2019:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.summonNarutoCollaborationPet(pl);
                                Service.gI().point(pl);
                                break;
                            case ConstItem.RUONG_HOP_TAC_NARUTO:
                                openNarutoCollaborationChest(pl, item);
                                break;
                            case ConstItem.CAPSULE_HONG_NGOC:
                                openRubyCapsule(pl, item);
                                break;
                            case 992: // Nhan thoi khong
                                pl.type = 2;
                                pl.maxTime = 5;
                                Service.gI().Transport(pl);
                                break;
                            case 361:
                                pl.idGo = (short) Util.nextInt(0, 6);
                                NgocRongNamecService.gI().menuCheckTeleNamekBall(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                break;
                            case 892:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 882, 883, 884);
                                Service.gI().point(pl);
                                break;
                            case 893:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 885, 886, 887);
                                Service.gI().point(pl);
                                break;
                            case 908:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 891, 892, 893);
                                Service.gI().point(pl);
                                break;
                            case 909:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 894, 895, 896);
                                Service.gI().point(pl);
                                break;
                            case 910:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 897, 898, 899);
                                Service.gI().point(pl);
                                break;
                            case 916:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 925, 926, 927);
                                Service.gI().point(pl);
                                break;
                            case 917:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 928, 929, 930);
                                Service.gI().point(pl);
                                break;
                            case 918:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 931, 932, 933);
                                Service.gI().point(pl);
                                break;
                            case 919:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 934, 935, 936);
                                Service.gI().point(pl);
                                break;
                            case 936:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 718, 719, 720);
                                Service.gI().point(pl);
                                break;
                            case 942:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 966, 967, 968);
                                Service.gI().point(pl);
                                break;
                            case 943:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 969, 970, 971);
                                Service.gI().point(pl);
                                break;
                            case 944:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 972, 973, 974);
                                Service.gI().point(pl);
                                break;
                            case 967:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1050, 1051, 1052);
                                Service.gI().point(pl);
                                break;
                            case 1008:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1074, 1075, 1076);
                                Service.gI().point(pl);
                                break;
                            case 1039:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1089, 1090, 1091);
                                Service.gI().point(pl);
                                break;
                            case 1040:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1092, 1093, 1094);
                                Service.gI().point(pl);
                                break;
                            case 1046:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, -1, -1, -1);
                                Service.gI().point(pl);
                                break;
                            case 1107:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1155, 1156, 1157);
                                Service.gI().point(pl);
                                break;
                            case 1114:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1158, 1159, 1160);
                                Service.gI().point(pl);
                                break;
                            case 1188:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1183, 1184, 1185);
                                Service.gI().point(pl);
                                break;
                            case 1202:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1201, 1202, 1203);
                                Service.gI().point(pl);
                                break;
                            case 1203:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1201, 1202, 1203);
                                Service.gI().point(pl);
                                break;
                            case 1207:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1077, 1078, 1079);
                                Service.gI().point(pl);
                                break;
                            case 1224:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1227, 1228, 1229);
                                Service.gI().point(pl);
                                break;
                            case 1225:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1233, 1234, 1235);
                                Service.gI().point(pl);
                                break;
                            case 1226:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1230, 1231, 1232);
                                Service.gI().point(pl);
                                break;
                            case 1243:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1245, 1246, 1247);
                                Service.gI().point(pl);
                                break;
                            case 1244:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1248, 1249, 1250);
                                Service.gI().point(pl);
                                break;
                            case 1256:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1267, 1268, 1269);
                                Service.gI().point(pl);
                                break;
                            case 1318:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1299, 1300, 1301);
                                Service.gI().point(pl);
                                break;
                            case 1347:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1302, 1303, 1304);
                                Service.gI().point(pl);
                                break;
                            case 1414:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1341, 1342, 1343);
                                Service.gI().point(pl);
                                break;
                            case 1435:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1347, 1348, 1349);
                                Service.gI().point(pl);
                                break;
                            case 1452:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1365, 1366, 1367);
                                Service.gI().point(pl);
                                break;
                            case 1458:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1368, 1369, 1370);
                                Service.gI().point(pl);
                                break;
                            case 1482:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1398, 1399, 1400);
                                Service.gI().point(pl);
                                break;
                            case 1497:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1401, 1402, 1403);
                                Service.gI().point(pl);
                                break;
                            case 1550:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1428, 1429, 1430);
                                Service.gI().point(pl);
                                break;
                            case 1551:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1425, 1426, 1427);
                                Service.gI().point(pl);
                                break;
                            case 1564:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1437, 1438, 1439);
                                Service.gI().point(pl);
                                break;
                            case 1568:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1443, 1444, 1445);
                                Service.gI().point(pl);
                                break;
                            case 1573:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1446, 1447, 1448);
                                Service.gI().point(pl);
                                break;
                            case 1596:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1473, 1474, 1475);
                                Service.gI().point(pl);
                                break;
                            case 1597:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1473, 1474, 1475);
                                Service.gI().point(pl);
                                break;
                            case 1611:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1488, 1494, 1495);
                                Service.gI().point(pl);
                                break;
                            case 1620:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1496, 1497, 1498);
                                Service.gI().point(pl);
                                break;
                            case 1621:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1496, 1497, 1498);
                                Service.gI().point(pl);
                                break;
                            case 1622:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1488, 1489, 1490);
                                Service.gI().point(pl);
                                break;
                            case 1629:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1505, 1506, 1507);
                                Service.gI().point(pl);
                                break;
                            case 1630:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1508, 1509, 1510);
                                Service.gI().point(pl);
                                break;
                            case 1631:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1513, 1516, 1517);
                                Service.gI().point(pl);
                                break;
                            case 1633:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1523, 1524, 1525);
                                Service.gI().point(pl);
                                break;
                            case 1654:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1526, 1529, 1530);
                                Service.gI().point(pl);
                                break;
                            case 1668:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1550, 1551, 1552);
                                Service.gI().point(pl);
                                break;
                            case 1682:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1558, 1559, 1560);
                                Service.gI().point(pl);
                                break;
                            case 1683:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1561, 1562, 1563);
                                Service.gI().point(pl);
                                break;
                            case 1686:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1572, 1573, 1574);
                                Service.gI().point(pl);
                                break;
                            case 1750:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1464, 1465, 1466);
                                Service.gI().point(pl);
                                break;
                            case 1765:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1662, 1663, 1664);
                                Service.gI().point(pl);
                                break;
                            case 1789:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1724, 1725, 1726);
                                Service.gI().point(pl);
                                break;
                            case 1727:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1616, 1617, 1618);
                                Service.gI().point(pl);
                                break;
                            case 1729:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1621, 1622, 1623);
                                Service.gI().point(pl);
                                break;
                            case 1766:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1665, 1666, 1667);
                                Service.gI().point(pl);
                                break;
                            case 1767:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1668, 1669, 1670);
                                Service.gI().point(pl);
                                break;
                            case 1768:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1671, 1672, 1673);
                                Service.gI().point(pl);
                                break;
                            case 1769:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1674, 1675, 1676);
                                Service.gI().point(pl);
                                break;
                            case 1770:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1677, 1678, 1679);
                                Service.gI().point(pl);
                                break;
                            case 1771:
                                InventoryService.gI().itemBagToBody(pl, indexBag);
                                PetService.Pet2(pl, 1680, 1681, 1682);
                                Service.gI().point(pl);
                                break;
                            case 1655: // capule kich hoat
                                UseItem.gI().CapsuleKichHoat(pl, item);
                                break;
                            case ActivationRewardService.SET_BOX_ID:
                                ActivationRewardService.gI().openSetBox(pl, item);
                                break;
                            case ActivationRewardService.SINGLE_CAPSULE_ID:
                                ActivationRewardService.gI().openSingleCapsule(pl, item);
                                break;
                            case 1787: // capule kich hoat
                                UseItem.gI().MapRiengTu(pl, item);
                                break;
                            case 718: // vé tặng ngọc
                                UseItem.gI().VeTangNgoc(pl, item);
                                break;
                            case 1575: // pháo bômg
                                UseItem.gI().PhaoBong(pl, item);
                                break;
                            case 1576: // pháo bômg
                                UseItem.gI().PhaoBongVip(pl, item);
                                break;
                            case 1609: // kem trai cay
                                UseItem.gI().KemTraiCay(pl, item);
                                break;
                            case 1608:
                                UseItem.gI().QuaThieuNhi(pl, item);
                                break;
                            case DragonBallFlagRadarPolicy.NORMAL_RADAR_ITEM_ID: // rada cờ ngọc rồng
                                UseItem.gI().RadaNgocRong(pl, item.template.id);
                                break;
                            case DragonBallFlagRadarPolicy.VIP_RADAR_ITEM_ID: // rada cờ ngọc rồng VIP
                                UseItem.gI().RadaNgocRongVip(pl, item.template.id);
                                break;
                            case 1569: // kho báu hải tặc
                                UseItem.gI().KhoBauHaiTac(pl, item.template.id);
                                break;
                            case 1798:
                            case 1801:
                            case 1799:
                            case 1800:
                            case 1802:
                                UseItem.gI().ThucAnChoThan(pl, item);
                                break;
                            case 1652: // Sử dụng item Loa Thế Giới
                               try {
                                UseItem.gI().LoaTheGioi(pl, item);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                            case 1776:
                                UseItem.gI().OpenHopQuaThuong(pl, item.template.id);
                                break;
                            case 1777:
                                UseItem.gI().OpenHopQuaVip(pl, item.template.id);
                                break;
                            case 1592:
                                UseItem.gI().OpenHopQuaGokuDay(pl, item.template.id);
                                break;
                            case 1594:
                            case 1591:
                                UseItem.gI().OpenHopQuaGokuDayVangNgoc(pl, item.template.id);
                                break;
                            case 1840:
                                UseItem.gI().OpenHopQuaGokuDayVip(pl, item.template.id);
                                break;
                            case 1757:
                                UseItem.gI().OpenHopQuaCadic(pl, item.template.id);
                                break;
                            case 1821:
                                UseItem.gI().OpenTrungRongNhi(pl, item.template.id);
                                break;
                            case 1635: // co bon la
                                UseItem.gI().useItemTime(pl, item);
                                break;
                            case 1505:
                                Item gm = null;
                                if (item.isNotNullItem()) {
                                    if (item.isGiayMau()) {
                                        gm = item;
                                    }
                                }
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.event3, -1,
                                        "|1|Gói Hộp đựng quà\n"
                                        + "|2|Giấy màu " + gm.quantity + "/99\n"
                                        + "|2|Giá vàng 2.000.000",
                                        "Đồng ý", "Đóng");

                                break;
                            case 1506:
                            case 1507:
                            case 1508:
                            case 1509:
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.event3_1, -1,
                                        "Bạn muốn gói loại nào",
                                        "Gói Hộp quà nhẹ nhàng", "Gói Hộp quà chỉnh chu", "Từ chối");
                                break;
                            case 211: //nho tím
                            case 212: //nho xanh
                                eatGrapes(pl, item);
                                break;
                            case 460:
                                XuongCho(pl, item);
                                break;
                            case 962:
                                C5(pl, item);
                                break;
                            case 963:
                                C7(pl, item);
                                break;
                            case 342:
                            case 343:
                            case 344:
                            case 345:
                                if (pl.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() < 3) {
                                    Service.gI().dropSatellite(pl, item, pl.zone, pl.location.x, pl.location.y);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBaoOK(pl, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                }
                                break;
                            case 380: //cskb
                                openCSKB(pl, item);
                                break;
                            case 1614:
                            case 1615:
                            case 1616:
                            case 381: //cuồng nộ
                            case 382: //bổ huyết
                            case 383: //bổ khí
                            case 384: //giáp xên
                            case 385: //ẩn danh
                            case 379: //máy dò capsule
                            case 638: //commeson                           
                            case 579:
                            case 1045: //đuôi khỉ
                            case 663: //bánh pudding
                            case 664: //xúc xíc
                            case 665: //kem dâu
                            case 666: //mì ly
                            case 667: //sushi
                            case 764: //Khau Trang
                            case 1150:
                            case 1151:
                            case 1152:
                            case 1153:
                            case 1154:
                            case 1233:
                            case 1532:
                            case 1628:
                            case ConstItem.TRAI_DUA:

                                useItemTime(pl, item);
                                break;
                            case ConstItem.CUA_RANG_ME:
                            case ConstItem.BACH_TUOC_NUONG:
                            case ConstItem.TOM_TAM_BOT_CHIEN_XU:
                                useItemTime(pl, item);
                                break;
                            case 521: //tdlt
                                useTDLT(pl, item);
                                break;
                            case 454: //bông tai
                                UseItem.gI().usePorata(pl);
                                break;
                            case 921: //bông tai
                                UseItem.gI().usePorata2(pl);
                                break;
                                case 1819: //bông tai
                                UseItem.gI().usePorata3(pl);
                                break;
                            case 193: //gói 10 viên capsule
                                openCapsuleUI(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            case 194: //capsule đặc biệt
                                openCapsuleUI(pl);
                                break;
                            case 401: //đổi đệ tử
                                changePet(pl, item);
                                break;
                            case 402: //sách nâng chiêu 1 đệ tử
                            case 403: //sách nâng chiêu 2 đệ tử
                            case 404: //sách nâng chiêu 3 đệ tử
                            case 759: //sách nâng chiêu 4 đệ tử
                                upSkillPet(pl, item);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                break;
                            case 727:
                            case 728:
                                UseItem.gI().ItemSieuThanThuy(pl, item);
                                break;
                            case 1537:
                            case 1775:
                                UseItem.gI().OpenHopThanlinh(pl, item);
                                break;
                            case 648:
                                UseItem.gI().NoelItemBox(pl, item);
                                break;
                            case 1171:
                                UseItem.gI().ChuLunBox(pl, item);
                                break;
                            case 1560:
                                if (InventoryService.gI().findItem(pl.inventory.itemsBag, 1561) != null) {
                                    UseItem.gI().RuongNgocRong(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn không có chía khoá vàng!");
                                }
                                break;

                            case 1170:
                                UseItem.gI().BlackGokuItemBoxEventNoel(pl, item);
                                break;
                            case 736:
                                ItemService.gI().OpenItem736(pl, item);
                                break;
                            case 987:
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp"); //đá bảo vệ
                                break;
                            case 568: //quả trứng
                                if (pl.mabuEgg == null) {
                                    MabuEgg.createMabuEgg(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    if (pl.zone.map.mapId == (21 + pl.gender)) {
                                        if (pl.mabuEgg != null) {
                                            pl.mabuEgg.sendMabuEgg();
                                        }
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn đã có quả trứng nên không thể sử dụng");
                                }
                                break;
                            case 2006:
                                Input.gI().createFormChangeNameByItem(pl);
                                break;
                            case ConstItem.DOI_SKILL_2_3_DE_TU: {
                                if (pl.pet == null || pl.pet.playerSkill == null
                                        || pl.pet.playerSkill.skills == null
                                        || pl.pet.playerSkill.skills.size() < 3
                                        || pl.pet.playerSkill.skills.get(1) == null
                                        || pl.pet.playerSkill.skills.get(2) == null
                                        || pl.pet.playerSkill.skills.get(1).skillId == -1
                                        || pl.pet.playerSkill.skills.get(2).skillId == -1) {
                                    Service.gI().sendThongBao(pl, "Đệ tử phải có đủ skill 2 và skill 3 mới có thể sử dụng.");
                                    break;
                                }
                                pl.pet.openSkill2();
                                pl.pet.openSkill3();
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBags(pl);
                                Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                                Service.gI().sendThongBao(pl, "Đã đổi ngẫu nhiên skill 2 và skill 3 của đệ tử.");
                            }
                            break;
                            case 1758: {
                                Player player = pl;
                                if (player.pet != null) {
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                        player.pet.openSkill2();
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                            case 1759: {
                                Player player = pl;
                                if (player.pet != null) {
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                        player.pet.openSkill3();
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                            case 1795: //đổi đệ tử
                                changePetRamdom(pl, item);
                                return;
                            case ConstItem.DOI_DE_TU_2:
                                changeSecondDisciple(pl, item);
                                return;
                            case 1760: {
                                Player player = pl;
                                if (player.pet != null) {
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                        player.pet.openSkill4();
                                    } else {
                                        Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 4 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                            }
                            break;
                        }
                        break;
                }
                TaskService.gI().checkDoneTaskUseItem(pl, item);
                InventoryService.gI().sendItemBags(pl);
            } else {
                Service.gI().sendThongBaoOK(pl, "Sức mạnh không đủ yêu cầu");
            }
        }
    }

    static int rubyCapsuleRewardForRoll(int roll) {
        return roll < 900 ? roll % 50 + 1 : roll % 50 + 51;
    }

    private void openRubyCapsule(Player player, Item capsule) {
        int reward = rubyCapsuleRewardForRoll(Util.nextInt(1000));
        int rubyBefore = player.inventory.ruby;
        int rubyAfter = (int) Math.min(Integer.MAX_VALUE, (long) rubyBefore + reward);
        if (rubyAfter == rubyBefore) {
            return;
        }

        player.inventory.ruby = rubyAfter;
        InventoryService.gI().subQuantityItemsBag(player, capsule, 1);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, new StringBuilder()
                .append('+')
                .append(rubyAfter - rubyBefore)
                .append(' ')
                .append(ItemService.gI().getTemplate((short) 861).name)
                .toString());
    }

    public void openRuongGo(Player player) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE1, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE2, 1);
        Item ruongGo = InventoryService.gI().findItemBag(player, 570);
        if (ruongGo != null) {
            int level = InventoryService.gI().getParam(player, 72, 570);
            int requiredSlots = calculateRequiredEmptySlots(level);
            if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
                Service.gI().sendThongBao(player, "Cần ít nhất " + (requiredSlots - InventoryService.gI().getCountEmptyBag(player)) + " ô trống trong hành trang");
            } else {
                player.itemsWoodChest.clear();
                if (level == 0) {
                    InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                    InventoryService.gI().sendItemBags(player);
                    Item item = ItemService.gI().createNewItem((short) 190);
                    item.quantity = 1;
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBags(player);

                    Service.gI().sendThongBao(player, "reward");
                    return;
                }
                int baseGoldAmount = 100 * level;
                int randomFactor = Util.nextInt(-15, 15);
                int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

                Item itemGold = ItemService.gI().createNewItem((short) 190);
                itemGold.quantity = goldAmount * 1000;
                player.itemsWoodChest.add(itemGold);
                if (level >= 9) {
                    int quantity = 100 + (level - 9) * 20;
                    Item item77 = ItemService.gI().createNewItem((short) 77);
                    item77.quantity = quantity;
                    player.itemsWoodChest.add(item77);
                }

                // Phần thưởng đồ tại rương
                int clothesCount = 1;
                if (level >= 5 && level <= 8) {
                    clothesCount = 2;  // Nếu cấp độ từ 5 đến 8, thưởng 2 món đồ
                } else if (level >= 10 && level <= 12) {
                    clothesCount = 3;  // Nếu cấp độ từ 10 đến 12, thưởng 3 món đồ
                }

                // Tạo đồ thưởng (clothes) và thêm vào phần thưởng
                for (int i = 0; i < clothesCount; i++) {
                    int randItemId = randClothes(level);  // Lấy ID ngẫu nhiên của món đồ
                    Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) randItemId);
                    if (ops != null && !ops.isEmpty()) {
                        rewardItem.itemOptions.addAll(ops);  // Thêm thuộc tính item
                    }
                    rewardItem.quantity = 1;  // Số lượng món đồ là 1
                    player.itemsWoodChest.add(rewardItem);  // Thêm món đồ vào phần thưởng
                }

                // Phần thưởng item ngẫu nhiên (từ rewardItems)
                int[] rewardItems = {17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229};
                int rewardCount = 2;  // Số lượng item mặc định

                // Thay đổi số lượng phần thưởng tùy theo cấp độ
                if (level >= 5 && level <= 8) {
                    rewardCount = 3;  // Nếu cấp độ từ 5 đến 8, thưởng 3 item ngẫu nhiên
                } else if (level >= 10 && level <= 12) {
                    rewardCount = 4;  // Nếu cấp độ từ 10 đến 12, thưởng 4 item ngẫu nhiên
                }

                // Thêm item ngẫu nhiên vào phần thưởng
                Set<Integer> selectedItems = new HashSet<>();
                while (selectedItems.size() < rewardCount) {
                    int randItemId = rewardItems[Util.nextInt(0, rewardItems.length - 1)];
                    if (!selectedItems.contains(randItemId)) {
                        selectedItems.add(randItemId);
                        Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                        rewardItem.quantity = Util.nextInt(1, level);  // Số lượng item phụ thuộc vào cấp độ
                        player.itemsWoodChest.add(rewardItem);  // Thêm item vào phần thưởng
                    }
                }

                // Phần thưởng sao pha lê (nâng cấp)
                int saoPhaLeCount = (level > 9) ? 2 : 1;  // Nếu cấp độ > 9, thêm 2 sao phá lệ
                for (int i = 0; i < saoPhaLeCount; i++) {
                    int rand = Util.nextInt(0, 6);
                    Item level1 = ItemService.gI().createNewItem((short) (441 + rand));
                    level1.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));
                    level1.quantity = Util.nextInt(1, 3);  // Số lượng sao phá lệ
                    player.itemsWoodChest.add(level1);  // Thêm sao phá lệ vào phần thưởng
                }
                int dncCount = (level > 9) ? 2 : 1;
                for (int i = 0; i < dncCount; i++) {
                    int rand = Util.nextInt(0, 4);
                    Item dnc = ItemService.gI().createNewItem((short) (220 + rand));
                    dnc.itemOptions.add(new Item.ItemOption(71 - rand, 0));
                    dnc.quantity = Util.nextInt(1, level * 2);  // Số lượng đá nâng cấp phụ thuộc vào cấp độ
                    player.itemsWoodChest.add(dnc);  // Thêm đá nâng cấp vào phần thưởng            
                }

                // Trừ 1 rương gỗ
                InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                InventoryService.gI().sendItemBags(player);

                // Thêm các phần thưởng vào hành trang
                for (Item it : player.itemsWoodChest) {
                    InventoryService.gI().addItemBag(player, it);
                }
                InventoryService.gI().sendItemBags(player);

                // Cập nhật chỉ số rương gỗ
                player.indexWoodChest = player.itemsWoodChest.size() - 1;
                int i = player.indexWoodChest;
                if (i < 0) {
                    return;
                }
                Item itemWoodChest = player.itemsWoodChest.get(i);
                player.indexWoodChest--;
                String info = "|1|" + itemWoodChest.template.name;
                if (itemWoodChest.quantity > 1) {
                    info += " (x" + itemWoodChest.quantity + ")";
                }

                String info2 = "\n|2|";
                if (!itemWoodChest.itemOptions.isEmpty()) {
                    for (Item.ItemOption io : itemWoodChest.itemOptions) {
                        if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                            info2 += io.getOptionString() + "\n";
                        }
                    }
                }
                info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|" + itemWoodChest.template.description;
                NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n"
                        + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
            }
        }
    }

    public int calculateRequiredEmptySlots(int level) {
        // Khởi tạo số ô trống cần thiết
        int requiredSlots = 0;

        // Tính số lượng vàng
        int baseGoldAmount = 100 * level;
        int randomFactor = Util.nextInt(-15, 15);
        int goldAmount = baseGoldAmount + (baseGoldAmount * randomFactor / 100);

        // Vàng có ID 190, không tính vào số ô trống yêu cầu
        if (goldAmount > 0) {
            requiredSlots++;
        }

        // Tính phần thưởng quần áo
        int clothesCount = 1;
        if (level >= 5 && level <= 8) {
            clothesCount = 2;
        } else if (level >= 10 && level <= 12) {
            clothesCount = 3;
        }
        // Đếm số phần thưởng quần áo
        requiredSlots += clothesCount;

        // Tính phần thưởng item hỗ trợ
        int[] rewardItems = {17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229};
        int rewardCount = 2;

        if (level >= 5 && level <= 8) {
            rewardCount = 3;
        } else if (level >= 10 && level <= 12) {
            rewardCount = 4;
        }
        // Đếm phần thưởng item hỗ trợ
        requiredSlots += rewardCount;

        // Tính sao pha lê (Số lượng 2 nếu level > 9)
        int saoPhaLeCount = (level > 9) ? 2 : 1;
        requiredSlots += saoPhaLeCount;

        // Tính đá nâng cấp (Số lượng 2 nếu level > 9)
        int dncCount = (level > 9) ? 2 : 1;
        requiredSlots += dncCount;

        // Trả về tổng số ô trống cần thiết
        return requiredSlots;
    }
    
    public void RadaNgocRong(Player player, int itemUseiD) {
        openDragonBallFlagRadar(player, itemUseiD, false);
    }
    
    public void RadaNgocRongVip(Player player, int itemUseiD) {
        openDragonBallFlagRadar(player, itemUseiD, true);
    }
    
    private void openDragonBallFlagRadar(Player player, int itemUseId, boolean vip) {
        if (itemUseId != DragonBallFlagRadarPolicy.radarItemId(vip)) {
            Service.gI().sendThongBao(player, "Vật phẩm radar không hợp lệ.");
            return;
        }
        Item used = InventoryService.gI().findItemBag(player, itemUseId);
        if (used == null || used.quantity < 1) {
            Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng.");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        Item reward = ItemService.gI().createNewItem(DragonBallFlagRadarPolicy.rewardId(
                vip, randomInRange(0, Integer.MAX_VALUE - 1)));
        ItemService.gI().initDragonBallFlagOptionsIfEmpty(reward);
        reward.itemOptions.removeIf(o -> o != null && o.optionTemplate != null
                && (o.optionTemplate.id == 231 || o.optionTemplate.id == 93));
        if (!DragonBallFlagRadarPolicy.isPermanent(vip, randomInRange(0, 99))) {
            reward.itemOptions.add(new ItemOption(93, DragonBallFlagRadarPolicy.LIMITED_DURATION_DAYS));
        }
        if (InventoryService.gI().addItemBag(player, reward)) {
            InventoryService.gI().subQuantityItemsBag(player, used, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn nhận được " + reward.template.name
                    + (reward.getOptionById(93) == null ? " vĩnh viễn" : " 15 ngày"));
        }
    }

    public void KhoBauHaiTac(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1569);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {618, 619, 620, 621, 622, 623, 624, 625,626};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(10, 15);
            int hp = randomInRange(10, 15);
            int ki = randomInRange(10, 15);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(210, 2));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            newItem.itemOptions.add(new ItemOption(93, randomOption < 0.5 ? 0 : 15));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        }
    }

    private int randClothes(int level) {
        int result = level - Util.nextInt(2, 4);
        if (result < 1) {
            result = 1;
        }
        return ConstItem.LIST_ITEM_CLOTHES[Util.nextInt(0, 2)][Util.nextInt(0, 4)][result];
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            if (!PetService.gI().canReplacePet(player)) {
                return;
            }
            int gender = player.pet.gender + 1;
            if (gender > 2) {
                gender = 0;
            }
            PetService.gI().changeNormalPet(player, gender);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    private void changePetRamdom(Player player, Item item) {
        if (!validateSecondDiscipleChange(player, item)) {
            return;
        }
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_CONFIRM_CHANGE_SECOND_DISCIPLE, -1,
                "Bạn đã tháo hết trang bị của đệ tử cũ ra chưa?\nKhi đổi, đệ tử cũ sẽ mất.",
                new String[]{"OK", "Từ chối"}, item);
    }

    private void changeSecondDisciple(Player player, Item item) {
        if (!validateSecondDiscipleReroll(player, item)) {
            return;
        }
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_CONFIRM_CHANGE_SECOND_DISCIPLE, -1,
                "Bạn đã tháo hết trang bị của Đệ tử 2 cũ ra chưa?\nKhi đổi, đệ tử cũ sẽ mất.",
                new String[]{"OK", "Từ chối"}, item);
    }

    public void confirmChangeSecondDisciple(Player player, Object pendingItem, int select) {
        if (select != 0) {
            return;
        }
        if (!(pendingItem instanceof Item item)) {
            Service.gI().sendThongBao(player, "Yêu cầu đổi đệ tử đã hết hiệu lực, vui lòng thử lại.");
            return;
        }
        synchronized (player) {
            if (!item.isNotNullItem() || item.template == null) {
                Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ hoặc không còn trong hành trang!");
                return;
            }
            boolean rerollSecondDisciple = item.template.id == ConstItem.DOI_DE_TU_2;
            if (rerollSecondDisciple) {
                if (!validateSecondDiscipleReroll(player, item)) {
                    return;
                }
            } else if (!validateSecondDiscipleChange(player, item)) {
                return;
            }
            byte oldPetType = player.pet.typePet;
            byte newPetType = rerollSecondDisciple
                    ? chooseDifferentSecondDiscipleType(oldPetType, rand.nextInt(2))
                    : (byte) (2 + rand.nextInt(3));
            if (!PetService.gI().replaceWithSecondDisciple(player, newPetType)) {
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            if (oldPetType == 0 && player.mabuEgg != null) {
                player.mabuEgg.destroyEgg();
            }
            TaskService.gI().checkDoneTaskUseItem(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().showInfoPet(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + player.pet.name.substring(1) + "!");
        }
    }

    private boolean validateSecondDiscipleChange(Player player, Item item) {
        if (item == null || !item.isNotNullItem() || item.template.id != 1795
                || !player.inventory.itemsBag.contains(item)) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ hoặc không còn trong hành trang!");
            return false;
        }
        if (!hasRequiredSecondDiscipleKilis(item)) {
            Service.gI().sendThongBao(player, "Không đủ 3.000 Kilis để đổi Đệ tử 2!");
            return false;
        }
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử để đổi!");
            return false;
        }
        if (player.pet.typePet != 0 && player.pet.typePet != 1) {
            Service.gI().sendThongBao(player, "Chỉ có thể dùng đệ thường hoặc đệ Mabư để đổi Đệ tử 2!");
            return false;
        }
        if (player.pet.nPoint.power < 40_000_000_000L) {
            Service.gI().sendThongBao(player, "Đệ tử phải đạt ít nhất 40 tỷ sức mạnh!");
            return false;
        }
        if (player.pet.typePet == 0 && player.mabuEgg == null) {
            Service.gI().sendThongBao(player, "Đệ thường cần có trứng Mabư để đổi Đệ tử 2!");
            return false;
        }
        return PetService.gI().canReplacePet(player);
    }

    private boolean validateSecondDiscipleReroll(Player player, Item item) {
        if (item == null || !item.isNotNullItem() || item.template.id != ConstItem.DOI_DE_TU_2
                || !player.inventory.itemsBag.contains(item)) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ hoặc không còn trong hành trang!");
            return false;
        }
        if (!PetService.isSecondDisciple(player)) {
            Service.gI().sendThongBao(player, "Bạn phải có Đệ tử 2 mới có thể sử dụng vật phẩm này!");
            return false;
        }
        return PetService.gI().canReplacePet(player);
    }

    static byte chooseDifferentSecondDiscipleType(byte currentType, int roll) {
        if (!PetService.isSecondDiscipleType(currentType) || roll < 0 || roll > 1) {
            throw new IllegalArgumentException("Invalid second disciple reroll input");
        }
        return (byte) (2 + ((currentType - 2 + roll + 1) % 3));
    }

    static boolean hasRequiredSecondDiscipleKilis(Item item) {
        return item != null && item.hasOption(250, 3000);
    }

    private void eatGrapes(Player pl, Item item) {
        int percentCurrentStatima = pl.nPoint.stamina * 100 / pl.nPoint.maxStamina;
        if (percentCurrentStatima > 50) {
            Service.gI().sendThongBao(pl, "Thể lực vẫn còn trên 50%");
            return;
        } else if (item.template.id == 211) {
            pl.nPoint.stamina = pl.nPoint.maxStamina;
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 100%");
        } else if (item.template.id == 212) {
            pl.nPoint.stamina += (pl.nPoint.maxStamina * 20 / 100);
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 20%");
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        PlayerService.gI().sendCurrentStamina(pl);
    }

    private void PhaoBong(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = {{5000, 20000}};
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
            pl.inventory.gold = Inventory.LIMIT_GOLD;
        }
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000); // eff này live
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        /*    pl.point_sukien1 += 1;
        if (!Manager.isTopSukien1Changed) {
            Manager.isTopSukien1Changed = true;
        } */
        Item removeItem = InventoryService.gI().findItemBag(pl, 1575);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBags(pl);
    }

    private void PhaoBongVip(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = {{500000, 2000000}};
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
            pl.inventory.gold = Inventory.LIMIT_GOLD;
        }
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000); // eff này live
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);

        /*  pl.point_sukien += 1;
        if (!Manager.isTopSukienChanged) {
            Manager.isTopSukienChanged = true;
        }*/
        Item removeItem = InventoryService.gI().findItemBag(pl, 1576);
        if (removeItem != null) {
            InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        }

        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBags(pl);
    }

    private void KemTraiCay(Player player, Item item) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.XSMAX, 1);
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemused = InventoryService.gI().findItemBag(player, 1609);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1804};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(20, 28);
            int hp = randomInRange(20, 30);
            int ki = randomInRange(20, 30);
            int khongthegiaodich = randomInRange(0, 0);

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));

            int randomOption = (int) (Math.random() * 100);
            newItem.itemOptions.add(new ItemOption(93, randomOption < 0.5 ? 0 : 15));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            player.point_sukien2 += 1;
            if (!Manager.isTopSukien2Changed) {
                Manager.isTopSukien2Changed = true;
            }

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        }
    }

    private void QuaThieuNhi(Player player, Item item) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.XSMAX, 1);
        if (InventoryService.gI().getCountEmptyBag(player) >= 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1608);
            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int[] itemIds = {1807, 1599, 1600, 1601, 1602};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            if (newItem == null) {
                Service.gI().sendThongBao(player, "Không thể tạo vật phẩm. Liên hệ admin.");
                return;
            }

            RewardService.gI().initChiSoItem(newItem);

            int dame = randomInRange(20, 25);
            int hp = randomInRange(20, 25);
            int ki = randomInRange(20, 25);
            int khongthegiaodich = 0;

            newItem.itemOptions.add(new ItemOption(50, dame));
            newItem.itemOptions.add(new ItemOption(77, hp));
            newItem.itemOptions.add(new ItemOption(103, ki));
            newItem.itemOptions.add(new ItemOption(210, 4));
            newItem.itemOptions.add(new ItemOption(30, khongthegiaodich));
            newItem.itemOptions.add(new ItemOption(93, Math.random() < 0.5 ? 0 : 15));

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            player.point_sukien += 1;
            Manager.isTopSukienChanged = true;

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Cần ít nhất 5 ô trống trong túi!");
        }
    }

    private void ThucAnChoThan(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemUsed = InventoryService.gI().findItemBag(player, item.template.id);

            if (itemUsed == null || itemUsed.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            int id = itemUsed.template.id;
            if ((id == 1789 || id == 1799 || id == 1800 || id == 1801 || id == 1802) && itemUsed.quantity >= 99) {

                InventoryService.gI().subQuantityItemsBag(player, itemUsed, 99);

                Item newItem = ItemService.gI().createNewItem((short) 1805);
                InventoryService.gI().addItemBag(player, newItem);

                Service.gI().sendThongBao(player, "Bạn đã nhận được 1 " + newItem.template.name + "!");
            } else {
                Service.gI().sendThongBao(player, "Số lượng vật phẩm không đủ hoặc không đúng loại!");
                return;
            }

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
        } else {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
        }
    }

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short rewardId = MysteryCapsuleRewardPolicy.rewardForRoll(
                    Util.nextInt(0, MysteryCapsuleRewardPolicy.ROLL_BOUND - 1));
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (rewardId == MysteryCapsuleRewardPolicy.GOLD_REWARD) {
                pl.inventory.gold += Util.nextInt(5000, 20000);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }
                PlayerService.gI().sendInfoHpMpMoney(pl);
                icon[1] = 930;
            } else {
                Item it = ItemService.gI().createNewItem(rewardId);
                it.itemOptions.add(new ItemOption(73, 0));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);

            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void useItemTime(Player pl, Item item) {
        switch (item.template.id) {
            case 379: // máy dò capsule
                long addedMayDoTime = pl.itemTime.addMayDoTime(System.currentTimeMillis());
                if (addedMayDoTime <= 0) {
                    Service.gI().sendThongBao(pl, item.template.name);
                    return;
                }
                break;
            case 1635: // co bon la
                pl.itemTime.lastTimeUseCoBonLa = System.currentTimeMillis();
                pl.itemTime.isUseCoBonLa = true;
                Service.gI().sendThongBao(pl, "Cỏ bốn lá tăng 70% tỉ lệ rơi nguyên liệu sự kiện hè trong thời gian sử dụng");
                break;
            case 1614: // nuoc mia 1
                if (pl.itemTime.isUseNuocMia2 || pl.itemTime.isUseNuocMia3) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeUseNuocMia1 = System.currentTimeMillis();
                pl.itemTime.isUseNuocMia1 = true;
                Service.gI().point(pl);
                break;
            case 1615: // nuoc mia 1
                if (pl.itemTime.isUseNuocMia1 || pl.itemTime.isUseNuocMia3) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeUseNuocMia2 = System.currentTimeMillis();
                pl.itemTime.isUseNuocMia2 = true;
                Service.gI().point(pl);
                break;
            case 1616: // nuoc mia 1
                if (pl.itemTime.isUseNuocMia1 || pl.itemTime.isUseNuocMia2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeUseNuocMia3 = System.currentTimeMillis();
                pl.itemTime.isUseNuocMia3 = true;
                Service.gI().point(pl);
                break;
            case 381: // cuồng nộ
                if (pl.itemTime.isUseCuongNo2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeCuongNo = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo = true;
                Service.gI().point(pl);
                break;
            case 382: //bổ huyết
                if (pl.itemTime.isUseBoHuyet2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeBoHuyet = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet = true;
                Service.gI().point(pl);
                break;
            case 383: //bổ khí
                if (pl.itemTime.isUseBoKhi2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeBoKhi = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi = true;
                Service.gI().point(pl);
                break;
            case 384: //giáp xên
                if (pl.itemTime.isUseGiapXen2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sự dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeGiapXen = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen = true;
                Service.gI().point(pl);
                break;
            case 385: // ẩn danh
                if (pl.itemTime.isUseAnDanh2) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                pl.itemTime.lastTimeAnDanh = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh = true;
                break;
            case 764: // Khau trang
                pl.itemTime.lastTimeKhauTrang = System.currentTimeMillis();
                pl.itemTime.isUseKhauTrang = true;
                break;
            case ConstItem.CUONG_NO_2:
            case ConstItem.BO_KHI_2:
            case ConstItem.BO_HUYET_2:
            case ConstItem.GIAP_XEN_BO_HUNG_2:
            case ConstItem.AN_DANH_2:
                if (pl.itemTime.hasActiveNormalCounterpart(item.template.id)) {
                    Service.gI().sendThongBao(pl, "Chỉ có thể sử dụng cùng lúc 1 vật phẩm bổ trợ cùng loại");
                    return;
                }
                long addedSuperTime = pl.itemTime.addSuperItemTime(
                        item.template.id, System.currentTimeMillis());
                if (addedSuperTime <= 0) {
                    Service.gI().sendThongBao(pl,
                            "Thời gian " + item.template.name + " đã đạt tối đa 120 phút");
                    return;
                }
                long remainingSuperMinutes = Math.max(1,
                        pl.itemTime.getRemainingSuperItemTime(item.template.id) / 60_000);
                Service.gI().sendThongBao(pl,
                        item.template.name + " còn " + remainingSuperMinutes + " phút");
                break;

            case 638: //Commeson
                pl.itemTime.lastTimeUseCMS = System.currentTimeMillis();
                pl.itemTime.isUseCMS = true;
                break;
            case 1233: //Nồi cơm điện
                pl.itemTime.lastTimeUseNCD = System.currentTimeMillis();
                pl.itemTime.isUseNCD = true;
                break;
            case 579:
            case 1045: // Đuôi khỉ
                pl.itemTime.lastTimeUseDK = System.currentTimeMillis();
                pl.itemTime.isUseDK = true;
                break;
            case 663: //bánh pudding
            case 664: //xúc xíc
            case 665: //kem dâu
            case 666: //mì ly
            case 667: //sushi
                int previousMealIcon = pl.itemTime.iconMeal;
                long addedMealTime = pl.itemTime.addMealTime(
                        item.template.iconID, System.currentTimeMillis());
                if (addedMealTime <= 0) {
                    Service.gI().sendThongBao(pl,
                            "Thời gian " + item.template.name + " đã đạt tối đa 120 phút");
                    return;
                }
                if (previousMealIcon > 0 && previousMealIcon != item.template.iconID) {
                    ItemTimeService.gI().removeItemTime(pl, previousMealIcon);
                }
                long remainingMealMinutes = Math.max(1,
                        pl.itemTime.getRemainingMealTime() / 60_000);
                Service.gI().sendThongBao(pl,
                        item.template.name + " còn " + remainingMealMinutes + " phút");
                break;
            case ConstItem.CUA_RANG_ME:
            case ConstItem.BACH_TUOC_NUONG:
            case ConstItem.TOM_TAM_BOT_CHIEN_XU:
                long addedMeal2Time = pl.itemTime.addMeal2Time(
                        item.template.iconID, System.currentTimeMillis());
                if (addedMeal2Time < 0) {
                    Service.gI().sendThongBao(pl,
                            "Chỉ có thể sử dụng cùng lúc 1 loại thức ăn hỗ trợ");
                    return;
                }
                if (addedMeal2Time == 0) {
                    Service.gI().sendThongBao(pl,
                            "Thời gian " + item.template.name + " đã đạt tối đa 120 phút");
                    return;
                }
                long remainingMeal2Minutes = Math.max(1,
                        pl.itemTime.getRemainingMeal2Time() / 60_000);
                Service.gI().sendThongBao(pl,
                        item.template.name + " còn " + remainingMeal2Minutes + " phút");
                break;

            case 1532: //máy dò đồ
                pl.itemTime.lastTimeUseKhoBauX2 = System.currentTimeMillis();
                pl.itemTime.isUseKhoBauX2 = true;

//            case 2109: //máy dò đồ
//                pl.itemTime.lastTimeUseMayDo2 = System.currentTimeMillis();
//                pl.itemTime.isUseMayDo2 = true;
                break;
            case 1628: //máy dò đồ
                long remainingBuaSanta = pl.itemTime.addBuaSantaTime(System.currentTimeMillis());
                Service.gI().sendThongBao(pl, "Bùa x2 tn,sm đệ tử còn "
                        + Math.max(1, remainingBuaSanta / 60_000) + " phút");
                break;
            case ConstItem.TRAI_DUA:
                long addedTime = pl.itemTime.addTraiDuaTime(System.currentTimeMillis());
                if (addedTime <= 0) {
                    Service.gI().sendThongBao(pl, "Thời gian sử dụng Trái dừa đã đạt tối đa 500 phút");
                    return;
                }
                Service.gI().sendThongBao(pl, "Trái dừa tăng 70% tỉ lệ rơi nguyên liệu sự kiện hè trong "
                        + (pl.itemTime.getRemainingTraiDuaTime() / 60_000) + " phút");
                break;

        }
        Service.gI().point(pl);
        ItemTimeService.gI().sendAllItemTime(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
    }

    private void controllerCallRongThan(Player pl, Item item) {
        int tempId = item.template.id;
        if (tempId >= SummonDragon.NGOC_RONG_1_SAO && tempId <= SummonDragon.NGOC_RONG_7_SAO) {
            switch (tempId) {
                case SummonDragon.NGOC_RONG_1_SAO:
                case SummonDragon.NGOC_RONG_2_SAO:
                case SummonDragon.NGOC_RONG_3_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) (tempId - 13));
                    break;
                default:
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON,
                            -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= Shenron_Service.NGOC_RONG_1_SAO && tempId <= Shenron_Service.NGOC_RONG_7_SAO) {
            Shenron_Service.gI().openMenuSummonShenron(pl, 0);
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                String[] subName = item.template.name.split("");
                byte level = Byte.parseByte(subName[subName.length - 1]);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 7) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {//Hoc skill moi
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else { // neu chua hoc ma hoc lv cao
                            Skill skillNeed = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " cấp " + skillNeed.point + " trước!");
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            pl.BoughtSkill.add((int) item.template.id);
                            //System.out.println(curSkill.template.name + " - " + curSkill.point);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp " + (curSkill.point + 1) + " trước!");
                        }
                    }
                    InventoryService.gI().sendItemBags(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usePorata2(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion2(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }
    private void usePorata3(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion3(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void openCapsuleUI(Player pl) {
        pl.idMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    public void choseMapCapsule(Player pl, int index) {

        if (pl.idNRNM != -1) {
            Service.gI().sendThongBao(pl, "Không thể mang ngọc rồng này lên Phi thuyền");
            Service.gI().hideWaitDialog(pl);
            return;
        }

        int zoneId = -1;
        if (index > pl.mapCapsule.size() - 1 || index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        Zone zoneChose = pl.mapCapsule.get(index);
        //Kiểm tra số lượng người trong khu

        if (zoneChose.getNumOfPlayers() > 25
                || MapService.gI().isMapBanDoKhoBau(zoneChose.map.mapId)
                || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId)
                || MapService.gI().isMapMaBu(zoneChose.map.mapId)
                || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            if (MapService.gI().isMapBanDoKhoBau(zoneChose.map.mapId)) {
                pl.mapBeforeCapsule = null;
            }
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        boolean leavingTreasureMap = pl.zone != null && pl.zone.map != null
                && MapService.gI().isMapBanDoKhoBau(pl.zone.map.mapId);
        boolean leavingPotaufeu = pl.zone != null && pl.zone.map != null
                && pl.zone.map.mapId == PotaufeuPolicy.MAP_ID;
        if (leavingTreasureMap || leavingPotaufeu) {
            pl.mapBeforeCapsule = null;
        } else if (index != 0 || zoneChose.map.mapId == 21
                || zoneChose.map.mapId == 22
                || zoneChose.map.mapId == 23) {
            pl.mapBeforeCapsule = pl.zone;
        } else {
            zoneId = pl.mapBeforeCapsule != null ? pl.mapBeforeCapsule.zoneId : -1;
            pl.mapBeforeCapsule = null;
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, pl.mapCapsule.get(index).map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (!Util.canDoWithTime(player.lastTimeEatPea, 1000)) {
            return;
        }
        player.lastTimeEatPea = System.currentTimeMillis();
        Item pea = null;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                pea = item;
                break;
            }
        }
        if (pea != null) {
            int hpKiHoiPhuc = 0;
            int lvPea = Integer.parseInt(pea.template.name.substring(13));
            for (Item.ItemOption io : pea.itemOptions) {
                if (io.optionTemplate.id == 2) {
                    hpKiHoiPhuc = io.param * 1000;
                    break;
                }
                if (io.optionTemplate.id == 48) {
                    hpKiHoiPhuc = io.param;
                    break;
                }
            }
            player.nPoint.setHp(player.nPoint.hp + hpKiHoiPhuc);
            player.nPoint.setMp(player.nPoint.mp + hpKiHoiPhuc);
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendInfoPlayerEatPea(player);
            if (player.pet != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
                int statima = 100 * lvPea;
                player.pet.nPoint.stamina += statima;
                if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) {
                    player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
                }
                player.pet.nPoint.setHp(player.pet.nPoint.hp + hpKiHoiPhuc);
                player.pet.nPoint.setMp(player.pet.nPoint.mp + hpKiHoiPhuc);
                Service.gI().sendInfoPlayerEatPea(player.pet);
                Service.gI().chatJustForMe(player, player.pet, "Cám ơn sư phụ");
            }

            InventoryService.gI().subQuantityItemsBag(player, pea, 1);
            InventoryService.gI().sendItemBags(player);
        }
    }

    private void upSkillPet(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        try {
            switch (item.template.id) {
                case 402: //skill 1
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 403: //skill 2
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 404: //skill 3
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 759: //skill 4
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;

            }

        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    public void OpenHopThanlinh(Player player, Item itemused) {
        if (player == null || itemused == null) {
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            int[][] itemsByGenderAndType = {
                {555, 556, 562, 563, 561},
                {557, 558, 564, 565, 561},
                {559, 560, 566, 567, 561}
            };

            List<Item> allPreInitializedItems = new ArrayList<>();

            for (int genderIndex = 0; genderIndex < itemsByGenderAndType.length; genderIndex++) {
                short aoId = (short) itemsByGenderAndType[genderIndex][0];
                short quanId = (short) itemsByGenderAndType[genderIndex][1];
                short gangId = (short) itemsByGenderAndType[genderIndex][2];
                short giayId = (short) itemsByGenderAndType[genderIndex][3];
                short nhanId = (short) itemsByGenderAndType[genderIndex][4];

                Item aotl = ItemService.gI().createNewItem(aoId);
                RewardService.gI().initChiSoItem(aotl);
                aotl.itemOptions.add(new ItemOption(30, 0));
                allPreInitializedItems.add(aotl);

                Item wTl = ItemService.gI().createNewItem(quanId);
                RewardService.gI().initChiSoItem(wTl);
                wTl.itemOptions.add(new ItemOption(30, 0));
                allPreInitializedItems.add(wTl);

                Item gTl = ItemService.gI().createNewItem(gangId);
                RewardService.gI().initChiSoItem(gTl);
                gTl.itemOptions.add(new ItemOption(30, 0));
                allPreInitializedItems.add(gTl);

                Item jayTl = ItemService.gI().createNewItem(giayId);
                RewardService.gI().initChiSoItem(jayTl);
                jayTl.itemOptions.add(new ItemOption(30, 0));
                allPreInitializedItems.add(jayTl);

                Item RdTl = ItemService.gI().createNewItem(nhanId);
                RewardService.gI().initChiSoItem(RdTl);
                RdTl.itemOptions.add(new ItemOption(30, 0));
                allPreInitializedItems.add(RdTl);
            }

            Random random = new Random();
            Item chosenItem = allPreInitializedItems.get(random.nextInt(allPreInitializedItems.size()));
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().addItemBag(player, chosenItem);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn vừa nhận được 1 " + chosenItem.template.name + " Thần linh!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 1 ô trống hành trang");
        }
    }

    public void OpenHopQuaThuong(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1772, 1773};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 10));   // HP
            newItem.itemOptions.add(new ItemOption(77, 10));   // KI
            newItem.itemOptions.add(new ItemOption(103, 10)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(14, 10));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption < 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được item ngẫu nhiên");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenTrungRongNhi(Player player, int itemUseiD) {
        if (player == null || player.inventory == null) {
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }

            short randomItemId = (short) randomBabyDragonItemId(
                    rand.nextInt(BABY_DRAGON_WEIGHT_TOTAL));

            Item newItem = ItemService.gI().createNewItem(randomItemId);

            int permanentRoll = rand.nextInt(100);
            int limitedDayRoll = rand.nextInt(BABY_DRAGON_MAX_EXPIRATION_DAYS);
            applyBabyDragonOptions(newItem, permanentRoll, limitedDayRoll);

            if (InventoryService.gI().addItemBag(player, newItem) == false) {
                Service.gI().sendThongBao(player,
                        "Không thể thêm pet vào hành trang, Trứng vàng chưa bị sử dụng.");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);

            InventoryService.gI().sendItemBags(player);
            Service.gI().point(player);
            int expirationDays = babyDragonExpirationDays(permanentRoll, limitedDayRoll);
            String duration = expirationDays == 0 ? "vĩnh viễn" : expirationDays + " ngày";
            Service.gI().sendThongBao(player,
                    "Bạn đã nhận được " + newItem.template.name + " (" + duration + ")!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 1 ô trống trong hành trang.");
        }
    }

    private void openNarutoCollaborationChest(Player player, Item chest) {
        if (player == null || player.inventory == null || chest == null
                || !chest.isNotNullItem() || chest.quantity < 1) {
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player,
                    "Yêu cầu có ít nhất 1 ô trống trong hành trang.");
            return;
        }

        int rewardId = narutoCollaborationRewardIdForRoll(
                rand.nextInt(NARUTO_COLLAB_REWARD_ITEM_IDS.length));
        Item reward = ItemService.gI().createNewItem((short) rewardId);
        if (reward == null || !reward.isNotNullItem()) {
            Service.gI().sendThongBao(player,
                    "Không thể tạo phần thưởng, rương chưa bị sử dụng.");
            return;
        }
        int permanentRoll = rand.nextInt(100);
        int limitedDayRoll = rand.nextInt(NARUTO_COLLAB_MAX_EXPIRATION_DAYS);
        applyNarutoCollaborationOptions(reward, permanentRoll, limitedDayRoll,
                randomNarutoCollaborationStats(rewardId));
        if (!InventoryService.gI().addItemBag(player, reward)) {
            Service.gI().sendThongBao(player,
                    "Không thể thêm phần thưởng vào hành trang, rương chưa bị sử dụng.");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, chest, 1);
        InventoryService.gI().sendItemBags(player);
        int expirationDays = narutoCollaborationExpirationDays(permanentRoll, limitedDayRoll);
        String duration = expirationDays == 0 ? "vĩnh viễn" : expirationDays + " ngày";
        Service.gI().sendThongBao(player,
                "Bạn đã nhận được " + reward.template.name + " (" + duration + ")!");
    }

    static int narutoCollaborationRewardIdForRoll(int roll) {
        if (roll < 0 || roll >= NARUTO_COLLAB_REWARD_ITEM_IDS.length) {
            throw new IllegalArgumentException("Naruto collaboration chest roll is out of range");
        }
        return NARUTO_COLLAB_REWARD_ITEM_IDS[roll];
    }

    private static int[] randomNarutoCollaborationStats(int itemId) {
        int qualityRoll = rand.nextInt(100);
        return switch (itemId) {
            case 2026 -> new int[]{randomHuntStat(10, 25, qualityRoll),
                randomHuntStat(10, 35, qualityRoll) * 1_000,
                randomHuntStat(5, 15, qualityRoll)};
            case 2027 -> new int[]{randomHuntStat(15, 25, qualityRoll),
                randomHuntStat(5_000, 12_000, qualityRoll),
                randomHuntStat(10, 15, qualityRoll),
                randomHuntStat(10, 20, qualityRoll)};
            default -> new int[0];
        };
    }

    private static int randomHuntStat(int min, int max, int qualityRoll) {
        int[] bounds = narutoCollaborationStatBounds(min, max, qualityRoll);
        return nextIntInclusive(bounds[0], bounds[1]);
    }

    static int[] narutoCollaborationStatBounds(int min, int max, int qualityRoll) {
        if (max - min < 2) {
            throw new IllegalArgumentException("Stat range must contain at least three values");
        }
        if (qualityRoll < 0 || qualityRoll >= 100) {
            throw new IllegalArgumentException("Quality roll must be from 0 to 99");
        }
        if (qualityRoll < NARUTO_COLLAB_MAX_STAT_RATE_PERCENT) {
            return new int[]{max, max};
        }
        int midpoint = min + (max - min) / 2;
        if (qualityRoll < NARUTO_COLLAB_HIGH_STAT_ROLL_LIMIT_PERCENT) {
            return new int[]{midpoint + 1, max - 1};
        }
        return new int[]{min, midpoint};
    }

    private static int nextIntInclusive(int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }

    static void applyNarutoCollaborationOptions(Item item, int permanentRoll,
            int limitedDayRoll, int... randomStats) {
        if (item == null || item.template == null || item.itemOptions == null) {
            throw new IllegalStateException("Missing Naruto collaboration item template");
        }
        item.itemOptions.clear();
        for (int[] option : narutoCollaborationBaseOptions(item.template.id, randomStats)) {
            item.itemOptions.add(new ItemOption(option[0], option[1]));
        }
        int expirationDays = narutoCollaborationExpirationDays(permanentRoll, limitedDayRoll);
        if (expirationDays > 0) {
            item.itemOptions.add(new ItemOption(93, expirationDays));
        }
    }

    static int[][] narutoCollaborationBaseOptions(int itemId, int... randomStats) {
        return switch (itemId) {
            case 2019 -> {
                requireRandomStatCount(itemId, randomStats, 0);
                yield new int[][]{{50, 15}, {77, 15}, {103, 15}, {204, 10}, {14, 7}};
            }
            case 2026 -> {
                requireRandomStatCount(itemId, randomStats, 3);
                requireStatRange("Naruto HP percent", randomStats[0], 10, 25);
                requireStatRange("Naruto HP", randomStats[1], 10_000, 35_000);
                requireStatStep("Naruto HP", randomStats[1], 1_000);
                requireStatRange("Naruto damage reduction", randomStats[2], 5, 15);
                yield new int[][]{{77, randomStats[0]}, {22, randomStats[1] / 1_000},
                    {94, randomStats[2]}};
            }
            case 2027 -> {
                requireRandomStatCount(itemId, randomStats, 4);
                requireStatRange("Sasuke damage percent", randomStats[0], 15, 25);
                requireStatRange("Sasuke attack", randomStats[1], 5_000, 12_000);
                requireStatRange("Sasuke critical", randomStats[2], 10, 15);
                requireStatRange("Sasuke critical damage", randomStats[3], 10, 20);
                yield new int[][]{{50, randomStats[0]}, {0, randomStats[1]},
                    {14, randomStats[2]}, {5, randomStats[3]}};
            }
            case 2030 -> {
                requireRandomStatCount(itemId, randomStats, 0);
                yield new int[][]{{50, 20}, {77, 20}, {103, 20}, {95, 10}, {96, 10}, {14, 15}};
            }
            case 2039 -> {
                requireRandomStatCount(itemId, randomStats, 0);
                yield new int[][]{{50, 22}, {77, 22}, {103, 22}, {101, 55}, {14, 10}};
            }
            default -> throw new IllegalArgumentException("Unknown Naruto collaboration item " + itemId);
        };
    }

    private static void requireRandomStatCount(int itemId, int[] randomStats, int expected) {
        if (randomStats == null || randomStats.length != expected) {
            throw new IllegalArgumentException(
                    "Naruto collaboration item " + itemId + " requires " + expected + " random stats");
        }
    }

    private static void requireStatRange(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be from " + min + " to " + max);
        }
    }

    private static void requireStatStep(String name, int value, int step) {
        if (value % step != 0) {
            throw new IllegalArgumentException(name + " must be divisible by " + step);
        }
    }

    static int narutoCollaborationExpirationDays(int permanentRoll, int limitedDayRoll) {
        if (permanentRoll < 0 || permanentRoll >= 100) {
            throw new IllegalArgumentException("Permanent roll must be from 0 to 99");
        }
        if (limitedDayRoll < 0 || limitedDayRoll >= NARUTO_COLLAB_MAX_EXPIRATION_DAYS) {
            throw new IllegalArgumentException("Limited day roll must be from 0 to 29");
        }
        return permanentRoll < NARUTO_COLLAB_PERMANENT_RATE_PERCENT ? 0 : limitedDayRoll + 1;
    }

    private int randomInRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    static void applyBabyDragonOptions(Item item, int permanentRoll, int limitedDayRoll) {
        if (item == null || item.template == null || item.itemOptions == null) {
            throw new IllegalStateException("Missing baby dragon item template");
        }
        item.itemOptions.clear();
        for (int[] option : babyDragonBaseOptions(item.template.id)) {
            item.itemOptions.add(new ItemOption(option[0], option[1]));
        }
        item.itemOptions.add(new ItemOption(30, 0));
        int expirationDays = babyDragonExpirationDays(permanentRoll, limitedDayRoll);
        if (expirationDays > 0) {
            item.itemOptions.add(new ItemOption(93, expirationDays));
        }
    }

    static int[][] babyDragonBaseOptions(int itemId) {
        return switch (itemId) {
            case 1765 -> new int[][]{{77, 16}, {50, 16}, {103, 16}, {236, 20}};
            case 1766 -> new int[][]{{50, 18}, {5, 7}, {14, 5}};
            case 1767 -> new int[][]{{50, 18}, {94, 5}, {108, 7}};
            case 1768 -> new int[][]{{77, 18}, {5, 7}, {14, 5}};
            case 1769 -> new int[][]{{77, 18}, {94, 5}, {108, 7}};
            case 1770 -> new int[][]{{77, 22}, {50, 22}, {94, 8}, {5, 11}, {14, 8}};
            case 1771 -> new int[][]{{77, 22}, {50, 22}, {94, 8}, {5, 11}, {14, 8}, {106, 0}};
            default -> throw new IllegalArgumentException("Unknown baby dragon item " + itemId);
        };
    }

    static int randomBabyDragonItemId(int roll) {
        if (roll < 0 || roll >= BABY_DRAGON_WEIGHT_TOTAL) {
            throw new IllegalArgumentException("Baby dragon roll must be from 0 to 99");
        }
        int cumulativeWeight = 0;
        for (int i = 0; i < BABY_DRAGON_ITEM_IDS.length; i++) {
            cumulativeWeight += BABY_DRAGON_ITEM_WEIGHTS[i];
            if (roll < cumulativeWeight) {
                return BABY_DRAGON_ITEM_IDS[i];
            }
        }
        throw new IllegalStateException("Invalid baby dragon weight configuration");
    }

    static int babyDragonExpirationDays(int permanentRoll, int limitedDayRoll) {
        if (permanentRoll < 0 || permanentRoll >= 100) {
            throw new IllegalArgumentException("Permanent roll must be from 0 to 99");
        }
        if (limitedDayRoll < 0 || limitedDayRoll >= BABY_DRAGON_MAX_EXPIRATION_DAYS) {
            throw new IllegalArgumentException("Limited day roll must be from 0 to 29");
        }
        return permanentRoll < BABY_DRAGON_PERMANENT_RATE_PERCENT ? 0 : limitedDayRoll + 1;
    }

    public void OpenHopQuaVip(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1761, 1731, 1732};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 25));   // HP
            newItem.itemOptions.add(new ItemOption(77, 25));   // KI
            newItem.itemOptions.add(new ItemOption(103, 25)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(236, 10));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption < 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được item ngẫu nhiên");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaGokuDay(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 25));   // HP
            newItem.itemOptions.add(new ItemOption(77, 25));   // KI
            newItem.itemOptions.add(new ItemOption(103, 25)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption < 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được item ngẫu nhiên");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaGokuDayVangNgoc(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 20));   // HP
            newItem.itemOptions.add(new ItemOption(77, 20));   // KI
            newItem.itemOptions.add(new ItemOption(103, 20)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 2));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption < 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được item ngẫu nhiên");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaGokuDayVip(Player player, int itemUseId) {
        if (InventoryService.gI().getCountEmptyBag(player) >= 5) {
            Item itemUsed = InventoryService.gI().findItemBag(player, itemUseId);

            if (itemUsed == null) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần sử dụng.");
                return;
            }

            int[] itemIds = {1588, 1589, 1595, 1587, 1593, 1590};
            int randomIndex = Util.nextInt(0, itemIds.length - 1);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            // Thêm các option cố định
            newItem.itemOptions.add(new ItemOption(50, 27));   // HP
            newItem.itemOptions.add(new ItemOption(77, 27));   // KI
            newItem.itemOptions.add(new ItemOption(103, 27));  // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));   // Chỉ số ẩn

            int randomOption = Util.nextInt(0, 30);
            if (randomOption < 50) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            // Thêm vào túi và cập nhật
            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemUsed, 1);
            InventoryService.gI().sendItemBags(player);

            Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name + "!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 5 ô trống trong hành trang.");
        }
    }

    public void OpenHopQuaCadic(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 1) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);

            int[] itemIds = {1741, 1742, 1743, 1744, 1745, 1746};
            int randomIndex = (int) (Math.random() * itemIds.length);
            short randomItemId = (short) itemIds[randomIndex];

            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);

            newItem.itemOptions.add(new ItemOption(50, 27));   // HP
            newItem.itemOptions.add(new ItemOption(77, 27));   // KI
            newItem.itemOptions.add(new ItemOption(103, 27)); // Sức đánh
            newItem.itemOptions.add(new ItemOption(210, 4));  // May mắn

            int randomOption = (int) (Math.random() * 100);
            if (randomOption < 0.5) {
                newItem.itemOptions.add(new ItemOption(93, 0));
            } else {
                newItem.itemOptions.add(new ItemOption(93, 3));
            }

            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được item ngẫu nhiên");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có 5 ô trống trong hành trang.");
        }
    }

    private void ItemManhGiay(Player pl, Item item) {
        if (pl.winSTT && !Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            Service.gI().sendThongBao(pl, "Hãy gặp thần mèo Karin để sử dụng");
            return;
        } else if (pl.winSTT && Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            pl.winSTT = false;
            pl.callBossPocolo = false;
            pl.zoneSieuThanhThuy = null;
        }
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564, "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?", "Đồng ý", "Từ chối");
    }

    private void XuongCho(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN && !pl.isDie()) {
                    checkSoi = true;
                }
            }
        }

        if (!checkSoi) {
            Service.gI().sendThongBao(pl, "Không tìm thấy Sói hẹc quyn");
            return;
        }

        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN) {
                    Boss soihecQuyn = (Boss) bossPlayer;
                    if (soihecQuyn != null) {
                        if (((SoiHecQuyn) soihecQuyn).KiemTraNhatXuong()) {
                            Service.gI().sendThongBao(pl, "Sói đã no rồi");
                            continue;
                        } else {
                            ((SoiHecQuyn) soihecQuyn).NhatXuong();
                            Service.gI().chat(soihecQuyn, "Ê, Cục xương ngon quá");
                        }

                        ItemMap itemMap = null;
                        int x = pl.location.x;
                        if (x < 0 || x >= pl.zone.map.mapWidth) {
                            return;
                        }
                        int y = pl.zone.map.yPhysicInTop(x, pl.location.y - 24);
                        itemMap = new ItemMap(pl.zone, 460, 1, x, y, pl.id);
                        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.KE_THAO_TUNG_SOI, 1);
                        itemMap.isPickedUp = true;
                        itemMap.createTime -= 23000;
                        if (itemMap != null) {
                            Service.gI().dropItemMap(pl.zone, itemMap);
                        }

                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        InventoryService.gI().sendItemBags(pl);

                        if (Util.nextInt(4) < 3) { // 75% cơ hội
                            int rand = Util.nextInt(0, 6); // Random từ 0 đến 6
                            short idItem = (short) (rand + 441); // Item 441 + rand
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));

                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        } else {
                            short idItem = 459; // Item 459
                            Item it = ItemService.gI().createNewItem(idItem);
                            it.itemOptions.add(new Item.ItemOption(112, 80));
                            it.itemOptions.add(new Item.ItemOption(93, 90));
                            it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                                InventoryService.gI().addItemBag(pl, it);
                                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                            } else {
                                Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                            }
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                        ((SoiHecQuyn) soihecQuyn).leaveMapNew();
                    }

                }
            }

            InventoryService.gI().sendItemBags(pl);
        }

    }

    public void NoelItemBox(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int spl = Util.nextInt(441, 445);
            int dnc = Util.nextInt(381, 384);
            int nr = Util.nextInt(17, 20);
            int nrBang = Util.nextInt(925, 931);
            int mts = Util.nextInt(1066, 1070);

            if (Util.isTrue(5, 90)) {
                int ruby = Util.nextInt(10, 20);
                pl.inventory.gem += ruby;
                PlayerService.gI().sendInfoHpMpMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().sendItemBags(pl);
                Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + "  Ngọc");
            } else {
                int[] temp = {spl, dnc, nr, nrBang, mts, 533, 380};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);
                /*
                # Item Vặt
                 */
                if (temp[index] >= 441 && temp[index] <= 443) {// sao pha le
                    it.itemOptions.add(new ItemOption(temp[index] - 346, 5));
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 444 && temp[index] <= 445) {
                    it.itemOptions.add(new ItemOption(temp[index] - 346, 3));
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 381 && temp[index] <= 384) { // da nang cap
                    it.quantity = Util.nextInt(1, 5);
                } else if (temp[index] >= 1066 && temp[index] <= 1070) { // da nang cap
                    it.quantity = Util.nextInt(1, 5);

                    /*
                   # Item Cải Trang
                     */
                } else if (temp[index] >= 387 && temp[index] <= 393) { // mu noel do
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(30, 40)));
                    it.itemOptions.add(new ItemOption(80, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    it.itemOptions.add(new ItemOption(199, 0));
                    /*
                    # Item Pet
                  # Nếu Muốn Random Item Thì:  } else if (temp[index] == 936 && temp[index] == 936) {
                     */
                } else if (temp[index] == 936) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(5, 10)));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                    /*
                    # Item Flag
                    # Nếu Muốn Random Item Thì:  } else if (temp[index] == 822 && temp[index] == 822) {
                     */
                } else if (temp[index] == 822) { // cay thong noel
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(10, 20)));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(3, 30)));
                    it.itemOptions.add(new ItemOption(30, 0));
                    it.itemOptions.add(new ItemOption(74, 0));
                    /*
                      # Item VanBay
                      # Nếu Muốn Random Item Thì:  } else if (temp[index] == 746 && temp[index] == 746) {
                     */
                } else if (temp[index] == 746) { // xe truot tuyet
                    it.itemOptions.add(new ItemOption(74, 0));
                    it.itemOptions.add(new ItemOption(30, 0));
                    if (Util.isTrue(99, 100)) {
                        it.itemOptions.add(new ItemOption(93, Util.nextInt(30, 360)));
                    }

                } else if (temp[index] == 821) {
                    it.itemOptions.add(new ItemOption(30, 0));
                } else {
                    it.itemOptions.add(new ItemOption(73, 0));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void HopQuaChinhChu(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int tst = Util.nextInt(637, 642);
            int itc1 = Util.nextInt(1150, 1154);

            {
                int[] temp = {tst, itc1};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);

                /*
                   # Item Cải Trang
                 */
                if (temp[index] == 1503 && temp[index] == 1504) { //oy tiu nun
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));

                } else if (temp[index] == 681) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void HopQuaNheNhang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int tst = Util.nextInt(381, 385);
            int itc1 = Util.nextInt(628, 636);

            {
                int[] temp = {tst, itc1};
                byte index = (byte) Util.nextInt(0, temp.length - 1);
                short[] icon = new short[2];
                icon[0] = item.template.iconID;
                Item it = ItemService.gI().createNewItem((short) temp[index]);

                /*
                   # Item Cải Trang
                 */
                if (temp[index] == 1503 && temp[index] == 1504) { //oy tiu nun
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));

                } else if (temp[index] == 681) { // tuan loc
                    it.itemOptions.add(new ItemOption(50, Util.nextInt(19, 25)));
                    it.itemOptions.add(new ItemOption(77, Util.nextInt(18, 25)));
                    it.itemOptions.add(new ItemOption(103, Util.nextInt(17, 25)));
                    it.itemOptions.add(new ItemOption(47, Util.nextInt(13, 12)));
                    it.itemOptions.add(new ItemOption(106, 0));
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                icon[1] = it.template.iconID;
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBags(pl);
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void BlackGokuItemBoxEventNoel(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int hanhtinh = pl.gender;
            int[] caitrangdietquy = {1087, 1088, 1089, 1090, 1091}; // cải trang diệt wỹ

            int itemBlackGoku = Util.nextInt(0, 3) == 0 ? 746
                    : // 1/3%
                    Util.nextInt(0, 3) == 1 ? (hanhtinh == 0 ? 1155 : hanhtinh == 1 ? 1157 : 1156)
                    : // Cải trang Noel
                    Util.nextInt(0, 3) == 2 ? (hanhtinh == 0 ? 1018 : hanhtinh == 1 ? 1019 : 1020)
                    : // Cải trang Broly
                    caitrangdietquy[Util.nextInt(0, caitrangdietquy.length - 1)]; // Cải Trang Diệt Wỹ

            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            if (itemBlackGoku == 746) { // Xe trượt tuyết
                if (Util.isTrue(1, 100)) { // 1% ra HSD vĩnh viễn
                    it.itemOptions.add(new ItemOption(73, 0)); // Option 73 với giá trị 0: HSD vĩnh viễn
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30))); // 99% ra HSD 7-30 ngày
                }
            } else if (itemBlackGoku == 1155 || itemBlackGoku == 1156 || itemBlackGoku == 1157
                    || itemBlackGoku == 1018 || itemBlackGoku == 1019 || itemBlackGoku == 1020) { // Cải trang Noel hoặc Broly
                it.itemOptions.add(new ItemOption(50, 23));
                it.itemOptions.add(new ItemOption(77, 23));
                it.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra option 93
                } else {
                    it.itemOptions.add(new ItemOption(73, 0)); // 10% ra option 73
                }
            } else if (itemBlackGoku >= 1087 && itemBlackGoku <= 1191) { // Diệt Quỷ
                it.itemOptions.add(new ItemOption(50, 22));
                it.itemOptions.add(new ItemOption(77, 21));
                it.itemOptions.add(new ItemOption(103, 21));
                if (Util.isTrue(95, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra HSD (1-3 ngày)
                } else {
                    it.itemOptions.add(new ItemOption(73, 0)); // 5% không ra HSD
                }
            }

            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // send
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void C5(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[] itemList = {1087, 1088, 1089, 1090, 1091};

            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 16))); //
            it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 17))); // 
            it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 15))); // 
            it.itemOptions.add(new ItemOption(95, Util.nextInt(1, 5))); //
            it.itemOptions.add(new ItemOption(96, Util.nextInt(1, 5))); // 

            int[] options = {94, 97, 108};
            int randomOption = options[Util.nextInt(0, options.length - 1)];
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5)));
            if (Util.isTrue(98, 100)) {
                it.itemOptions.add(new ItemOption(93, 5));
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void C7(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[] itemList = {1087, 1088, 1089, 1090, 1091};

            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 16))); //
            it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 17))); // 
            it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 15))); // 
            it.itemOptions.add(new ItemOption(95, Util.nextInt(1, 5))); //
            it.itemOptions.add(new ItemOption(96, Util.nextInt(1, 5))); // 

            int[] options = {94, 97, 108};
            int randomOption = options[Util.nextInt(0, options.length - 1)];
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5)));
            if (Util.isTrue(100, 100)) {
                it.itemOptions.add(new ItemOption(93, 7));
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void ChuLunBox(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // Mảng vật phẩm không giới hạn hành tinh
            int[] itemList = {1158, 1159, 1160, 1161, 1162, 1163, 1164};

            // Chọn ngẫu nhiên một vật phẩm từ danh sách itemList
            int itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];

            // Tạo vật phẩm mới
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);

            // Thêm các item options cố định
            it.itemOptions.add(new ItemOption(50, 11)); // Option 50 = 11
            it.itemOptions.add(new ItemOption(77, 13)); // Option 77 = 13
            it.itemOptions.add(new ItemOption(103, 13)); // Option 103 = 13        

            int[] options = {94, 97, 108}; // Mảng chứa các option
            int randomOption = options[Util.nextInt(0, options.length - 1)]; // Chọn ngẫu nhiên một option từ mảng
            it.itemOptions.add(new ItemOption(randomOption, Util.nextInt(3, 5))); // Thêm option ngẫu nhiên
            if (Util.isTrue(98, 100)) {
                it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 90% ra HSD (1-3 ngày)
            } else {
                it.itemOptions.add(new ItemOption(73, 0)); // 5% không ra HSD
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    public void NonNoelDo(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int hanhtinh = pl.gender;

            // Chỉ giữ lại cải trang Noel
            int itemBlackGoku = (hanhtinh == 0 ? 387 : hanhtinh == 1 ? 390 : 393);

            // Tạo vật phẩm
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            it.itemOptions.add(new ItemOption(50, Util.nextInt(10, 20)));
            it.itemOptions.add(new ItemOption(80, Util.nextInt(30, 50)));
            it.itemOptions.add(new ItemOption(103, Util.nextInt(10, 20)));
            it.itemOptions.add(new ItemOption(106, 1));

            // 95% ra HSD 1-3 ngày, 5% ra vĩnh viễn
            if (Util.isTrue(95, 100)) {
                it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3))); // 95% ra option 93
            } else {
                it.itemOptions.add(new ItemOption(73, 0)); // 5% ra option 73
            }
            // Cập nhật túi đồ
            InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
            InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
            InventoryService.gI().sendItemBags(pl); // send
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void CapsuleKichHoat(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Hãy chọn một món quà", "Áo", "Quần", "Găng", "Giày", "Rada");
    }

    private void MapRiengTu(Player pl, Item item) {
        ChangeMapService.gI().changeMapBySpaceShip(pl, 164, -1, 870);
    }

    private void VeTangNgoc(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, 900, -1, "Nhập tên người chơi mà bạn muốn tặng ngọc", "Tặng", "Từ chối");
    }

    public void LoaTheGioi(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, 901, -1, "Nhập Nội Dung Muốn Chát", "Ok", "Từ chối");
    }

    public void TuiVang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            pl.inventory.gold += Util.nextInt(100000, 10000000);
            int itemBlackGoku = 190;
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);
            int randomValue = Util.nextInt(3, 333);
            it.itemOptions.add(new ItemOption(1, randomValue));
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().addItemBag(pl, it);
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Happy");
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 5_000_000;
        int n = 0;
        switch (item.template.id) {
            case 727:
                n = 2;
                break;
            case 728:
                n = 10;
                break;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBags(pl);
        if (Util.isTrue(90, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) {
                Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
            }
        }
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id).findFirst().orElse(null);
        if (radarTemplate == null) {
            return;
        }
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) {
                return;
            }
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ " + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
                return;
            }
        }
        Card card = pl.Cards.stream().filter(r -> r.Id == item.template.id).findFirst().orElse(null);
        if (card == null) {
            Card newCard = new Card(item.template.id, (byte) 1, radarTemplate.Max, (byte) -1, radarTemplate.Options);
            pl.Cards.add(newCard);
            RadarService.gI().RadarSetAmount(pl, newCard.Id, newCard.Amount, newCard.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, newCard.Id, newCard.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);
        } else {
            if (card.Level >= 2) {
                Service.gI().sendThongBao(pl, "Thẻ này đã đạt cấp tối đa");
                return;
            }
            card.Amount++;
            if (card.Amount >= card.MaxAmount) {
                card.Amount = 0;
                if (card.Level == -1) {
                    card.Level = 1;
                } else {
                    card.Level++;
                }
                Service.gI().point(pl);
            }
            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBags(pl);
        }
    }

    private void RuongNgocRong(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            // Số ngẫu nhiên từ 0 đến 100 để quyết định tỷ lệ
            int random = Util.nextInt(0, 100);

            int itemBlackGoku;

            // 85% xuất hiện item 20, 19, 18, 17
            if (random < 85) {
                int[] itemList = {20, 19, 18, 17};  // Các item có tỉ lệ xuất hiện 85%
                itemBlackGoku = itemList[Util.nextInt(0, itemList.length - 1)];
            } // 10% xuất hiện item 16
            else if (random < 95) {
                itemBlackGoku = 16;  // Item 16 có tỉ lệ 10%
            } // 5% xuất hiện item 14 hoặc 15
            else {
                itemBlackGoku = Util.nextInt(14, 15);  // Item 14 hoặc 15 có tỉ lệ 5%
            }

            // Tạo vật phẩm mới từ ID đã chọn
            Item it = ItemService.gI().createNewItem((short) itemBlackGoku);

            // Kiểm tra nếu người chơi có item 1561 (chìa khóa)
            Item item1561 = InventoryService.gI().findItem(pl.inventory.itemsBag, 1561);
            if (item1561 != null) {
                // Trừ vật phẩm và thêm vật phẩm mới vào túi đồ
                InventoryService.gI().subQuantityItemsBag(pl, item, 1); // Trừ 1 Item Box
                InventoryService.gI().subQuantityItemsBag(pl, item1561, 1); // Trừ 1 Item Box
                InventoryService.gI().addItemBag(pl, it); // Thêm item vào túi
                InventoryService.gI().sendItemBags(pl); // Gửi cập nhật túi đồ
                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Bạn không có chìa khoá vàng");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

}
