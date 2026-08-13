package nro.models.services;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;

public final class BillDestroyShopRequirementTest {

    private BillDestroyShopRequirementTest() {
    }

    public static void main(String[] args) {
        testRequiresFiveRealDivineItems();
        testRequiresOneFoodStackOfNinetyNine();
        testFindsDivineItemInMatchingBodySlot();
        testPurchaseConsumptionBlocksNextPurchase();
        testDestroyEquipmentPowerRequirement();
        System.out.println("BILL_DESTROY_SHOP_REQUIREMENT_TEST_OK");
    }

    private static void testRequiresFiveRealDivineItems() {
        Player player = createEligiblePlayer();
        check(InventoryService.gI().fullSetThan(player), "Five divine items must pass");

        player.inventory.itemsBody.set(0, createItem(0, 0, 1));
        check(!InventoryService.gI().fullSetThan(player),
                "A level-13 non-divine item must not count as divine equipment");
    }

    private static void testRequiresOneFoodStackOfNinetyNine() {
        Player player = createEligiblePlayer();
        Item food = player.inventory.itemsBag.get(0);

        food.quantity = 98;
        check(InventoryService.gI().findBillFood(player) == null,
                "A food stack below 99 must fail");

        food.quantity = 99;
        check(InventoryService.gI().findBillFood(player) == food,
                "A valid stack of 99 food must be selected");
    }

    private static void testFindsDivineItemInMatchingBodySlot() {
        Player player = createEligiblePlayer();
        for (int slot = 0; slot < 5; slot++) {
            check(InventoryService.gI().findEquippedDivineItem(player, slot)
                    == player.inventory.itemsBody.get(slot),
                    "The divine sacrifice must come from body slot " + slot);
        }
        check(InventoryService.gI().findEquippedDivineItem(player, 5) == null,
                "Only the five destroy-equipment slots are valid");
    }

    private static void testPurchaseConsumptionBlocksNextPurchase() {
        Player player = createEligiblePlayer();
        check(InventoryService.gI().canOpenBillShop(player),
                "Eligible player must pass before purchasing");

        Item divineGloves = InventoryService.gI().findEquippedDivineItem(player, 2);
        InventoryService.gI().subQuantityItemsBody(player, divineGloves, 1);

        check(!InventoryService.gI().fullSetThan(player),
                "Losing one divine item must break the full set");
        check(!InventoryService.gI().canOpenBillShop(player),
                "The next purchase must be blocked until five divine items are equipped again");
    }

    private static void testDestroyEquipmentPowerRequirement() {
        Item destroyItem = createItem(657, 2, 14);
        check(InventoryService.getPowerRequirement(destroyItem) == 50_000_000_000L, null);

        Item.ItemOption explicitRequirement = new Item.ItemOption();
        explicitRequirement.optionTemplate = new Template.ItemOptionTemplate(21, null, 0);
        explicitRequirement.param = 25;
        destroyItem.itemOptions.add(explicitRequirement);
        check(InventoryService.getPowerRequirement(destroyItem) == 50_000_000_000L, null);
        InventoryService.normalizeDestroyItemPowerRequirement(destroyItem);
        check(explicitRequirement.param == 50, null);

        Item normalItem = createItem(1, 0, 1);
        normalItem.template.strRequire = 1_500_000;
        check(InventoryService.getPowerRequirement(normalItem) == 1_500_000L, null);
    }

    private static Player createEligiblePlayer() {
        Player player = new Player();
        player.inventory.itemsBody.add(createItem(555, 0, 13));
        player.inventory.itemsBody.add(createItem(556, 1, 13));
        player.inventory.itemsBody.add(createItem(562, 2, 13));
        player.inventory.itemsBody.add(createItem(563, 3, 13));
        player.inventory.itemsBody.add(createItem(561, 4, 13));
        player.inventory.itemsBag.add(createItem(663, 29, 14, 99));
        return player;
    }

    private static Item createItem(int id, int type, int level) {
        return createItem(id, type, level, 1);
    }

    private static Item createItem(int id, int type, int level, int quantity) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) id;
        item.template.type = (byte) type;
        item.template.level = (byte) level;
        item.quantity = quantity;
        return item;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
