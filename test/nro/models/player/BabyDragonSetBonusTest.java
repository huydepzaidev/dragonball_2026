package nro.models.player;

import nro.models.item.Item;
import nro.models.player_system.Template;

public final class BabyDragonSetBonusTest {

    private BabyDragonSetBonusTest() {
    }

    public static void main(String[] args) {
        Player player = new Player();
        for (int itemId = Inventory.BABY_DRAGON_MIN_ID;
                itemId < Inventory.BABY_DRAGON_MAX_ID; itemId++) {
            player.inventory.itemsBag.add(babyDragon(itemId, -1));
        }
        player.inventory.itemsBox.add(babyDragon(1765, -1));
        player.inventory.itemsBody.add(babyDragon(1771, 30));
        require(!player.inventory.hasFullPermanentBabyDragonSet());

        player.inventory.itemsBody.clear();
        player.inventory.itemsBody.add(babyDragon(1771, -1));
        require(player.inventory.hasFullPermanentBabyDragonSet());
        require(player.inventory.hasPermanentBabyDragon(1765));
        require(player.inventory.hasPermanentBabyDragon(1771));

        player.nPoint.applyPermanentBabyDragonSetBonus();
        require(player.nPoint.tlHp.size() == 1 && player.nPoint.tlHp.get(0) == 1);
        require(player.nPoint.tlMp.size() == 1 && player.nPoint.tlMp.get(0) == 1);
        require(player.nPoint.tlDame.size() == 1 && player.nPoint.tlDame.get(0) == 1);
        require(player.nPoint.tlNeDon == 1);

        Player duplicateOnly = new Player();
        for (int i = 0; i < Inventory.BABY_DRAGON_SET_SIZE; i++) {
            duplicateOnly.inventory.itemsBag.add(babyDragon(1765, -1));
        }
        require(!duplicateOnly.inventory.hasFullPermanentBabyDragonSet());

        Player legacyPermanent = new Player();
        for (int itemId = Inventory.BABY_DRAGON_MIN_ID;
                itemId <= Inventory.BABY_DRAGON_MAX_ID; itemId++) {
            legacyPermanent.inventory.itemsBag.add(babyDragon(itemId, 0));
        }
        require(legacyPermanent.inventory.hasFullPermanentBabyDragonSet());
    }

    private static Item babyDragon(int itemId, int expirationDays) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) itemId;
        item.quantity = 1;
        item.itemOptions.add(new Item.ItemOption(
                new Template.ItemOptionTemplate(30, "Không thể giao dịch", 0), 0));
        if (expirationDays >= 0) {
            item.itemOptions.add(new Item.ItemOption(
                    new Template.ItemOptionTemplate(93, "Hạn sử dụng # ngày", 0),
                    expirationDays));
        }
        return item;
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
