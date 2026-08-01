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

    /* Các mẫu trang bị kích hoạt theo giới tính/hành tinh, chia theo vị trí đồ. */
    private static final int[][][] ITEM_IDS = {
        {
            {0, 3, 33, 34, 136, 137, 138, 139, 230, 231, 232, 233},
            {1, 4, 41, 42, 152, 153, 154, 155, 234, 235, 236, 237},
            {2, 5, 49, 50, 168, 169, 170, 171, 238, 239, 240, 241}
        },
        {
            {6, 9, 35, 36, 140, 141, 142, 143, 242, 243, 244, 245},
            {7, 10, 43, 44, 156, 157, 158, 159, 246, 247, 248, 249},
            {8, 11, 51, 52, 172, 173, 174, 175, 250, 251, 252, 253}
        },
        {
            {21, 24, 37, 38, 144, 145, 146, 147, 254, 256, 257},
            {22, 25, 45, 46, 160, 161, 162, 163, 258, 259, 260, 261},
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
        // SKH Thường: 1 món = 50%; 2 món = món đầu chính, món sau làm phôi
        // và 100%. Cả hai đều ra mẫu trang bị thấp nhất cùng vị trí.
        int ratio = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP
                ? RATIO_VIP : RATIO_THUONG;
        int count = player.combineNew.itemsCombine.size();
        boolean isVip = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP;
        if ((isVip && count != 1) || (!isVip && (count < 1 || count > 2))) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    isVip ? "SKH VIP cần đúng 1 món đồ Thần Linh!"
                            : "SKH Thường cần 1 món chính hoặc 1 món chính và 1 món phôi!", "Đóng");
            return;
        }
        Item main = player.combineNew.itemsCombine.get(0);
        Item phoi = count == 2 ? player.combineNew.itemsCombine.get(1) : null;
        if (!isValid(main) || main.quantity < 1
                || (phoi != null && (!isValid(phoi) || phoi.quantity < 1 || phoi == main))) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Các vật phẩm phải là đồ Thần Linh chưa kích hoạt!", "Đóng");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hành trang cần ít nhất 1 ô trống!", "Đóng");
            return;
        }
        if (count == 2) {
            ratio = 100;
        }
        player.combineNew.ratioCombine = ratio;
        String npcSay = "|2|Tỉ lệ thành công: " + ratio + "%\n"
                + "|2|Món chính: 1 " + main.template.name;
        if (phoi != null) {
            npcSay += "\n|2|Phôi: 1 " + phoi.template.name
                    + "\n|2|Kết quả: trang bị kích hoạt cấp thấp nhất cùng vị trí món chính";
        } else if (!isVip) {
            npcSay += "\n|2|Kết quả: mẫu đồ cùi nhất cùng vị trí, random set kích hoạt";
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                npcSay, "Nâng SKH", "Từ chối");
    }

    public static void thucHien(Player player) {
        int count = player.combineNew.itemsCombine.size();
        boolean isVip = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP;
        if ((isVip && count != 1) || (!isVip && (count < 1 || count > 2))) {
            Service.gI().sendThongBao(player, isVip
                    ? "SKH VIP cần đúng 1 món đồ Thần Linh!"
                    : "SKH Thường cần 1 món chính hoặc 1 món chính và 1 món phôi!");
            return;
        }
        Item main = player.combineNew.itemsCombine.get(0);
        Item phoi = count == 2 ? player.combineNew.itemsCombine.get(1) : null;
        if (!isValid(main) || main.quantity < 1
                || (phoi != null && (!isValid(phoi) || phoi.quantity < 1 || phoi == main))) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ để nâng SKH!");
            return;
        }
        int ratio = player.combineNew.typeCombine == CombineService.NANG_SKH_VIP
                ? RATIO_VIP : RATIO_THUONG;
        // Cả hai công thức SKH Thường đều dùng mẫu đồ cùi nhất trong shop làng.
        // Phôi chỉ thay đổi tỷ lệ thành công (1 món: 50%, 2 món: 100%).
        boolean guaranteedLowTier = player.combineNew.typeCombine == CombineService.NANG_SKH_THUONG;
        if (phoi != null) {
            ratio = 100;
        }
        InventoryService.gI().subQuantityItemsBag(player, main, 1);
        if (phoi != null) {
            InventoryService.gI().subQuantityItemsBag(player, phoi, 1);
        }
        if (Util.isTrue(ratio, 100)) {
            Item reward = createReward(player, main, guaranteedLowTier);
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

    private static Item createReward(Player player, Item source, boolean guaranteedLowTier) {
        int gender = Math.max(0, Math.min(2, player.gender));
        int slot = getSlot(source.template.id);
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
        if (itemId == 561) {
            return 4;
        }
        if (itemId == 555 || itemId == 557 || itemId == 559) {
            return 0;
        }
        if (itemId == 556 || itemId == 558 || itemId == 560) {
            return 1;
        }
        if (itemId == 562 || itemId == 564 || itemId == 566) {
            return 2;
        }
        if (itemId == 563 || itemId == 565 || itemId == 567) {
            return 3;
        }
        return -1;
    }

    private static boolean isValid(Item item) {
        return item != null && item.template != null && item.isDTL() && !item.isSKH();
    }
}
