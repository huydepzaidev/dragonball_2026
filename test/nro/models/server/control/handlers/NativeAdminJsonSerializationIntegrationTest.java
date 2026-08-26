package nro.models.server.control.handlers;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import nro.models.data.LocalManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Covers the final JSON serialization step used by the Control HTTP response. */
public final class NativeAdminJsonSerializationIntegrationTest {

    private NativeAdminJsonSerializationIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = LocalManager.getConnection()) {
            Method rewardsMethod = NativeAdminHandler.class.getDeclaredMethod(
                    "rewards", Connection.class, Map.class);
            rewardsMethod.setAccessible(true);
            Map<String, String> query = new HashMap<>();
            query.put("ranking", "top_boss");
            JSONObject payload = (JSONObject) rewardsMethod.invoke(null, connection, query);

            String encoded = payload.toJSONString();
            if (!(JSONValue.parse(encoded) instanceof JSONObject)) {
                throw new AssertionError("Native admin rewards response is not valid JSON");
            }
            JSONArray configs = (JSONArray) payload.get("configs");
            if (configs.isEmpty()
                    || !(((JSONObject) configs.get(0)).get("updated_at") instanceof String)) {
                throw new AssertionError("SQL timestamp was not converted to a JSON string");
            }
        } finally {
            LocalManager.close();
        }
        System.out.println("NATIVE_ADMIN_JSON_SERIALIZATION_OK");
    }
}
