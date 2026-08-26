package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import nro.models.player_system.Template.ItemOptionTemplate;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Manager;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class CatalogHandler {

    public static void handleItems(HttpExchange exchange, AdminUser user, String search) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONArray list = new JSONArray();
        String filter = search != null ? search.trim().toLowerCase() : "";

        for (ItemTemplate temp : Manager.ITEM_TEMPLATES) {
            if (temp == null) continue;
            if (!filter.isEmpty()) {
                boolean matchId = String.valueOf(temp.id).contains(filter);
                boolean matchName = temp.name != null && temp.name.toLowerCase().contains(filter);
                if (!matchId && !matchName) continue;
            }

            JSONObject item = new JSONObject();
            item.put("id", (int) temp.id);
            item.put("name", temp.name);
            item.put("type", (int) temp.type);
            item.put("icon_id", (int) temp.iconID);
            item.put("description", temp.description);
            list.add(item);

            if (list.size() >= 200) break; // limit to 200 items per search query
        }

        JSONObject res = new JSONObject();
        res.put("total", list.size());
        res.put("items", list);
        JsonResponse.ok(exchange, res);
    }

    public static void handleOptions(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONArray list = new JSONArray();
        for (ItemOptionTemplate temp : Manager.ITEM_OPTION_TEMPLATES) {
            if (temp == null) continue;
            JSONObject opt = new JSONObject();
            opt.put("id", temp.id);
            opt.put("name", temp.name);
            opt.put("type", (int) temp.type);
            list.add(opt);
        }

        JSONObject res = new JSONObject();
        res.put("total", list.size());
        res.put("options", list);
        JsonResponse.ok(exchange, res);
    }
}
