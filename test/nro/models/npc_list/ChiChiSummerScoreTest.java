package nro.models.npc_list;

public final class ChiChiSummerScoreTest {

    private ChiChiSummerScoreTest() {
    }

    public static void main(String[] args) {
        String actual = ChiChi.formatSummerEventScore(1_000L, 2_500L);
        String expected = "ĐIỂM SỰ KIỆN HÈ\n"
                + "Điểm tuần này: 1.000 điểm\n"
                + "Tổng điểm tích lũy: 2.500 điểm";
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
        System.out.println("CHI_CHI_SUMMER_SCORE_OK weekly=1000 total=2500 dialog=true");
    }
}
