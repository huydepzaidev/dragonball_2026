package nro.models.server.control.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.simple.JSONObject;

public final class JsonResponse {

    public static void send(HttpExchange exchange, int statusCode, JSONObject responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

        byte[] bytes = responseBody.toJSONString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    public static void ok(HttpExchange exchange, Object data, String message) throws IOException {
        JSONObject res = new JSONObject();
        res.put("success", true);
        res.put("data", data);
        res.put("message", message != null ? message : "OK");
        send(exchange, 200, res);
    }

    public static void ok(HttpExchange exchange, Object data) throws IOException {
        ok(exchange, data, "OK");
    }

    public static void error(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject res = new JSONObject();
        res.put("success", false);
        res.put("message", message != null ? message : "Có lỗi xảy ra");
        res.put("status_code", statusCode);
        send(exchange, statusCode, res);
    }

    public static void badRequest(HttpExchange exchange, String message) throws IOException {
        error(exchange, 400, message);
    }

    public static void unauthorized(HttpExchange exchange, String message) throws IOException {
        error(exchange, 401, message != null ? message : "Yêu cầu đăng nhập hoặc token không hợp lệ");
    }

    public static void forbidden(HttpExchange exchange, String message) throws IOException {
        error(exchange, 403, message != null ? message : "Bạn không đủ quyền thực hiện hành động này");
    }

    public static void notFound(HttpExchange exchange, String message) throws IOException {
        error(exchange, 404, message != null ? message : "Endpoint không tồn tại");
    }

    public static void serverError(HttpExchange exchange, String message) throws IOException {
        error(exchange, 500, message != null ? message : "Lỗi xử lý máy chủ nội bộ");
    }
}
