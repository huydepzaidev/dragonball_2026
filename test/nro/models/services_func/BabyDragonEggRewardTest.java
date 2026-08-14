package nro.models.services_func;

import java.util.Arrays;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;

public final class BabyDragonEggRewardTest {

    private BabyDragonEggRewardTest() {
    }

    public static void main(String[] args) {
        requireOptions(1765, new int[][]{{77, 16}, {50, 16}, {103, 16}, {236, 20}});
        requireOptions(1766, new int[][]{{50, 18}, {5, 7}, {14, 5}});
        requireOptions(1767, new int[][]{{50, 18}, {94, 5}, {108, 7}});
        requireOptions(1768, new int[][]{{77, 18}, {5, 7}, {14, 5}});
        requireOptions(1769, new int[][]{{77, 18}, {94, 5}, {108, 7}});
        requireOptions(1770, new int[][]{{77, 22}, {50, 22}, {94, 8}, {5, 11}, {14, 8}});
        requireOptions(1771,
                new int[][]{{77, 22}, {50, 22}, {94, 8}, {5, 11}, {14, 8}, {106, 0}});

        int permanentResults = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (UseItem.babyDragonExpirationDays(roll, 0) == 0) {
                permanentResults++;
            }
        }
        require(permanentResults == 1);
        require(UseItem.babyDragonExpirationDays(1, 0) == 1);
        require(UseItem.babyDragonExpirationDays(99, 29) == 30);
        verifyPetRarity();
        verifyGeneratedOptions();
    }

    private static void verifyPetRarity() {
        int[] counts = new int[UseItem.BABY_DRAGON_ITEM_IDS.length];
        for (int roll = 0; roll < UseItem.BABY_DRAGON_WEIGHT_TOTAL; roll++) {
            int itemId = UseItem.randomBabyDragonItemId(roll);
            counts[itemId - UseItem.BABY_DRAGON_ITEM_IDS[0]]++;
        }
        require(Arrays.equals(counts, UseItem.BABY_DRAGON_ITEM_WEIGHTS));
        require(counts[5] == 5); // Rồng nhí 2 sao
        require(counts[6] == 5); // Rồng nhí 1 sao
    }

    private static void verifyGeneratedOptions() {
        Manager.ITEM_OPTION_TEMPLATES.clear();
        for (int optionId = 0; optionId <= 254; optionId++) {
            Manager.ITEM_OPTION_TEMPLATES.add(
                    new Template.ItemOptionTemplate(optionId, "option " + optionId, 0));
        }

        Item permanent = babyDragonItem(1765);
        UseItem.applyBabyDragonOptions(permanent, 0, 29);
        require(permanent.getOptionById(30) != null);
        require(permanent.getOptionById(93) == null);

        Item limited = babyDragonItem(1771);
        UseItem.applyBabyDragonOptions(limited, 99, 29);
        require(limited.getOptionById(30) != null);
        require(limited.getOptionById(93) != null);
        require(limited.getOptionById(93).param == 30);
        require(limited.getOptionById(106) != null);
    }

    private static Item babyDragonItem(int itemId) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) itemId;
        item.quantity = 1;
        return item;
    }

    private static void requireOptions(int itemId, int[][] expected) {
        int[][] actual = UseItem.babyDragonBaseOptions(itemId);
        require(Arrays.deepEquals(actual, expected));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
