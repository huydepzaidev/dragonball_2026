package nro.models.server;

import java.time.LocalDate;
import nro.models.boss.BossID;

public final class DailyRankingServiceTest {

    private DailyRankingServiceTest() {
    }

    public static void main(String[] args) {
        int[] allowed = {
            BossID.SIEU_BO_HUNG,
            BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3,
            BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6, BossID.XEN_CON_7,
            BossID.BLACK_GOKU, BossID.CUMBER, BossID.GOD_BILL, BossID.GOD_CHAMPA,
            BossID.PILAP, BossID.MAI_PILAP, BossID.PU_PILAP,
            BossID.SOI_DO_VO_TINH, BossID.SOI_VANG_VO_TINH,
            BossID.SOI_XANH_XAM_VO_TINH, BossID.CHILL_1, BossID.CHILL_2,
            BossID.COOLER, BossID.ZAMASU
        };
        for (int bossId : allowed) {
            assertTrue(DailyRankingService.isEligibleBossId(bossId));
        }
        assertFalse(DailyRankingService.isEligibleBossId(BossID.KUKU));
        assertFalse(DailyRankingService.isEligibleBossId(BossID.FIDE));
        assertEquals(50L, DailyRankingService.BOSS_KILL_POINTS);
        assertEquals(LocalDate.of(2026, 8, 3),
                DailyRankingService.getWeekStart(LocalDate.of(2026, 8, 9)));
        assertEquals(LocalDate.of(2026, 8, 10),
                DailyRankingService.getWeekStart(LocalDate.of(2026, 8, 10)));
        System.out.println("WEEKLY_RANKING_OK allowed=22 bossPoints=50 mondayReset=true");
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected=true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected=false");
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
