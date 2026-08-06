package nro.models.server;

import java.lang.reflect.Method;
import java.util.Map;

public final class TopRewardServiceTest {

    private TopRewardServiceTest() {
    }

    public static void main(String[] args) throws Exception {
        assertContains(TopRewardService.rankingSql("top_boss"), "daily_ranking_score");
        assertContains(TopRewardService.rankingSql("summer"), "SUMMER_EVENT");
        assertContains(TopRewardService.rankingSql("top_power"), "data_point");
        assertContains(TopRewardService.rankingSql("top_task"), "data_task");
        assertContains(TopRewardService.rankingSql("childrens_day"), "point_sukien");
        assertContains(TopRewardService.rankingSql("top_up"), "tongnap");
        assertContains(TopRewardService.rankingSql("top_up"), "a.is_admin=0");
        assertContains(TopRewardService.rankingSql("top_boss", true), "d.ranking_date=?");
        for (String key : new String[]{
            "top_boss", "summer", "top_power", "top_task",
            "childrens_day", "sugarcane", "fruit_ice_cream", "top_up"
        }) {
            assertContains(TopRewardService.rankingSql(key), "LIMIT 10");
        }
        if (TopRewardService.MAX_REWARD_RANK != 10) {
            throw new AssertionError("Automatic reward limit must be Top 10");
        }
        StringBuilder snapshot = new StringBuilder("[");
        for (int rank = 1; rank <= 10; rank++) {
            if (rank > 1) {
                snapshot.append(',');
            }
            snapshot.append("{\"rank_position\":").append(rank)
                    .append(",\"title\":\"Top ").append(rank)
                    .append("\",\"message\":\"Test\",\"sender_name\":\"Admin\",")
                    .append("\"rewards_json\":\"[]\"}");
        }
        snapshot.append(']');
        Method parser = TopRewardService.class.getDeclaredMethod("parseConfigSnapshot", String.class);
        parser.setAccessible(true);
        Object parsed = parser.invoke(TopRewardService.gI(), snapshot.toString());
        if (!(parsed instanceof Map<?, ?> configs) || configs.size() != 10) {
            throw new AssertionError("Top reward snapshot must contain 10 ranks");
        }
        try {
            TopRewardService.rankingSql("unknown");
            throw new AssertionError("Unsupported ranking must fail");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("TOP_REWARD_QUERY_WHITELIST_OK");
    }

    private static void assertContains(String value, String expected) {
        if (!value.contains(expected)) {
            throw new AssertionError("Missing '" + expected + "' in " + value);
        }
    }
}
