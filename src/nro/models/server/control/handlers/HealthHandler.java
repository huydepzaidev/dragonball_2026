package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import nro.models.server.ServerManager;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONObject;

public final class HealthHandler {

    public static void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject data = new JSONObject();
        data.put("status", "ONLINE");
        data.put("app_name", "Ngọc Rồng Vegeta");
        data.put("server_name", ServerManager.NAME);
        data.put("time_start", ServerManager.timeStart);
        data.put("server_port", ServerManager.PORT);
        data.put("current_time", System.currentTimeMillis());

        JsonResponse.ok(exchange, data, "Server đang hoạt động bình thường");
    }
}
