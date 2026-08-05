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

        DaishinkanRanking.hideTaskScore(top);
        assertHidden(top, " - Ẩn NV");
        DaishinkanRanking.hideSummerEventScore(top);
        assertHidden(top, " - Ẩn điểm");
        DaishinkanRanking.hideBossScore(top);
        assertHidden(top, " - Ẩn Boss");

        System.out.println("DAISHINKAN_RANKING_OK limit=10 power=vi scores=hidden");
    }

    private static void assertHidden(TOP top, String expected) {
        assertEquals(true, top.isHiddenScore());
        assertEquals(0L, top.getParamCompare());
        assertEquals(expected, top.getInfo1());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}