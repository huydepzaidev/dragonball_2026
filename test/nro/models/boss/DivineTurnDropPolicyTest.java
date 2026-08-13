package nro.models.boss;

public final class DivineTurnDropPolicyTest {

    private DivineTurnDropPolicyTest() {
    }

    public static void main(String[] args) {
        oneMemberDistribution();
        twoMemberDistribution();
        multiMemberDistribution();
        pityGuaranteesOneItem();
        invalidConfigFallsBackToDefaults();
        excludedBossesNeverEnterDivineTurns();
        System.out.println("DIVINE_TURN_DROP_POLICY_OK max=3 pity=6th-blank-turn");
    }

    private static void oneMemberDistribution() {
        eq(0, drops(1, 0, 0));
        eq(0, drops(1, 6_499, 0));
        eq(1, drops(1, 6_500, 0));
        eq(1, drops(1, 9_499, 0));
        eq(2, drops(1, 9_500, 0));
        eq(2, drops(1, 9_999, 0));
    }

    private static void twoMemberDistribution() {
        eq(0, drops(2, 4_999, 0));
        eq(1, drops(2, 5_000, 0));
        eq(1, drops(2, 8_499, 0));
        eq(2, drops(2, 8_500, 0));
        eq(2, drops(2, 9_999, 0));
    }

    private static void multiMemberDistribution() {
        eq(0, drops(3, 3_999, 0));
        eq(1, drops(3, 4_000, 0));
        eq(1, drops(9, 7_499, 0));
        eq(2, drops(5, 7_500, 0));
        eq(2, drops(3, 9_499, 0));
        eq(3, drops(3, 9_500, 0));
        eq(3, drops(9, 9_999, 0));
    }

    private static void pityGuaranteesOneItem() {
        eq(0, drops(3, 0, 4));
        eq(1, drops(1, 0, 5));
        eq(1, drops(9, 9_999, 5));
    }

    private static void invalidConfigFallsBackToDefaults() {
        DivineTurnDropPolicy.Rates invalid = new DivineTurnDropPolicy.Rates(
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
        eq(2, DivineTurnDropPolicy.dropCount(1, 9_999, 0, invalid));
    }

    private static int drops(int members, int roll, int blankTurns) {
        return DivineTurnDropPolicy.dropCount(members, roll, blankTurns,
                DivineTurnDropPolicy.DEFAULT_RATES);
    }

    private static void excludedBossesNeverEnterDivineTurns() {
        int[] excluded = {
            BossID.KUKU, BossID.MAP_DAU_DINH, BossID.RAMBO,
            BossID.SO_4, BossID.SO_3, BossID.SO_2, BossID.SO_1,
            BossID.TIEU_DOI_TRUONG,
            BossID.SO_4_NM, BossID.SO_3_NM, BossID.SO_2_NM, BossID.SO_1_NM,
            BossID.TIEU_DOI_TRUONG_NM,
            BossID.FIDE, BossID.ANDROID_19, BossID.DR_KORE,
            BossID.PIC, BossID.POC, BossID.KING_KONG
        };
        for (int bossId : excluded) {
            if (DivineTurnDropService.isDivineEligibleBossId(bossId)) {
                throw new AssertionError(bossId);
            }
        }
        if (!DivineTurnDropService.isDivineEligibleBossId(BossID.COOLER)
                || !DivineTurnDropService.isDivineEligibleBossId(BossID.BLACK_GOKU)
                || !DivineTurnDropService.isDivineEligibleBossId(BossID.XEN_BO_HUNG)) {
            throw new AssertionError();
        }
    }

    private static void eq(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
