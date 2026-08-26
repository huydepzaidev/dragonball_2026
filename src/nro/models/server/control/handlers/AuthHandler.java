package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.AuthManager;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public final class AuthHandler {

    public static void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject req = parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String username = (String) req.get("username");
        String password = (String) req.get("password");
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        AuthManager.AuthResult result = AuthManager.gI().login(username, password, clientIp);
        if (!result.success) {
            JsonResponse.unauthorized(exchange, result.message);
            return;
        }

        JSONObject data = new JSONObject();
        data.put("access_token", result.accessToken);
        data.put("refresh_token", result.refreshToken);
        data.put("expires_in", result.expiresIn);

        JSONObject userObj = new JSONObject();
        userObj.put("id", result.user.getId());
        userObj.put("username", result.user.getUsername());
        userObj.put("role", result.user.getRole().name());
        data.put("user", userObj);

        AuditLogService.gI().log(result.user, "ADMIN_LOGIN", "AUTH", result.user.getId(), "{\"ip\":\"" + clientIp + "\"}", clientIp);

        JsonResponse.ok(exchange, data, "Đăng nhập thành công");
    }

    public static void handleRefresh(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject req = parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String refreshToken = (String) req.get("refresh_token");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            JsonResponse.badRequest(exchange, "Thiếu refresh_token");
            return;
        }

        AuthManager.AuthResult result = AuthManager.gI().refreshToken(refreshToken);
        if (!result.success) {
            JsonResponse.unauthorized(exchange, result.message);
            return;
        }

        JSONObject data = new JSONObject();
        data.put("access_token", result.accessToken);
        data.put("refresh_token", result.refreshToken);
        data.put("expires_in", result.expiresIn);

        JSONObject userObj = new JSONObject();
        userObj.put("id", result.user.getId());
        userObj.put("username", result.user.getUsername());
        userObj.put("role", result.user.getRole().name());
        data.put("user", userObj);

        JsonResponse.ok(exchange, data, "Làm mới phiên đăng nhập thành công");
    }

    public static void handleMe(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject userObj = new JSONObject();
        userObj.put("id", user.getId());
        userObj.put("username", user.getUsername());
        userObj.put("role", user.getRole().name());

        JsonResponse.ok(exchange, userObj);
    }

    public static void handleLogout(HttpExchange exchange, AdminUser user, String token) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        AuthManager.gI().revokeToken(token);
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        AuditLogService.gI().log(user, "ADMIN_LOGOUT", "AUTH", user.getId(), "{}", clientIp);

        JsonResponse.ok(exchange, null, "Đã đăng xuất thành công");
    }

    public static JSONObject parseRequestBody(HttpExchange exchange) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() == 0) return new JSONObject();
            return (JSONObject) JSONValue.parse(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
