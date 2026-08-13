package nro.models.boss.Boss_mini;

public final class MiniBossRubyCapsuleRewardTest {

    private MiniBossRubyCapsuleRewardTest() {
    }

    public static void main(String[] args) {
        int[] counts = new int[6];
        for (int roll = 0; roll < MiniBossRubyCapsuleRewardService.TOTAL_WEIGHT; roll++) {
            int capsuleCount = MiniBossRubyCapsuleRewardService.capsuleCountForRoll(roll);
            require(capsuleCount >= 1 && capsuleCount <= 5);
            counts[capsuleCount]++;
        }

        require(counts[1] == 700);
        require(counts[2] == 150);
        require(counts[3] == 80);
        require(counts[4] == 50);
        require(counts[5] == 20);
        require(counts[1] > counts[2]);
        require(counts[2] > counts[3]);
        require(counts[3] > counts[4]);
        require(counts[4] > counts[5]);

        requireThrows(-1);
        requireThrows(MiniBossRubyCapsuleRewardService.TOTAL_WEIGHT);
    }

    private static void requireThrows(int roll) {
        try {
            MiniBossRubyCapsuleRewardService.capsuleCountForRoll(roll);
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
