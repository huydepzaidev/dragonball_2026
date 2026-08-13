package nro.models.player;

import java.util.Arrays;
import java.util.Collections;
import nro.models.map.phoban.TreasureMapPolicy;

public final class ExperienceStackingPolicyTest {

    private ExperienceStackingPolicyTest() {
    }

    public static void main(String[] args) {
        long base = 1_000L;

        eq(1_500L, NPoint.applyExperiencePercentBonuses(base, Arrays.asList(20, 30)));
        eq(2_000L, NPoint.addExperienceMultiplierBonus(base, base, 2));
        eq(3_000L, NPoint.addExperienceMultiplierBonus(base, base, 3));
        eq(4_000L, NPoint.addExperienceMultiplierBonus(base, base, 4));

        long allWisdomCharms = base;
        allWisdomCharms = NPoint.addExperienceMultiplierBonus(allWisdomCharms, base, 2);
        allWisdomCharms = NPoint.addExperienceMultiplierBonus(allWisdomCharms, base, 3);
        allWisdomCharms = NPoint.addExperienceMultiplierBonus(allWisdomCharms, base, 4);
        eq(7_000L, allWisdomCharms);
        eq(14_000L, TreasureMapPolicy.multiplyMapExperience(allWisdomCharms));

        long equipmentBonus = NPoint.applyExperiencePercentBonuses(base, Arrays.asList(20, 30));
        long treasureRadar = NPoint.addExperienceMultiplierBonus(equipmentBonus, equipmentBonus, 2);
        eq(6_000L, TreasureMapPolicy.multiplyMapExperience(treasureRadar));
        eq(2_000L, TreasureMapPolicy.multiplyMapExperience(
                NPoint.applyExperiencePercentBonuses(base, Collections.emptyList())));

        eq(200, NPoint.petExperiencePercentForMultiplier(3));
        eq(300, NPoint.petExperiencePercentForMultiplier(4));
        eq(500L, NPoint.calculateMasterShareFromPet(1_001L));
        eq(0L, NPoint.calculateMasterShareFromPet(-1L));
    }

    private static void eq(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError();
        }
    }
}
