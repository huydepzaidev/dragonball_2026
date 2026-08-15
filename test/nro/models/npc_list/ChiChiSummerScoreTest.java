package nro.models.npc_list;

import nro.models.consts.ConstItem;

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
        verifySummerRewards();
        System.out.println("CHI_CHI_SUMMER_SCORE_OK weekly=1000 total=2500 dialog=true");
    }

    private static void verifySummerRewards() {
        require(ChiChi.summerRewardIndexForRoll(1) == 0, "first reward lower boundary");
        require(ChiChi.summerRewardIndexForRoll(3_465) == 0, "first reward upper boundary");
        require(ChiChi.summerRewardIndexForRoll(3_466) == 1, "second reward lower boundary");
        require(ChiChi.summerRewardIndexForRoll(6_931) == 2, "third reward lower boundary");
        require(ChiChi.summerRewardIndexForRoll(8_416) == 3, "fourth reward lower boundary");
        require(ChiChi.summerRewardIndexForRoll(9_900) == 3, "card reward upper boundary");
        require(ChiChi.summerRewardIndexForRoll(9_901) == 4, "Naruto chest lower boundary");
        require(ChiChi.summerRewardIndexForRoll(10_000) == 4, "Naruto chest upper boundary");

        require(ChiChi.summerRewardItemIdForIndex(4) == ConstItem.RUONG_HOP_TAC_NARUTO,
                "Naruto chest item id");
        require(ChiChi.summerRewardQuantityForIndex(4) == 1, "Naruto chest quantity");

        String expected = "Các phần quà ngẫu nhiên:\n"
                + "- Rồng Thần Namek\n"
                + "- Oozaru\n"
                + "- Oozarun 1\n"
                + "- Oozarun 2\n"
                + "- Rương hợp tác Naruto";
        String actual = ChiChi.formatSummerRewardList();
        require(expected.equals(actual), "reward-only dialog");
        require(!actual.contains("%"), "dialog must hide rates");
        require(!actual.contains("đổi tối đa"), "dialog must hide exchange metrics");
        require(!actual.contains("Vỏ ốc"), "dialog must hide inventory quantities");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
