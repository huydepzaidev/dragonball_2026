package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 */
public class EpSaoTrangBi {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBi = null;
            Item daPhaLe = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }
            int star = 0;
            int starEmpty = 0;
            if (trangBi != null && daPhaLe != null) {
                for (ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id == 102) {
                        star = io.param;
                    } else if (io.optionTemplate.id == 107) {
                        starEmpty = io.param;
                    }
                }

                if (starEmpty > star && starEmpty <= CombineService.MAX_STAR_ITEM) {
                    player.combineNew.gemCombine = CombineSystem.getGemEpSao(star);
                    int optionId = CombineSystem.getOptionDaPhaLe(daPhaLe);
                    int param = CombineSystem.getParamDaPhaLe(daPhaLe);
                    if (optionId < 0 || param <= 0) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Sao pha lê hoặc ngọc rồng không hợp lệ", "Đóng");
                        return;
                    }

                    String npcSay = trangBi.template.name + "\n|2|";
                    for (ItemOption io : trangBi.itemOptions) {
                        if (io.optionTemplate.id != 102) {
                            npcSay += io.getOptionString() + "\n";
                        }
                    }

                    npcSay += "|7|" + ItemService.gI().getItemOptionTemplate(optionId).name
                            .replaceAll("#", param + "") + "\n";
                    npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\ncần " + player.combineNew.gemCombine + " ngọc");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào, và lỗ sao tối đa là 9", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
        }
    }

    public static void epSaoTrangBi(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBi = null;
            Item daPhaLe = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }

            if (trangBi == null || daPhaLe == null || daPhaLe.quantity < 1) {
                Service.gI().sendThongBao(player, "Cần trang bị, sao pha lê thường hoặc ngọc rồng hợp lệ");
                return;
            }

            int star = 0;
            int starEmpty = 0;
            ItemOption optionStar = null;

            for (ItemOption io : trangBi.itemOptions) {
                if (io.optionTemplate.id == 102) {
                    star = io.param;
                    optionStar = io;
                } else if (io.optionTemplate.id == 107) {
                    starEmpty = io.param;
                }
            }

            if (starEmpty < 1 || starEmpty > CombineService.MAX_STAR_ITEM || star >= starEmpty) {
                Service.gI().sendThongBao(player, "Không thể ép sao cao hơn lỗ");
                return;
            }

            int optionId = CombineSystem.getOptionDaPhaLe(daPhaLe);
            int param = CombineSystem.getParamDaPhaLe(daPhaLe);
            if (optionId < 0 || param <= 0) {
                Service.gI().sendThongBao(player, "Sao pha lê hoặc ngọc rồng không hợp lệ");
                return;
            }

            int gem = CombineSystem.getGemEpSao(star);
            if (gem <= 0 || player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                return;
            }
            player.combineNew.gemCombine = gem;
            player.inventory.subGem(gem);

            boolean merged = false;
            for (ItemOption io : trangBi.itemOptions) {
                if (io.optionTemplate.id == optionId) {
                    io.param += param;
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                trangBi.itemOptions.add(new ItemOption(optionId, param));
            }

            if (optionStar != null) {
                optionStar.param += 1;
            } else {
                trangBi.itemOptions.add(new ItemOption(102, 1));
            }

            InventoryService.gI().subQuantityItemsBag(player, daPhaLe, 1);
            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }

}
