package nro.models.combine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Angel equipment crafting at Whis. */
public final class AngelEquipmentCrafting {

    private static final short[][] ANGEL_ITEM_IDS = {
        {1048, 1051, 1054, 1057, 1060},
        {1049, 1052, 1055, 1058, 1061},
        {1050, 1053, 1056, 1059, 1062}
    };

    private AngelEquipmentCrafting() {
    }

    public static void showInfoCombine(Player player) {
        Ingredients ingredients = Ingredients.parse(player);
        if (!ingredients.isValid()) {
            showError(player, ingredients.error);
            return;
        }
        if (player.inventory.gold < AngelCraftingPolicy.GOLD_COST) {
            showError(player, "Không đủ 10 triệu vàng để chế tạo");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            showError(player, "Hành trang phải có ít nhất 1 ô trống");
            return;
        }

        int quality = ingredients.qualityPercent();
        int successRate = ingredients.successRate();
        player.combineNew.goldCombine = AngelCraftingPolicy.GOLD_COST;
        player.combineNew.ratioCombine = successRate;

        String npcSay = "|2|Phẩm chất Thiên Sứ: +" + quality + "% chỉ số\n"
                + "|1|Tỉ lệ thành công: " + successRate + "%\n"
                + "|1|Chi phí: " + Util.numberToMoney(AngelCraftingPolicy.GOLD_COST) + " vàng\n"
                + "|7|Thất bại sẽ mất toàn bộ nguyên liệu";
        CombineService.gI().whis.createOtherMenu(player,
                CombineService.CHE_TAO_TRANG_BI_THIEN_SU,
                npcSay, "Chế tạo", "Từ chối");
    }

    public static void craft(Player player) {
        Ingredients ingredients = Ingredients.parse(player);
        if (!ingredients.isValid()) {
            Service.gI().sendThongBao(player, ingredients.error);
            return;
        }
        if (player.inventory.gold < AngelCraftingPolicy.GOLD_COST) {
            Service.gI().sendThongBao(player, "Không đủ 10 triệu vàng để chế tạo");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang phải có ít nhất 1 ô trống");
            return;
        }

        int quality = ingredients.qualityPercent();
        int successRate = ingredients.successRate();
        int recipeGender = ingredients.recipe.template.gender > 2
                ? player.gender : ingredients.recipe.template.gender;
        int angelItemId = ANGEL_ITEM_IDS[recipeGender][ingredients.fragments.typeIdManh()];
        int luckyStoneId = ingredients.luckyStone.template.id;

        player.inventory.gold -= AngelCraftingPolicy.GOLD_COST;
        consumeIngredients(player, ingredients);

        if (Util.isTrue(successRate, 100)) {
            Item angelItem = ItemService.gI().DoThienSu(angelItemId, recipeGender);
            applyQuality(angelItem, recipeGender, quality);
            addLuckyOptions(angelItem, luckyStoneId);
            if (InventoryService.gI().addItemBag(player, angelItem)) {
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player,
                        "Chế tạo thành công " + angelItem.template.name + " phẩm chất +" + quality + "%");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Không thể thêm trang bị Thiên Sứ vào hành trang");
            }
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player,
                    "Chế tạo thất bại (tỉ lệ " + successRate + "%). Toàn bộ nguyên liệu đã bị tiêu hao");
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

