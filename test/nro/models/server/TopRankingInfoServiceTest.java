package nro.models.server;

import java.util.HashMap;
import java.util.Map;

public final class TopRankingInfoServiceTest {

    private TopRankingInfoServiceTest() {
    }

    public static void main(String[] args) {
        assertEquals("1.000 vàng, 10 ngọc, 5 hồng ngọc",
                TopRankingInfoService.summarizeRewards(
                        "[{\"id\":-1,\"quantity\":1000,\"options\":[]},"
                        + "{\"id\":-2,\"quantity\":10,\"options\":[]},"
                        + "{\"id\":-3,\"quantity\":5,\"options\":[]}]"));

        StringBuilder empty = new StringBuilder();
        TopRankingInfoService.appendRewardSection(empty, Map.of());
        assertContains(empty.toString(), "Top 1: Chưa cấu hình trên web");
        assertContains(empty.toString(), "Top 4–10: Chưa cấu hình trên web");

        Map<Integer, String> incomplete = new HashMap<>();
        incomplete.put(4, "[{\"id\":-1,\"quantity\":100,\"options\":[]}]");
        StringBuilder incompleteText = new StringBuilder();
        TopRankingInfoService.appendRewardSection(incompleteText, incomplete);
        assertContains(incompleteText.toString(), "Top 4–10: Chưa cấu hình đầy đủ trên web");

        Map<Integer, String> grouped = new HashMap<>();
        for (int rank = 4; rank <= 10; rank++) {
            grouped.put(rank, "[{\"id\":-1,\"quantity\":100,\"options\":[]}]");
        }
        StringBuilder groupedText = new StringBuilder();
        TopRankingInfoService.appendRewardSection(groupedText, grouped);
        assertContains(groupedText.toString(), "Top 4–10: 100 vàng");
        System.out.println("TOP_RANKING_INFO_OK");
    }

    private static void assertContains(String value, String expected) {
        if (!value.contains(expected)) {
            throw new AssertionError("Missing '" + expected + "' in " + value);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
