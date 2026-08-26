package nro.models.server.control.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import org.json.simple.JSONObject;

public final class NativeAdminRewardsTest {

    private NativeAdminRewardsTest() {
    }

    public static void main(String[] args) throws Exception {
        Method periodMethod = NativeAdminHandler.class.getDeclaredMethod(
                "rankingPeriod", String.class, String.class, String.class);
        periodMethod.setAccessible(true);

        JSONObject lifetime = invokePeriod(periodMethod, "top_power", "ignored", "");
        assertEquals("LIFETIME", lifetime.get("type"));
        assertEquals("lifetime", lifetime.get("key"));
        if (lifetime.get("ranking_date") != null) {
            throw new AssertionError("Lifetime ranking must not have ranking_date");
        }

        LocalDate monday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }
        JSONObject weekly = invokePeriod(periodMethod, "top_boss", "ignored", monday.toString());
        assertEquals("WEEKLY", weekly.get("type"));
        assertEquals("week-" + monday.toString().replace("-", ""), weekly.get("key"));
        assertEquals(monday.toString(), weekly.get("ranking_date"));

        JSONObject manual = invokePeriod(periodMethod, "top_up", "topup-20260824", "");
        assertEquals("MANUAL", manual.get("type"));
        assertEquals("topup-20260824", manual.get("key"));

        expectInvalid(periodMethod, "top_boss", "", monday.plusDays(1).toString());
        expectInvalid(periodMethod, "top_up", "bad key with spaces", "");

        Method narutoMethod = NativeAdminHandler.class.getDeclaredMethod("narutoPreset", int.class);
        narutoMethod.setAccessible(true);
        if (!(narutoMethod.invoke(null, 2019) instanceof int[][] preset) || preset.length != 6) {
            throw new AssertionError("Naruto Top 1 preset is missing");
        }
        if (narutoMethod.invoke(null, 1) != null) {
            throw new AssertionError("Normal item must not receive Naruto preset");
        }
        System.out.println("NATIVE_ADMIN_REWARD_PERIODS_OK");
    }

    private static JSONObject invokePeriod(Method method, String ranking, String key, String date)
            throws Exception {
        return (JSONObject) method.invoke(null, ranking, key, date);
    }

    private static void expectInvalid(Method method, String ranking, String key, String date)
            throws Exception {
        try {
            invokePeriod(method, ranking, key, date);
            throw new AssertionError("Invalid reward period was accepted");
        } catch (InvocationTargetException expected) {
            if (!(expected.getCause() instanceof IllegalArgumentException)) {
                throw expected;
            }
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
