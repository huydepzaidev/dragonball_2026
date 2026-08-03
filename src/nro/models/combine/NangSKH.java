package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Nâng đồ Thần Linh thành trang bị kích hoạt tại Bà Hạt Mít. */
public final class NangSKH {

    private static final int RATIO_THUONG = 50;
    private static final int RATIO_VIP = 100;
    private static final long COST_GOLD_THUONG = 500_000_000L;
    private static final long COST_GOLD_VIP = 500_000_000L;

    /* Đồng bộ đúng pool item của Capsule kích hoạt tự chọn (ID 1655). */
    private static final int[][][] ITEM_IDS = {
        {
            {0, 3, 33, 34, 136, 137, 138, 139, 230, 231, 232, 233},
            {1, 4, 41, 42, 152, 153, 154, 155, 235, 236, 237},
            {2, 5, 49, 50, 168, 169, 170, 171, 238, 239, 240, 241}
        },
        {
            {6, 9, 35, 36, 140, 141, 142, 143, 242, 243, 244, 245},
            {7, 10, 43, 44, 156, 157, 158, 159, 246, 247, 248, 249},
            {8, 11, 51, 52, 172, 173, 174, 174, 250, 251, 252, 253}
        },
        {
            {21, 24, 37, 38, 144, 145, 146, 147, 254, 256, 257},
            {22, 25, 45, 46, 160, 161, 162, 163, 259, 260, 261},
            {23, 26, 53, 54, 176, 177, 178, 179, 262, 263, 264, 265}
        },
        {
            {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269},
            {28, 31, 47, 48, 164, 165, 166, 167, 270, 271, 272, 273},
            {29, 32, 55, 56, 180, 181, 182, 183, 274, 275, 276, 277}
        },
        {
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281},
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281},
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281}
        }
    };

    private static final int[][] SKH_OPTIONS = {
        {128, 129, 127, 233, 245},
        {130, 131, 132, 233, 237},
        {133, 135, 134, 233, 241}
    };

    private static final int[][] SKH_VIP_OPTIONS = {
        {233, 245},
        {237},
        {241}
    };

    private NangSKH() {
    }

    public static void showInfoCombine(Player player) {
        int count = player.combineNew.itemsCombine.size();
        boolean isVip = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP;
        boolean isThuong100 = player.combineNew.typeCombine == CombineService.NANG_SKH_THUONG_100;
        int expectedCount = (isVip || isThuong100) ? 2 : 1;
        int ratio = (isVip || isThuong100) ? RATIO_VIP : RATIO_THUONG;
        if (count != expectedCount) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                isVip ? "SKH VIP cần 2 món: món chính đồ Hủy Diệt và món phôi đồ Thần Linh!"
                            : isThuong100 ? "Công thức 100% cần đúng 2 món Thần Linh!"
                                    : "Công thức 50% cần đúng 1 món Thần Linh!", "Đóng");
            return;
        }
        Item main = player.combineNew.itemsCombine.get(0);
        Item phoi = (isVip || isThuong100) ? player.combineNew.itemsCombine.get(1) : null;
        if (isVip ? !isValidVip(main, phoi) : !isValid(main) || (phoi != null && (!isValid(phoi) || phoi == main))) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    isVip ? "Món chính phải là đồ Hủy Diệt, món phôi phải là đồ Thần Linh chưa kích hoạt!"
                            : "Các vật phẩm phải là đồ Thần Linh chưa kích hoạt!", "Đóng");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hành trang cần ít nhất 1 ô trống!", "Đóng");
            return;
        }
        player.combineNew.ratioCombine = ratio;
        long cost = isVip ? COST_GOLD_VIP : COST_GOLD_THUONG;
        String npcSay = "|2|Tỉ lệ thành công: " + ratio + "%\n"
                + "|2|Chi phí: " + Util.numberToMoney(cost) + " vàng\n"
                + "|2|Món chính: 1 " + main.template.name;
        if (phoi != null) {
            npcSay += "\n|2|Phôi: 1 " + phoi.template.name
                    + "\n|2|Kết quả: random trang bị kích hoạt từ cùi nhất đến VIP, theo vị trí món chính";
        } else if (!isVip) {
            npcSay += "\n|2|Kết quả: mẫu đồ cùi nhất cùng vị trí, random set kích hoạt";
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay, "Nâng SKH", "Từ chối");
    }

    public static void thucHien(Player player) {
        int count = player.combineNew.itemsCombine.size();
        boolean isVip = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP;
        boolean isThuong100 = player.combineNew.typeCombine == CombineService.NANG_SKH_THUONG_100;
        int expectedCount = (isVip || isThuong100) ? 2 : 1;
        if (count != expectedCount) {
            Service.gI().sendThongBao(player, isVip
                    ? "SKH VIP cần 2 món: món chính đồ Hủy Diệt và món phôi đồ Thần Linh!"
                    : isThuong100 ? "Công thức 100% cần đúng 2 món Thần Linh!"
                            : "Công thức 50% cần đúng 1 món Thần Linh!");
            return;
        }
        Item main = player.combineNew.itemsCombine.get(0);
        Item phoi = (isVip || isThuong100) ? player.combineNew.itemsCombine.get(1) : null;
        if (isVip ? !isValidVip(main, phoi) : !isValid(main) || (phoi != null && (!isValid(phoi) || phoi == main))) {
            Service.gI().sendThongBao(player, isVip
                    ? "Món chính phải là đồ Hủy Diệt, món phôi phải là đồ Thần Linh chưa kích hoạt!"
                    : "Vật phẩm không hợp lệ để nâng SKH!");
            return;
        }
        long cost = isVip ? COST_GOLD_VIP : COST_GOLD_THUONG;
        if (player.inventory.gold < cost) {
            Service.gI().sendThongBao(player,
                    "Không đủ vàng, cần " + Util.numberToMoney(cost) + " vàng để nâng SKH "
                    + (isVip ? "VIP" : "Thường") + "!");
            return;
        }
        int mainItemId = main.template.id;
        int ratio = (isVip || isThuong100) ? RATIO_VIP : RATIO_THUONG;
        // Cả hai công thức SKH Thường đều dùng mẫu đồ cùi nhất trong shop làng.
        boolean guaranteedLowTier = !isVip;
        player.inventory.gold -= cost;
        Service.gI().sendMoney(player);
        InventoryService.gI().subQuantityItemsBag(player, main, 1);
        if (phoi != null) {
            InventoryService.gI().subQuantityItemsBag(player, phoi, 1);
        }
        if (Util.isTrue(ratio, 100)) {
            Item reward = createReward(player, mainItemId, guaranteedLowTier);
            if (reward != null && InventoryService.gI().addItemBag(player, reward)) {
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Nâng SKH thành công: " + reward.template.name);
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Không thể nhận vật phẩm SKH, hãy kiểm tra hành trang!");
            }
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Nâng SKH thất bại, vật phẩm đã bị tiêu hao!");
        }
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static Item createReward(Player player, int sourceItemId, boolean guaranteedLowTier) {
        int sourcePlanet = getPlanet(sourceItemId);
        int gender = sourcePlanet >= 0 ? sourcePlanet : Math.max(0, Math.min(2, player.gender));
        int slot = getSlot(sourceItemId);
        if (slot < 0) {
            return null;
        }
        int itemId = guaranteedLowTier
                ? ITEM_IDS[slot][gender][0]
                : ITEM_IDS[slot][gender][Util.nextInt(ITEM_IDS[slot][gender].length)];
        int[] options = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP
                ? SKH_VIP_OPTIONS[gender] : SKH_OPTIONS[gender];
        int option = options[Util.nextInt(options.length)];
        return ItemService.gI().createItemSKH(itemId, option);
    }

    private static int getSlot(int itemId) {
        if (itemId == 561 || itemId == 656) {
            return 4;
        }
        if (itemId == 555 || itemId == 557 || itemId == 559
                || itemId == 650 || itemId == 652 || itemId == 654) {
            return 0;
        }
        if (itemId == 556 || itemId == 558 || itemId == 560
                || itemId == 651 || itemId == 653 || itemId == 655) {
            return 1;
        }
        if (itemId == 562 || itemId == 564 || itemId == 566
                || itemId == 657 || itemId == 659 || itemId == 661) {
            return 2;
        }
        if (itemId == 563 || itemId == 565 || itemId == 567
                || itemId == 658 || itemId == 660 || itemId == 662) {
            return 3;
        }
        return -1;
    }

    private static int getPlanet(int itemId) {
        return switch (itemId) {
            case 555, 556, 562, 563, 650, 651, 657, 658 -> 0; // Trái Đất
            case 557, 558, 564, 565, 652, 653, 659, 660 -> 1; // Namếc
            case 559, 560, 566, 567, 654, 655, 661, 662 -> 2; // Xayda
            default -> -1; // Nhẫn Thần Linh dùng chung
        };
    }

    private static boolean isValid(Item item) {
        return item != null && item.template != null && item.isDTL() && !item.isSKH();
    }

    private static boolean isValidVip(Item main, Item phoi) {
        return main != null && main.template != null && main.quantity > 0 && main.isDHD()
                && !main.isSKH() && phoi != null && phoi != main && phoi.template != null
                && phoi.quantity > 0 && phoi.isDTL() && !phoi.isSKH();
    }
}
