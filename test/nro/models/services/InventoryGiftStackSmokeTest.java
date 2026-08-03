package nro.models.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.player_system.Template;

public final class InventoryGiftStackSmokeTest {

    private static final long DAY = 86_400_000L;
    private static final long BASE_TIME = 1_000L * DAY + 3_600_000L;

    private InventoryGiftStackSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        InventoryService inventory = InventoryService.gI();

        List<Item> bag = emptyBag(6);
        Item first = giftBox((short) 1608, "Hộp quà thiếu nhi", BASE_TIME, 30);
        bag.set(0, first);
        Item sameBatch = giftBox((short) 1608, "Hộp quà thiếu nhi",
                BASE_TIME + 300_000L, 30);
        if (!inventory.addItemList(bag, sameBatch)
                || first.quantity != 2
                || sameBatch.quantity != 0) {
            throw new AssertionError("Repeated gift-box purchases must stack");
        }

        Item nextBatch = giftBox((short) 1608, "Hộp quà thiếu nhi",
                BASE_TIME + DAY, 30);
        if (!inventory.addItemList(bag, nextBatch)
                || first.quantity != 3
                || bag.get(1).isNotNullItem()) {
            throw new AssertionError("Every children's gift box must use one stack");
        }

        List<Item> oldSplitStacks = emptyBag(6);
        oldSplitStacks.set(0, giftBox((short) 1608, "Hộp quà thiếu nhi", BASE_TIME, 30));
        oldSplitStacks.set(1, giftBox((short) 1608, "Hộp quà thiếu nhi",
                BASE_TIME + 300_000L, 30));
        oldSplitStacks.set(2, giftBox((short) 1608, "Hộp quà thiếu nhi",
                BASE_TIME + DAY, 30));
        Method merge = InventoryService.class
                .getDeclaredMethod("mergeDuplicateStackableItems", List.class);
        merge.setAccessible(true);
        merge.invoke(inventory, oldSplitStacks);
        if (oldSplitStacks.get(0).quantity != 3
                || oldSplitStacks.get(1).isNotNullItem()
                || oldSplitStacks.get(2).isNotNullItem()) {
            throw new AssertionError("Old split stacks were not merged safely");
        }

        List<Item> cadicBag = emptyBag(3);
        Item cadicFirst = giftBox((short) 1757, "Hộp quà Cađíc VIP", BASE_TIME, 0);
        Item cadicSecond = giftBox((short) 1757, "Hộp quà Cađíc VIP",
                BASE_TIME + 10 * DAY, 0);
        if (!inventory.addItemList(cadicBag, cadicFirst)
                || !inventory.addItemList(cadicBag, cadicSecond)
                || cadicBag.get(0).quantity != 2
                || cadicBag.get(1).isNotNullItem()) {
            throw new AssertionError("Cadic VIP permanent-box stacking changed unexpectedly");
        }

        List<Item> charmBag = emptyBag(25);
        for (int i = 0; i < 20; i++) {
            if (!inventory.addItemList(charmBag, discipleCharm())) {
                throw new AssertionError("Disciple charm purchase failed at " + i);
            }
        }
        if (charmBag.get(0).quantity != 20 || charmBag.get(1).isNotNullItem()) {
            throw new AssertionError("Twenty disciple charms must use one stack");
        }

        System.out.println("INVENTORY_GIFT_STACK_SMOKE_TEST_OK");
    }

    private static List<Item> emptyBag(int size) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(new Item());
        }
        return items;
    }

    private static Item giftBox(short id, String name, long createTime, int expirationDays) {
        Template.ItemTemplate template = new Template.ItemTemplate(
                id, (byte) 27, (byte) 3, name, "", (short) 0, (short) -1,
                id == 1757, 0);
        Item item = new Item();
        item.template = template;
        item.quantity = 1;
        item.createTime = createTime;
        item.itemOptions.add(new Item.ItemOption(
                new Template.ItemOptionTemplate(30, "Không thể giao dịch", 0), 0));
        item.itemOptions.add(new Item.ItemOption(
                new Template.ItemOptionTemplate(93, "Hạn sử dụng", 0), expirationDays));
        return item;
    }

    private static Item discipleCharm() {
        Template.ItemTemplate template = new Template.ItemTemplate(
                (short) 1628, (byte) 29, (byte) 3, "Bùa x2 tn,sm đệ tử", "",
                (short) 0, (short) -1, false, 0);
        Item item = new Item();
        item.template = template;
        item.quantity = 1;
        item.itemOptions.add(new Item.ItemOption(
                new Template.ItemOptionTemplate(73, "Sức mạnh yêu cầu", 0), 0));
        return item;
    }
}
