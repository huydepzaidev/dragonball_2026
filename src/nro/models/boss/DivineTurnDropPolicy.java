package nro.models.boss;

/** Pure probability policy for divine-equipment drops in one boss turn. */
public final class DivineTurnDropPolicy {

    public static final Rates DEFAULT_RATES = new Rates(
            6_500, 3_000, 500,
            5_000, 3_500, 1_500,
            4_000, 3_500, 2_000, 500,
            5);

    private DivineTurnDropPolicy() {
    }

    public static int dropCount(int memberCount, int roll, int blankTurns, Rates rates) {
        if (memberCount < 1) {
            throw new IllegalArgumentException("memberCount must be positive");
        }
        if (roll < 0 || roll >= 10_000) {
            throw new IllegalArgumentException("roll must be in [0, 9999]");
        }
        Rates effective = rates == null || !rates.isValid() ? DEFAULT_RATES : rates;
        if (blankTurns >= effective.pityBlankTurns()) {
            return 1;
        }
        if (memberCount == 1) {
            return bucket(roll, effective.oneZero(), effective.oneOne(), effective.oneTwo());
        }
        if (memberCount == 2) {
            return bucket(roll, effective.twoZero(), effective.twoOne(), effective.twoTwo());
        }
        return bucket(roll, effective.multiZero(), effective.multiOne(),
                effective.multiTwo(), effective.multiThree());
    }

    private static int bucket(int roll, int... weights) {
        int upper = 0;
        for (int count = 0; count < weights.length; count++) {
            upper += weights[count];
            if (roll < upper) {
                return count;
            }
        }
        return 0;
    }

    public record Rates(
            int oneZero, int oneOne, int oneTwo,
            int twoZero, int twoOne, int twoTwo,
            int multiZero, int multiOne, int multiTwo, int multiThree,
            int pityBlankTurns) {

        public boolean isValid() {
            return nonNegative(oneZero, oneOne, oneTwo,
                    twoZero, twoOne, twoTwo,
                    multiZero, multiOne, multiTwo, multiThree)
                    && oneZero + oneOne + oneTwo == 10_000
                    && twoZero + twoOne + twoTwo == 10_000
                    && multiZero + multiOne + multiTwo + multiThree == 10_000
                    && pityBlankTurns >= 1 && pityBlankTurns <= 100;
        }

        private static boolean nonNegative(int... values) {
            for (int value : values) {
                if (value < 0 || value > 10_000) {
                    return false;
                }
            }
            return true;
        }
    }
}
