package nro.models.mob;

public final class MysteryCapsuleDropPolicyTest {

    private MysteryCapsuleDropPolicyTest() {
    }

    public static void main(String[] args) {
        int drops = 0;
        for (int roll = 0; roll < MysteryCapsuleDropPolicy.ROLL_BOUND; roll++) {
            if (MysteryCapsuleDropPolicy.dropsForRoll(roll)) {
                drops++;
            }
        }
        if (drops != 50) {
            throw new AssertionError();
        }
        for (int mobId = 58; mobId <= 65; mobId++) {
            if (!MysteryCapsuleDropPolicy.isEligibleMob(mobId)) {
                throw new AssertionError();
            }
        }
        if (MysteryCapsuleDropPolicy.isEligibleMob(57)
                || MysteryCapsuleDropPolicy.isEligibleMob(66)) {
            throw new AssertionError();
        }
    }
}
