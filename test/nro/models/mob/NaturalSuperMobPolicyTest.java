package nro.models.mob;

public final class NaturalSuperMobPolicyTest {

    private NaturalSuperMobPolicyTest() {
    }

    public static void main(String[] args) {
        testEligibility();
        testSpawnThresholdsAndSlots();
        testHpBounds();
        testDamageCaps();
        testExperienceAndAura();
        System.out.println("NATURAL_SUPER_MOB_POLICY_OK");
    }

    private static void testEligibility() {
        assertFalse(NaturalSuperMobPolicy.isEligibleBaseHp(2_999));
        assertTrue(NaturalSuperMobPolicy.isEligibleBaseHp(3_000));
        assertTrue(NaturalSuperMobPolicy.isEligibleBaseHp(9_999_999));
        assertFalse(NaturalSuperMobPolicy.isEligibleBaseHp(10_000_000));
    }

    private static void testSpawnThresholdsAndSlots() {
        assertEquals(15, NaturalSuperMobPolicy.requiredNormalKills(1));
        assertEquals(14, NaturalSuperMobPolicy.requiredNormalKills(2));
        assertEquals(12, NaturalSuperMobPolicy.requiredNormalKills(4));
        assertEquals(10, NaturalSuperMobPolicy.requiredNormalKills(7));
        assertEquals(1, NaturalSuperMobPolicy.maxConcurrentSupers(1));
        assertEquals(2, NaturalSuperMobPolicy.maxConcurrentSupers(4));
        assertEquals(3, NaturalSuperMobPolicy.maxConcurrentSupers(7));

        NaturalSuperMobSpawnState solo = new NaturalSuperMobSpawnState();
        for (int i = 0; i < 14; i++) {
            assertFalse(solo.recordEligibleNormalKill(1, 0));
        }
        assertTrue(solo.recordEligibleNormalKill(1, 0));
        assertEquals(0, solo.getEligibleNormalKills());

        NaturalSuperMobSpawnState crowded = new NaturalSuperMobSpawnState();
        for (int i = 0; i < 9; i++) {
            assertFalse(crowded.recordEligibleNormalKill(8, 0));
        }
        assertTrue(crowded.recordEligibleNormalKill(8, 0));

        NaturalSuperMobSpawnState full = new NaturalSuperMobSpawnState();
        for (int i = 0; i < 10; i++) {
            assertFalse(full.recordEligibleNormalKill(8, 3));
        }
        assertEquals(10, full.getEligibleNormalKills());
        assertTrue(full.recordEligibleNormalKill(8, 2));
    }

    private static void testHpBounds() {
        assertEquals(30_000, NaturalSuperMobPolicy.minSuperHp(3_000));
        assertEquals(36_000, NaturalSuperMobPolicy.maxSuperHp(3_000));
        assertEquals(4_000_000, NaturalSuperMobPolicy.minSuperHp(500_000));
        assertEquals(6_000_000, NaturalSuperMobPolicy.maxSuperHp(500_000));
        assertEquals(10_000_000, NaturalSuperMobPolicy.maxSuperHp(9_999_999));

        for (int i = 0; i < 1_000; i++) {
            int hp = NaturalSuperMobPolicy.rollSuperHp(3_000);
            assertTrue(hp >= 30_000 && hp <= 36_000);
            assertTrue(NaturalSuperMobPolicy.rollSuperHp(500_000) <= 10_000_000);
        }
    }

    private static void testDamageCaps() {
        assertEquals(1_000, NaturalSuperMobPolicy.damageCapForHp(30_000));
        assertEquals(100_000, NaturalSuperMobPolicy.damageCapForHp(1_000_000));
        assertEquals(500_000, NaturalSuperMobPolicy.damageCapForHp(5_000_000));
        assertEquals(1_000_000, NaturalSuperMobPolicy.damageCapForHp(10_000_000));

        int previous = 0;
        for (int hp = 30_000; hp <= 10_000_000; hp += 10_000) {
            int cap = NaturalSuperMobPolicy.damageCapForHp(hp);
            assertTrue(cap >= previous);
            assertTrue(cap <= 1_000_000);
            previous = cap;
        }
    }

    private static void testExperienceAndAura() {
        assertEquals(3_000L, NaturalSuperMobPolicy.multiplyExperience(1_000L));
        assertEquals(Long.MAX_VALUE, NaturalSuperMobPolicy.multiplyExperience(Long.MAX_VALUE));
        for (int i = 0; i < 1_000; i++) {
            int aura = NaturalSuperMobPolicy.randomAuraLevel();
            assertTrue(aura >= 1 && aura <= 3);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("condition was false");
        }
    }

    private static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
