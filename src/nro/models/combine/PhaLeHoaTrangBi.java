package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 */
public class PhaLeHoaTrangBi {

    // Ti le thanh cong de tang dung mot sao o moi cap hien tai.
    private static final int[] STAR_RATES = {25, 20, 15, 10, 8, 7, 5, 5, 5};

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hãy chọn 1 vật phẩm để pha lê hóa", "Đóng");
            return;
        }

        Item item = player.combineNew.itemsCombine.get(0);
        if (!CombineSystem.isTrangBiPhaLeHoa(item)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Vật phẩm này không thể đục lỗ", "Đóng");
            return;
        }

        int star = 0;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 107) {
                star = io.param;
            }
        }
        if (star >= CombineService.MAX_STAR_ITEM) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Vật phẩm đã đạt tối đa sao pha lê", "Đóng");
            return;
        }

        QuyTrinh(player, item, star);
    }

    private static void QuyTrinh(Player player, Item item, int star) {
        player.combineNew.goldCombine = CombineSystem.getGoldPhaLeHoa(star);
        player.combineNew.gemCombine = CombineSystem.getGemPhaLeHoa(star);
        player.combineNew.ratioCombine = (int) getRatio(star);

        String npcSay = item.template.name + "\n|2|";
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id != 102) {
                npcSay += io.getOptionString() + "\n";
            }
        }
        npcSay += "|7|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%" + "\n";

        if (player.combineNew.goldCombine <= player.inventory.gold) {
            npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Nâng cấp\ncần " + player.combineNew.gemCombine + " ngọc", "Nâng cấp 10 lần", "Nâng cấp 100 lần");
        } else {
            npcSay += "Còn thiếu " + Util.numberToMoney(player.combineNew.goldCombine - player.inventory.gold) + " vàng";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        }
    }

    public static float getRatio(int star) {
        if (star < 0 || star >= STAR_RATES.length) {
            return 0;
        }

        int ratio = 0;
        for (int targetStar = star + 1; targetStar <= STAR_RATES.length; targetStar++) {
            ratio += STAR_RATES[targetStar - 1];
        }
        return ratio;
    }

    public static int getNextStarOnSuccess(int currentStar) {
        if (currentStar < 0 || currentStar >= CombineService.MAX_STAR_ITEM) {
            return currentStar;
        }
        return currentStar + 1;
    }

    public static void phaLeHoa(Player player, int... numm) {
        if (player.idMark != null && !Util.canDoWithTime(player.idMark.getLastTimeCombine(), 500)) {
            return;
        }
        player.idMark.setLastTimeCombine(System.currentTimeMillis());
        int n = numm.length > 0 ? numm[0] : 1;

        if (!player.combineNew.itemsCombine.isEmpty()) {
            int gold = player.combineNew.goldCombine;
            int gem = player.combineNew.gemCombine;
            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            } else if (player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                return;
            }

            int star = 0;
            boolean success = false;
            Item item = null;
            Item.ItemOption optionStar = null;

            for (int i = 0; i < n; i++) {
                gold = player.combineNew.goldCombine;
                gem = player.combineNew.gemCombine;
                if (player.inventory.gem < gem || player.inventory.gold < gold) {
                    break;
                }

                item = player.combineNew.itemsCombine.get(0);
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    star = 0;
                    optionStar = null;
                    for (Item.ItemOption io : item.itemOptions) {
                        if (io.optionTemplate.id == 107) {
                            star = io.param;
                            optionStar = io;
                            break;
                        }
                    }
                    if (star < CombineService.MAX_STAR_ITEM) {
                        player.combineNew.goldCombine = CombineSystem.getGoldPhaLeHoa(star);
                        player.combineNew.gemCombine = CombineSystem.getGemPhaLeHoa(star);

                        player.inventory.gold -= gold;
                        player.inventory.gem -= gem;

                        if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                            star = getNextStarOnSuccess(star);
                            success = true;
                            break;
                        }
                    }
                } else {
                    break;
                }
            }

            if (success) {
                if (item != null) {
                    if (optionStar == null) {
                        item.itemOptions.add(new Item.ItemOption(107, star));
                    } else {
                        optionStar.param = star;
                    }
                    //  ChatGlobalService.gI().ThongBaoDapDo(player, "Chúc mừng " + player.name + " vừa pha lê hóa thành công " + item.template.name + " lên " + star + " sao pha lê");
                }
                CombineService.gI().sendEffectSuccessCombine(player);
                CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
            } else {
                CombineService.gI().sendEffectFailCombine(player);

                String[] failMessages = {
                    "Tay run à, đập kiểu gì thế?",
                    "Lại xịt rồi, hahaha...",
                    "Ngon bắt được con lợn béo rồi...!",
                    "Làm lại đi, biết đâu lần sau đỏ!",
                    "Lần sau nhớ khấn trước khi đập!",
                    "Kỹ năng quá kém?",
                    "Hên xui thôi mà, đừng cay!",
                    "Còn vàng còn ngọc, đập tiếp đi!"
                };
                String msg = failMessages[Util.nextInt(failMessages.length)];
                CombineService.gI().baHatMit.npcChat(player, msg);
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}
