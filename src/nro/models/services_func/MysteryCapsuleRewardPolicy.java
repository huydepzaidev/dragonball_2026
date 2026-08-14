package nro.models.services_func;

public final class MysteryCapsuleRewardPolicy {

    public static final short GOLD_REWARD = -1;
    public static final int ROLL_BOUND = 10_000;
    private static final short[] NORMAL_LOW_RATE_ITEMS = {381, 382, 383};
    private static final short[] SUPER_LOW_RATE_ITEMS = {1150, 1151, 1152};

    private MysteryCapsuleRewardPolicy() {
    }

    public static short rewardForRoll(int roll) {
        if (roll < 0 || roll >= ROLL_BOUND) {
            throw new IllegalArgumentException();
        }
        if (roll < 4_400) {
            return GOLD_REWARD;
        }
        if (roll < 5_300) {
            return NORMAL_LOW_RATE_ITEMS[(roll - 4_400) / 300];
        }
        if (roll < 6_800) {
            return 384;
        }
        if (roll < 9_800) {
            return 385;
        }
        if (roll < 9_830) {
            return SUPER_LOW_RATE_ITEMS[(roll - 9_800) / 10];
        }
        if (roll < 9_890) {
            return 1153;
        }
        return 1154;
    }
}