    private static void applyQuality(Item item, int gender, int quality) {
        int optionId;
        int baseValue;
        switch (item.template.id) {
            case 1048, 1049, 1050 -> {
                optionId = 47;
                baseValue = Util.highlightsItem(gender == 2, Util.nextInt(2800, 4000));
            }
            case 1051, 1052, 1053 -> {
                optionId = 22;
                baseValue = Util.highlightsItem(gender == 0, Util.nextInt(120, 130));
            }
            case 1054, 1055, 1056 -> {
                optionId = 0;
                baseValue = Util.highlightsItem(gender == 2, Util.nextInt(10350, 11000));
            }
            case 1057, 1058, 1059 -> {
                optionId = 23;
                baseValue = Util.highlightsItem(gender == 1, Util.nextInt(90, 110));
            }
            case 1060, 1061, 1062 -> {
                optionId = 14;
                baseValue = Util.highlightsItem(gender == 1, Util.nextInt(18, 20));
            }
            default -> throw new IllegalArgumentException("Not an Angel equipment item: " + item.template.id);
        }

        boolean replaced = false;
        for (ItemOption option : item.itemOptions) {
            if (option.optionTemplate.id == optionId) {
                option.param = AngelCraftingPolicy.statWithQuality(baseValue, quality);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            item.itemOptions.add(new ItemOption(optionId,
                    AngelCraftingPolicy.statWithQuality(baseValue, quality)));
        }
    }

    private static void consumeIngredients(Player player, Ingredients ingredients) {
        InventoryService inventory = InventoryService.gI();
        inventory.subQuantityItemsBag(player, ingredients.recipe, 1);
        inventory.subQuantityItemsBag(player, ingredients.upgradeStone, 1);
        inventory.subQuantityItemsBag(player, ingredients.luckyStone, 1);
        inventory.subQuantityItemsBag(player, ingredients.fragments, AngelCraftingPolicy.REQUIRED_FRAGMENTS);
        for (Item sacrifice : ingredients.destroyItems) {
            inventory.subQuantityItemsBag(player, sacrifice, 1);
        }
        for (Item sacrifice : ingredients.divineItems) {
            inventory.subQuantityItemsBag(player, sacrifice, 1);
        }
    }

    private static void addLuckyOptions(Item angelItem, int luckyStoneId) {
        int luckyThreshold = 5 * (luckyStoneId - 1077); // Level 1-5: 10-30 on a 0-50 roll.
        int roll = Util.nextInt(0, 50);
        if (roll > luckyThreshold) {
            return;
        }

        int bonusLines;
        if (roll >= luckyThreshold - 3) {
            bonusLines = 3;
        } else if (roll >= luckyThreshold - 10) {
            bonusLines = 2;
        } else {
            bonusLines = 1;
        }
        angelItem.itemOptions.add(new ItemOption(15, bonusLines));
        List<Integer> optionPool = new ArrayList<>(Arrays.asList(50, 77, 103, 94, 5));
        for (int i = 0; i < bonusLines; i++) {
            int index = Util.nextInt(0, optionPool.size() - 1);
            angelItem.itemOptions.add(new ItemOption(optionPool.remove(index), Util.nextInt(1, 3)));
        }
    }

    private static void showError(Player player, String message) {
        CombineService.gI().whis.createOtherMenu(player, ConstNpc.IGNORE_MENU, message, "Đóng");
    }

    private static final class Ingredients {

        private Item recipe;
        private Item fragments;
        private Item upgradeStone;
        private Item luckyStone;
        private final List<Item> destroyItems = new ArrayList<>();
        private final List<Item> divineItems = new ArrayList<>();
        private String error;

        private static Ingredients parse(Player player) {
            Ingredients result = new Ingredients();
            if (player == null || player.combineNew == null) {
                result.error = "Không tìm thấy dữ liệu chế tạo";
                return result;
            }
            int itemCount = player.combineNew.itemsCombine.size();
            if (itemCount < 4 || itemCount > 7) {
                result.error = "Cần 4 nguyên liệu chính và tối đa 3 trang bị hiến tế";
                return result;
            }

            Set<Item> selectedItems = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Item item : player.combineNew.itemsCombine) {
                if (item == null || !item.isNotNullItem()) {
                    result.error = "Có vật phẩm không hợp lệ trong ô chế tạo";
                    return result;
                }
                if (!selectedItems.add(item)) {
                    result.error = "Không được chọn trùng cùng một vật phẩm";
                    return result;
                }
                if (AngelCraftingPolicy.isVipRecipe(item.template.id)) {
                    if (result.recipe != null) {
                        result.error = "Chỉ được dùng 1 Công thức VIP";
                        return result;
                    }
                    result.recipe = item;
                } else if (item.isManhTS()) {
                    if (result.fragments != null) {
                        result.error = "Chỉ được dùng 1 loại Mảnh Thiên Sứ";
                        return result;
                    }
                    result.fragments = item;
                } else if (item.isDaNangCap1()) {
                    if (result.upgradeStone != null) {
                        result.error = "Chỉ được dùng 1 Đá nâng cấp";
                        return result;
                    }
                    result.upgradeStone = item;
                } else if (item.isDaMayMan()) {
                    if (result.luckyStone != null) {
                        result.error = "Chỉ được dùng 1 Đá may mắn";
                        return result;
                    }
                    result.luckyStone = item;
                } else if (item.isDHD()) {
                    result.destroyItems.add(item);
                } else if (item.isThanLinh()) {
                    result.divineItems.add(item);
                } else {
                    result.error = "Vật phẩm " + item.template.name + " không dùng được trong công thức này";
                    return result;
                }
            }

            if (result.recipe == null) {
                result.error = "Thiếu Công thức VIP";
            } else if (result.fragments == null
                    || result.fragments.quantity < AngelCraftingPolicy.REQUIRED_FRAGMENTS) {
                result.error = "Cần đủ 999 Mảnh Thiên Sứ cùng loại";
            } else if (result.upgradeStone == null) {
                result.error = "Thiếu Đá nâng cấp";
            } else if (result.luckyStone == null) {
                result.error = "Thiếu Đá may mắn";
            } else if (AngelCraftingPolicy.qualityPercent(
                    result.destroyItems.size(), result.divineItems.size()) < 0) {
                result.error = "Tổ hợp hợp lệ: không đồ; 1 Hủy Diệt; 1 Hủy Diệt + 1 Thần Linh; hoặc 2 Hủy Diệt + 1 Thần Linh";
            }
            return result;
        }

        private boolean isValid() {
            return error == null;
        }

        private int qualityPercent() {
            return AngelCraftingPolicy.qualityPercent(destroyItems.size(), divineItems.size());
        }

        private int successRate() {
            return AngelCraftingPolicy.successRate(qualityPercent(), upgradeStone.template.id);
        }
    }
}
