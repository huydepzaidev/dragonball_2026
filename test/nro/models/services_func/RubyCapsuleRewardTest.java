package nro.models.services_func;

public final class RubyCapsuleRewardTest {

    private RubyCapsuleRewardTest() {
    }

    public static void main(String[] args) {
        require(UseItem.rubyCapsuleRewardForRoll(0) == 1);
        require(UseItem.rubyCapsuleRewardForRoll(899) == 50);
        require(UseItem.rubyCapsuleRewardForRoll(900) == 51);
        require(UseItem.rubyCapsuleRewardForRoll(999) == 100);
        int rewardsOver50 = 0;
        for (int roll = 0; roll < 1000; roll++) {
            int reward = UseItem.rubyCapsuleRewardForRoll(roll);
            require(reward >= 1 && reward <= 100);
            if (reward > 50) {
                rewardsOver50++;
            }
        }
        require(rewardsOver50 == 100);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
