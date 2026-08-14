package nro.models.player;

public final class NPointPowerGainReductionTest {

    private NPointPowerGainReductionTest() {
    }

    public static void main(String[] args) {
        assertMilestones(new NPoint(null));

        Player master = new Player();
        Pet pet = new Pet(master);
        assertMilestones(pet.nPoint);
    }

    private static void assertMilestones(NPoint nPoint) {

        assertGain(nPoint, 39_999_999_999L, 10_000L);
        assertGain(nPoint, 40_000_000_000L, 8_500L);
        assertGain(nPoint, 49_999_999_999L, 8_500L);
        assertGain(nPoint, 50_000_000_000L, 7_500L);
        assertGain(nPoint, 59_999_999_999L, 7_500L);
        assertGain(nPoint, 60_000_000_000L, 7_000L);
        assertGain(nPoint, 69_999_999_999L, 7_000L);
        assertGain(nPoint, 70_000_000_000L, 6_500L);
        assertGain(nPoint, 79_999_999_999L, 6_500L);
        assertGain(nPoint, 80_000_000_000L, 6_000L);
        assertGain(nPoint, 89_999_999_999L, 6_000L);
        assertGain(nPoint, 90_000_000_000L, 6_000L);
    }

    private static void assertGain(NPoint nPoint, long power, long expectedGain) {
        nPoint.power = power;
        long actualGain = nPoint.calSubTNSM(10_000L);
        if (actualGain != expectedGain) {
            throw new AssertionError();
        }
    }
}
