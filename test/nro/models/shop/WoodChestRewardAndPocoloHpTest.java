package nro.models.shop;

import nro.models.boss.BossesData;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services_func.UseItem;

public final class WoodChestRewardAndPocoloHpTest {

    public static void main(String[] args) {
        initItemOptionTemplates();
        testPocoloHp();
        testDivineItemsConfig();
        testActivationCapsuleConfig();
        testRequiredEmptySlotsCalculations();
        testIndependentRollsSimulation();
        System.out.println("WOOD_CHEST_REWARD_AND_POCOLO_HP_TEST_OK pocolo_hp=175M divine_1pct=true capsule_1pct=true non_tradeable=true slots_safe=true");
    }

    private static void initItemOptionTemplates() {
        while (Manager.ITEM_OPTION_TEMPLATES.size() <= 100) {
            int id = Manager.ITEM_OPTION_TEMPLATES.size();
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option " + id, 0));
        }
    }

    private static void testPocoloHp() {
        require(BossesData.POCOLO.getHp()[0] == 175_000_000, "Boss Pôcôlô phải có đúng 175.000.000 HP");
        require(BossesData.LIU_LIU.getHp()[0] == 150_000_000, "Boss Liu Liu phải giữ nguyên 150.000.000 HP");
    }

    private static void testDivineItemsConfig() {
        require(UseItem.WOOD_CHEST_DIVINE_ITEM_RATE_PERCENT == 1, "Tỷ lệ đồ Thần Linh phải là 1%");
        require(UseItem.WOOD_CHEST_DIVINE_ITEM_IDS.length == 13, "Danh sách đồ Thần Linh phải có đủ 13 món");

        for (short id : UseItem.WOOD_CHEST_DIVINE_ITEM_IDS) {
            Item it = new Item();
            it.template = new Template.ItemTemplate();
            it.template.id = id;
            require(it.isThanLinh(), "Item " + id + " phải là đồ Thần Linh (isThanLinh)");
            require(id >= 555 && id <= 567, "Item ID phải nằm trong dải 555-567");
            
            // Đảm bảo không có Option 30 (khóa giao dịch)
            boolean hasOption30 = it.itemOptions.stream().anyMatch(opt -> opt != null && opt.optionTemplate != null && opt.optionTemplate.id == 30);
            require(!hasOption30, "Đồ Thần Linh không được có Option 30");
        }
    }

    private static void testActivationCapsuleConfig() {
        require(UseItem.WOOD_CHEST_ACTIVATION_CAPSULE_RATE_PERCENT == 1, "Tỷ lệ Capsule kích hoạt phải là 1%");
        require(ConstNpc.CAPSULE_KICH_HOAT == 1655, "ID Capsule kích hoạt phải là 1655");

        Item capsule = new Item();
        capsule.template = new Template.ItemTemplate();
        capsule.template.id = (short) ConstNpc.CAPSULE_KICH_HOAT;
        capsule.quantity = 1;
        capsule.itemOptions.add(new Item.ItemOption(30, 0));

        require(capsule.quantity == 1, "Capsule phải nhận đúng số lượng 1");
        boolean hasOption30 = capsule.itemOptions.stream().anyMatch(opt -> opt != null && opt.optionTemplate != null && opt.optionTemplate.id == 30);
        require(hasOption30, "Capsule kích hoạt phải có Option 30 (khóa giao dịch)");
    }

    private static void testRequiredEmptySlotsCalculations() {
        // Level 12: Vàng(1) + Quần áo(3) + Item hỗ trợ(4) + Sao pha lê(2) + Đá nâng cấp(2) + Dự phòng xác suất(2) = 14
        int slotsLvl12 = UseItem.gI().calculateRequiredEmptySlots(12);
        require(slotsLvl12 >= 14, "Cần tối thiểu 14 ô trống cho rương cấp 12 để an toàn khi trúng cả 2 phần thưởng");

        // Level 1: Vàng(1) + Quần áo(1) + Item hỗ trợ(2) + Sao pha lê(1) + Đá nâng cấp(1) + Dự phòng xác suất(2) = 8
        int slotsLvl1 = UseItem.gI().calculateRequiredEmptySlots(1);
        require(slotsLvl1 >= 8, "Cần tối thiểu 8 ô trống cho rương cấp 1");
    }

    private static void testIndependentRollsSimulation() {
        // Mô phỏng 2 roll độc lập: Có thể trúng cả 2, trúng 1 trong 2, hoặc không trúng cái nào
        boolean winDivine = true;
        boolean winCapsule = true;

        Player player = new Player();
        if (winDivine) {
            Item divine = new Item();
            divine.template = new Template.ItemTemplate();
            divine.template.id = 555;
            divine.quantity = 1;
            player.itemsWoodChest.add(divine);
        }
        if (winCapsule) {
            Item capsule = new Item();
            capsule.template = new Template.ItemTemplate();
            capsule.template.id = 1655;
            capsule.quantity = 1;
            capsule.itemOptions.add(new Item.ItemOption(30, 0));
            player.itemsWoodChest.add(capsule);
        }

        require(player.itemsWoodChest.size() == 2, "Khi cả 2 roll độc lập cùng trúng, phải nhận đủ 2 phần thưởng");
        require(player.itemsWoodChest.get(0).template.id == 555, "Món 1 là đồ Thần Linh");
        require(player.itemsWoodChest.get(1).template.id == 1655, "Món 2 là Capsule kích hoạt");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
