package nro.models.server;

import nro.models.matches.TOP;

public final class DaishinkanRankingTest {

    private DaishinkanRankingTest() {
    }

    public static void main(String[] args) {
        assertEquals(10, DaishinkanRanking.LIMIT);

        TOP top = TOP.builder().build();
        DaishinkanRanking.showPower(top, 60_000_000_000L);
        assertEquals("60 tỉ sức mạnh", top.getInfo1());
        DaishinkanRanking.showPower(top, 2_500L);
        assertEquals("2,5 nghìn sức mạnh", top.getInfo1());

        DaishinkanRanking.showTaskScore(top, 29);
        assertVisible(top, "Nhiệm vụ số 29", 29L);
        DaishinkanRanking.showSummerEventScore(top, 1_000L);
        assertVisible(top, "1000 điểm", 1_000L);
        DaishinkanRanking.showBossScore(top, 350L);
        assertVisible(top, "350 điểm", 350L);

        System.out.println("DAISHINKAN_RANKING_OK limit=10 power=vi scores=visible");
    }

    private static void assertVisible(TOP top, String expected, long compareValue) {
        assertEquals(false, top.isHiddenScore());
        assertEquals(compareValue, top.getParamCompare());
        assertEquals(expected, top.getInfo1());
        assertEquals(expected, top.getInfo2());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
