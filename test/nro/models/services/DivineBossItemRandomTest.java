package nro.models.services;

import java.util.HashSet;
import java.util.Set;

public final class DivineBossItemRandomTest {

    private DivineBossItemRandomTest() {
    }

    public static void main(String[] args) {
        Set<Short> ids = new HashSet<>();
        for (int i = 0; i < ItemService.divineBossItemCount(); i++) {
            ids.add(ItemService.divineBossItemIdAt(i));
        }
        if (ids.size() != 13) {
            throw new AssertionError("Divine pool must contain 13 unique items");
        }
        for (short id = 555; id <= 567; id++) {
            if (!ids.contains(id)) {
                throw new AssertionError("Missing divine item " + id);
            }
        }

        int[] counts = new int[16];
        for (int roll = 0; roll < 10000; roll++) {
            int bonus = ItemService.divineBonusPercentForRoll(roll);
            if (bonus < 0 || bonus > 15) {
                throw new AssertionError("Bonus outside 0-15%: " + bonus);
            }
            counts[bonus]++;
        }
        assertCount(counts, 0, 2000);
        for (int bonus = 1; bonus <= 5; bonus++) {
            assertCount(counts, bonus, 900);
        }
        for (int bonus = 6; bonus <= 10; bonus++) {
            assertCount(counts, bonus, 500);
        }
        for (int bonus = 11; bonus <= 14; bonus++) {
            assertCount(counts, bonus, 225);
        }
        assertCount(counts, 15, 100);

        int baseAttack = ItemService.divineGloveBaseAttack((short) 562);
        if (baseAttack != 4500) {
            throw new AssertionError("Divine glove 562 must have 4500 base attack");
        }
        if (baseAttack * (100 + ItemService.divineBonusPercentForRoll(0)) / 100 != 4500
                || baseAttack * (100 + ItemService.divineBonusPercentForRoll(9999)) / 100 != 5175) {
            throw new AssertionError("Invalid 4500 attack scaling at 0%/15%");
        }
        System.out.println("DIVINE_BOSS_ITEM_RANDOM_OK");
    }

    private static void assertCount(int[] counts, int bonus, int expected) {
        if (counts[bonus] != expected) {
            throw new AssertionError("Unexpected weight for " + bonus + "%: " + counts[bonus]);
        }
    }
}
