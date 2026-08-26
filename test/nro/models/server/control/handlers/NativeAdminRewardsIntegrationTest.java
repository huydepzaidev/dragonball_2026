package nro.models.server.control.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import nro.models.data.LocalManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Read-only smoke test against the shared team2026 schema. */
public final class NativeAdminRewardsIntegrationTest {

    private NativeAdminRewardsIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = LocalManager.getConnection()) {
            Method rewardsMethod = NativeAdminHandler.class.getDeclaredMethod(
                    "rewards", Connection.class, Map.class);
            rewardsMethod.setAccessible(true);
            Map<String, String> query = new HashMap<>();
            query.put("ranking", "top_boss");
            JSONObject payload = (JSONObject) rewardsMethod.invoke(null, connection, query);
            requireArray(payload, "preview");
            requireArray(payload, "configs");
            requireArray(payload, "commands");
            requireArray(payload, "mailboxes");
            requireArray(payload, "mailbox_counts");
            if (!(payload.get("period") instanceof JSONObject period)
                    || !"WEEKLY".equals(period.get("type"))) {
                throw new AssertionError("Top boss period metadata is invalid");
            }

            int itemId;
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT id FROM item_template ORDER BY id LIMIT 1")) {
                if (!result.next()) {
                    throw new AssertionError("item_template is empty");
                }
                itemId = result.getInt(1);
            }
            Method normalize = NativeAdminHandler.class.getDeclaredMethod(
                    "normalizeRewards", Connection.class, Object.class, Integer.class);
            normalize.setAccessible(true);
            JSONArray bundle = new JSONArray();
            bundle.add(reward(itemId, 1));
            String normalized = (String) normalize.invoke(null, connection, bundle, null);
            if (!normalized.contains("\"id\":" + itemId)) {
                throw new AssertionError("Valid database item was not normalized");
            }

            bundle.add(reward(itemId, 2));
            try {
                normalize.invoke(null, connection, bundle, null);
                throw new AssertionError("Duplicate reward item was accepted");
            } catch (InvocationTargetException expected) {
                if (!(expected.getCause() instanceof IllegalArgumentException)) {
                    throw expected;
                }
            }
        } finally {
            LocalManager.close();
        }
        System.out.println("NATIVE_ADMIN_REWARDS_DB_OK");
    }

    private static JSONObject reward(int id, int quantity) {
        JSONObject reward = new JSONObject();
        reward.put("id", id);
        reward.put("quantity", quantity);
        reward.put("options", new JSONArray());
        return reward;
    }

    private static void requireArray(JSONObject payload, String key) {
        if (!(payload.get(key) instanceof JSONArray)) {
            throw new AssertionError("Missing array field: " + key);
        }
    }
}
