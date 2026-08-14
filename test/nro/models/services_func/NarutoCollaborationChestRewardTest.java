package nro.models.services_func;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import nro.models.item.Item;
import nro.models.player.NPoint;
import nro.models.player_system.Template;
import nro.models.server.Manager;

public final class NarutoCollaborationChestRewardTest {

    private NarutoCollaborationChestRewardTest() {
    }

    public static void main(String[] args) {
        int[] expected = {2019, 2026, 2027, 2030, 2039};
        int[] actual = new int[expected.length];
        Set<Integer> uniqueRewards = new HashSet<>();

        for (int roll = 0; roll < actual.length; roll++) {
            actual[roll] = UseItem.narutoCollaborationRewardIdForRoll(roll);
            uniqueRewards.add(actual[roll]);
        }

        require(Arrays.equals(actual, expected));
        require(uniqueRewards.size() == expected.length);
        requireThrows(-1);
        requireThrows(expected.length);
        verifyBaseOptions();
        verifyExpiration();
        verifyQualityDistribution();
        verifyGeneratedOptions();
        verifyBossDamageBonus();
    }

    private static void verifyBaseOptions() {
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2019),
                new int[][]{{50, 15}, {77, 15}, {103, 15}, {204, 10}, {14, 7}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2026, 10, 10_000, 5),
                new int[][]{{77, 10}, {22, 10}, {94, 5}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2026, 25, 35_000, 15),
                new int[][]{{77, 25}, {22, 35}, {94, 15}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2027, 15, 5_000, 10, 10),
                new int[][]{{50, 15}, {0, 5_000}, {14, 10}, {5, 10}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2027, 25, 12_000, 15, 20),
                new int[][]{{50, 25}, {0, 12_000}, {14, 15}, {5, 20}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2030),
                new int[][]{{50, 20}, {77, 20}, {103, 20}, {95, 10}, {96, 10}, {14, 15}}));
        require(Arrays.deepEquals(UseItem.narutoCollaborationBaseOptions(2039),
                new int[][]{{50, 22}, {77, 22}, {103, 22}, {101, 55}, {14, 10}}));
    }

    private static void verifyExpiration() {
        int permanentResults = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (UseItem.narutoCollaborationExpirationDays(roll, 0) == 0) {
                permanentResults++;
            }
        }
        require(permanentResults == 5);
        require(UseItem.narutoCollaborationExpirationDays(4, 0) == 0);
        require(UseItem.narutoCollaborationExpirationDays(5, 0) == 1);
        require(UseItem.narutoCollaborationExpirationDays(99, 29) == 30);
    }

    private static void verifyQualityDistribution() {
        require(Arrays.equals(UseItem.narutoCollaborationStatBounds(10, 25, 0),
                new int[]{25, 25}));
        require(Arrays.equals(UseItem.narutoCollaborationStatBounds(10, 25, 1),
                new int[]{18, 24}));
        require(Arrays.equals(UseItem.narutoCollaborationStatBounds(10, 25, 14),
                new int[]{18, 24}));
        require(Arrays.equals(UseItem.narutoCollaborationStatBounds(10, 25, 15),
                new int[]{10, 17}));
        require(Arrays.equals(UseItem.narutoCollaborationStatBounds(10, 25, 99),
                new int[]{10, 17}));
    }

    private static void verifyGeneratedOptions() {
        Manager.ITEM_OPTION_TEMPLATES.clear();
        for (int optionId = 0; optionId <= 254; optionId++) {
            Manager.ITEM_OPTION_TEMPLATES.add(
                    new Template.ItemOptionTemplate(optionId, "option " + optionId, 0));
        }

        Item permanent = collaborationItem(2039);
        UseItem.applyNarutoCollaborationOptions(permanent, 0, 29);
        require(permanent.getOptionById(101).param == 55);
        require(permanent.getOptionById(93) == null);

        Item limited = collaborationItem(2027);
        UseItem.applyNarutoCollaborationOptions(limited, 99, 29, 25, 12_000, 15, 20);
        require(limited.getOptionById(0).param == 12_000);
        require(limited.getOptionById(93).param == 30);

        Item naruto = collaborationItem(2026);
        UseItem.applyNarutoCollaborationOptions(naruto, 0, 0, 25, 35_000, 15);
        require(naruto.getOptionById(6) == null);
        require(naruto.getOptionById(22).param == 35);
    }

    private static Item collaborationItem(int itemId) {
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.id = (short) itemId;
        item.quantity = 1;
        return item;
    }

    private static void verifyBossDamageBonus() {
        NPoint point = new NPoint(null);
        point.tlDameAttBoss.add(10);
        require(point.applyBossDamageBonuses(1_000) == 1_100);
        require(point.applyBossDamageBonuses(Integer.MAX_VALUE) == Integer.MAX_VALUE);
    }

    private static void requireThrows(int roll) {
        try {
            UseItem.narutoCollaborationRewardIdForRoll(roll);
            throw new AssertionError();
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
