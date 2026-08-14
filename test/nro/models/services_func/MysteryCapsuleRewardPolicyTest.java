package nro.models.services_func;

import java.util.HashMap;
import java.util.Map;

public final class MysteryCapsuleRewardPolicyTest {

    private MysteryCapsuleRewardPolicyTest() {
    }

    public static void main(String[] args) {
        Map<Short, Integer> counts = new HashMap<>();
        for (int roll = 0; roll < MysteryCapsuleRewardPolicy.ROLL_BOUND; roll++) {
            short reward = MysteryCapsuleRewardPolicy.rewardForRoll(roll);
            counts.merge(reward, 1, Integer::sum);
        }

        assertCount(counts, MysteryCapsuleRewardPolicy.GOLD_REWARD, 4_400);
        for (short itemId = 381; itemId <= 383; itemId++) {
            assertCount(counts, itemId, 300);
        }
        assertCount(counts, (short) 384, 1_500);
        assertCount(counts, (short) 385, 3_000);
        for (short itemId = 1150; itemId <= 1152; itemId++) {
            assertCount(counts, itemId, 10);
        }
        assertCount(counts, (short) 1153, 60);
        assertCount(counts, (short) 1154, 110);
    }

    private static void assertCount(Map<Short, Integer> counts, short reward, int expected) {
        if (counts.getOrDefault(reward, 0) != expected) {
            throw new AssertionError();
        }
    }
}
