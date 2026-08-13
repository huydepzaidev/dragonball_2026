package nro.models.boss.Boss_mini;

import nro.models.boss.Boss;
import nro.models.boss.RubyCapsuleDropService;
import nro.models.utils.Util;

public final class MiniBossRubyCapsuleRewardService {

    static final int TOTAL_WEIGHT = 1000;

    private MiniBossRubyCapsuleRewardService() {
    }

    public static void dropPublicRubyCapsules(Boss boss) {
        int dropCount = capsuleCountForRoll(Util.nextInt(TOTAL_WEIGHT));
        RubyCapsuleDropService.dropPublicRubyCapsules(boss, dropCount);
    }

    static int capsuleCountForRoll(int roll) {
        if (roll < 0 || roll >= TOTAL_WEIGHT) {
            throw new IllegalArgumentException("roll must be between 0 and 999");
        }
        if (roll < 700) {
            return 1;
        }
        if (roll < 850) {
            return 2;
        }
        if (roll < 930) {
            return 3;
        }
        if (roll < 980) {
            return 4;
        }
        return 5;
    }
}
