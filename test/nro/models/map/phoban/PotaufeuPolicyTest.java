package nro.models.map.phoban;

public final class PotaufeuPolicyTest {
    private PotaufeuPolicyTest() {
    }

    public static void main(String[] args) {
        ok(!PotaufeuPolicy.canEnter(PotaufeuPolicy.REQUIRED_TASK_ID - 1));
        ok(PotaufeuPolicy.canEnter(PotaufeuPolicy.REQUIRED_TASK_ID));
        ok(!PotaufeuPolicy.isAutoReturnReady(10_000, 9_999));
        ok(PotaufeuPolicy.isAutoReturnReady(10_000, 10_000));
        eq(300_000, PotaufeuPolicy.CHALLENGE_DURATION_MS);
        eq(60_000, PotaufeuPolicy.HOME_CAPSULE_DELAY_MS);
        System.out.println("POTAUFEU_POLICY_OK");
    }

    private static void ok(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    private static void eq(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
