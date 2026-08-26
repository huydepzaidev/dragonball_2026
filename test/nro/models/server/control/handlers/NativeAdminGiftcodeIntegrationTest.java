package nro.models.server.control.handlers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import nro.models.data.LocalManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Transactional checks for the native Giftcode editor contract. */
public final class NativeAdminGiftcodeIntegrationTest {

    private NativeAdminGiftcodeIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = LocalManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Method giftcodeAct = NativeAdminHandler.class.getDeclaredMethod(
                        "giftcodeAct", Connection.class, String.class, JSONObject.class);
                giftcodeAct.setAccessible(true);

                int itemId = firstItemId(connection);
                String code = "CODEX_GIFT_" + Long.toString(System.nanoTime(), 36).toUpperCase();
                JSONObject valid = request(code, itemId);
                String result = (String) giftcodeAct.invoke(null, connection, "save", valid);
                if (!result.contains("Đã tạo giftcode")) {
                    throw new AssertionError("Giftcode create result is invalid: " + result);
                }

                expectIllegalArgument(() -> giftcodeAct.invoke(null, connection, "save", valid),
                        "Duplicate giftcode was accepted");

                JSONObject badDate = request(code + "_DATE", itemId);
                badDate.put("expired", "2030-99-99");
                expectIllegalArgument(() -> giftcodeAct.invoke(null, connection, "save", badDate),
                        "Invalid expiration was accepted");

                JSONObject badCount = request(code + "_COUNT", itemId);
                badCount.put("count_left", -2);
                expectIllegalArgument(() -> giftcodeAct.invoke(null, connection, "save", badCount),
                        "Invalid use count was accepted");

                JSONObject missingItem = request(code + "_ITEM", Integer.MAX_VALUE);
                expectIllegalArgument(() -> giftcodeAct.invoke(null, connection, "save", missingItem),
                        "Missing item_template ID was accepted");
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } finally {
            LocalManager.close();
        }
        System.out.println("NATIVE_ADMIN_GIFTCODE_DB_OK");
    }

    private static JSONObject request(String code, int itemId) {
        JSONArray rewards = new JSONArray();
        JSONObject reward = new JSONObject();
        reward.put("id", itemId);
        reward.put("quantity", 1);
        reward.put("options", new JSONArray());
        rewards.add(reward);

        JSONObject request = new JSONObject();
        request.put("id", null);
        request.put("code", code);
        request.put("count_left", -1);
        request.put("detail", rewards);
        request.put("expired", "2030-12-31 23:59:59");
        return request;
    }

    private static int firstItemId(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id FROM item_template ORDER BY id LIMIT 1")) {
            if (!result.next()) {
                throw new AssertionError("item_template is empty");
            }
            return result.getInt(1);
        }
    }

    private static void expectIllegalArgument(ThrowingAction action, String message)
            throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (InvocationTargetException expected) {
            if (!(expected.getCause() instanceof IllegalArgumentException)) {
                throw expected;
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
