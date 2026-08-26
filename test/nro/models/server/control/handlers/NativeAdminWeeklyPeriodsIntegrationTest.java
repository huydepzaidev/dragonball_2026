package nro.models.server.control.handlers;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import nro.models.data.LocalManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Verifies that Control exposes the empty current week just like web2026. */
public final class NativeAdminWeeklyPeriodsIntegrationTest {

    private NativeAdminWeeklyPeriodsIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = LocalManager.getConnection()) {
            Method rewardsMethod = NativeAdminHandler.class.getDeclaredMethod(
                    "rewards", Connection.class, Map.class);
            rewardsMethod.setAccessible(true);
            Map<String, String> query = new HashMap<>();
            query.put("ranking", "top_boss");
            JSONObject payload = (JSONObject) rewardsMethod.invoke(null, connection, query);

            Object rawPeriods = payload.get("weekly_periods");
            if (!(rawPeriods instanceof JSONArray periods) || periods.isEmpty()) {
                throw new AssertionError("Current weekly period is missing");
            }
            LocalDate monday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
                monday = monday.minusDays(1);
            }
            JSONObject first = (JSONObject) periods.get(0);
            if (!monday.toString().equals(String.valueOf(first.get("ranking_date")))) {
                throw new AssertionError("Current week must be the first selectable period");
            }
        } finally {
            LocalManager.close();
        }
        System.out.println("NATIVE_ADMIN_CURRENT_WEEK_OK");
    }
}
