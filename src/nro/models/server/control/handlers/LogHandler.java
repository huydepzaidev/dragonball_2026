package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.http.JsonResponse;
import nro.models.server.control.log.ConsoleLogBuffer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class LogHandler {

    public static void handle(HttpExchange exchange, AdminUser user, int limit, String levelFilter) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        List<ConsoleLogBuffer.LogEntry> logs = ConsoleLogBuffer.gI().getRecentLogs(limit, levelFilter);
        JSONArray arr = new JSONArray();
        for (ConsoleLogBuffer.LogEntry entry : logs) {
            arr.add(entry.toJson());
        }

        JSONObject res = new JSONObject();
        res.put("total", arr.size());
        res.put("logs", arr);

        JsonResponse.ok(exchange, res);
    }
}
