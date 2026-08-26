package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.UserRole;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class BossHandler {

    public static void handleList(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONArray bossList = new JSONArray();

        for (Boss boss : BossManager.getAllManagedBosses()) {
            if (boss == null) continue;
            JSONObject b = new JSONObject();
            b.put("id", boss.id);
            b.put("name", boss.name);
            b.put("status", boss.bossStatus != null ? boss.bossStatus.name() : "UNKNOWN");
            b.put("is_alive", !boss.isDie());

            if (boss.nPoint != null) {
                b.put("hp", boss.nPoint.hp);
                b.put("hp_max", boss.nPoint.hpMax);
                b.put("dame", boss.nPoint.dame);
            }

            if (boss.zone != null && boss.zone.map != null) {
                b.put("map_id", boss.zone.map.mapId);
                b.put("map_name", boss.zone.map.mapName);
                b.put("zone_id", boss.zone.zoneId);
                b.put("x", boss.location != null ? boss.location.x : 0);
                b.put("y", boss.location != null ? boss.location.y : 0);
            } else {
                b.put("map_id", -1);
                b.put("map_name", "Chưa xuất hiện");
                b.put("zone_id", -1);
            }

            bossList.add(b);
        }

        JSONObject res = new JSONObject();
        res.put("total", bossList.size());
        res.put("bosses", bossList);

        JsonResponse.ok(exchange, res);
    }

    public static void handleAction(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để thao tác với Boss");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String action = (String) req.get("action");
        if (action == null) {
            JsonResponse.badRequest(exchange, "Thiếu trường action");
            return;
        }

        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        switch (action.toLowerCase()) {
            case "respawn_single": {
                int bossId = ((Number) req.getOrDefault("boss_id", -1)).intValue();
                if (bossId == -1) {
                    JsonResponse.badRequest(exchange, "Thiếu boss_id");
                    return;
                }
                int count = BossManager.respawnBossesEverywhere(bossId);
                AuditLogService.gI().log(user, "RESPAWN_BOSS", "BOSS", bossId, "{\"count\":" + count + "}", clientIp);
                JsonResponse.ok(exchange, null, "Đã kích hoạt hồi sinh cho " + count + " instance boss (ID: " + bossId + ")");
                break;
            }
            case "respawn_all": {
                int count = BossManager.respawnAllBossesEverywhere();
                AuditLogService.gI().log(user, "RESPAWN_ALL_BOSSES", "BOSS", null, "{\"count\":" + count + "}", clientIp);
                JsonResponse.ok(exchange, null, "Đã kích hoạt hồi sinh toàn bộ " + count + " boss trên máy chủ");
                break;
            }
            default:
                JsonResponse.badRequest(exchange, "Hành động Boss không hợp lệ: " + action);
                break;
        }
    }
}
