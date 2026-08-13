package nro.models.map;

public final class WestSanctuaryAccessPolicyTest {

    private WestSanctuaryAccessPolicyTest() {
    }

    public static void main(String[] args) {
        ok(WestSanctuaryAccessPolicy.MAP_ID == 156);
        ok(!WestSanctuaryAccessPolicy.canEnter(
                WestSanctuaryAccessPolicy.REQUIRED_TASK_ID - 1));
        ok(WestSanctuaryAccessPolicy.canEnter(
                WestSanctuaryAccessPolicy.REQUIRED_TASK_ID));
        ok(WestSanctuaryAccessPolicy.canEnter(
                WestSanctuaryAccessPolicy.REQUIRED_TASK_ID + 1));
        System.out.println("WEST_SANCTUARY_ACCESS_POLICY_OK map=156 task=22");
    }

    private static void ok(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
