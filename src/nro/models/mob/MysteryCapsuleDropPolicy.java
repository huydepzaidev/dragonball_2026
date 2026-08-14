package nro.models.mob;

public final class MysteryCapsuleDropPolicy {

    public static final int ROLL_BOUND = 100;
    public static final int DROP_ROLLS = 50;

    private MysteryCapsuleDropPolicy() {
    }

    public static boolean isEligibleMob(int mobTemplateId) {
        return mobTemplateId >= 58 && mobTemplateId <= 65;
    }

    public static boolean dropsForRoll(int roll) {
        if (roll < 0 || roll >= ROLL_BOUND) {
            throw new IllegalArgumentException();
        }
        return roll < DROP_ROLLS;
    }
}
