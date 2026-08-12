package nro.models.map.phoban;

public final class TreasureMapPolicyTest {
    private TreasureMapPolicyTest() {
    }

    public static void main(String[] args) {
        eq(200_000_000, TreasureMapPolicy.mobMaxHp(110, false));
        eq(300_000_000, TreasureMapPolicy.mobMaxHp(110, true));
        eq(100_000_000, TreasureMapPolicy.mobMaxHp(55, false));
        eq(150_000_000, TreasureMapPolicy.mobMaxHp(55, true));
        ok(TreasureMapPolicy.isTreasureMap(135));
        ok(TreasureMapPolicy.isTreasureMap(138));
        ok(!TreasureMapPolicy.isTreasureMap(134));
        ok(!TreasureMapPolicy.isTreasureMap(139));
        ok(TreasureMapPolicy.canEnterTreasureMap(135, false));
        ok(TreasureMapPolicy.canEnterTreasureMap(138, false));
        ok(TreasureMapPolicy.canEnterTreasureMap(5, true));
        ok(!TreasureMapPolicy.canEnterTreasureMap(5, false));
        ok(TreasureMapPolicy.canReceiveExperience(500_000));
        ok(!TreasureMapPolicy.canReceiveExperience(500_001));
        long normal = TreasureMapPolicy.baseExperience(700_000, 200_000_000);
        long superMob = TreasureMapPolicy.baseExperience(700_000, 300_000_000);
        eq(600_000, normal);
        eq(650_000, superMob);
        eq(3_000_000, TreasureMapPolicy.multiplyMapExperience(normal));
        eq(3_250_000, TreasureMapPolicy.multiplyMapExperience(superMob));
        eq(Integer.MAX_VALUE, TreasureMapPolicy.toIntExperience((long) Integer.MAX_VALUE + 1));
        System.out.println("TREASURE_MAP_POLICY_OK");
    }

    private static void ok(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    private static void eq(long expected, long actual) {
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
}
