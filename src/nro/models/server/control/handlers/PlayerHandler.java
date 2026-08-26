package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.UserRole;
import nro.models.server.control.http.JsonResponse;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class PlayerHandler {

    public static void handleOnlineList(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        List<Player> players = Client.gI().snapshotPlayers();
        JSONArray list = new JSONArray();

        for (Player pl : players) {
            if (pl == null) continue;
            JSONObject p = new JSONObject();
            appendRuntimeFields(p, pl);
            list.add(p);
        }

        JSONObject res = new JSONObject();
        res.put("total", list.size());
        res.put("players", list);

        JsonResponse.ok(exchange, res);
    }

    public static void handleSearch(HttpExchange exchange, AdminUser user, String keyword) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            JsonResponse.badRequest(exchange, "Vui lòng nhập từ khóa tìm kiếm");
            return;
        }

        String normalizedKeyword = keyword.trim();
        String searchPattern = "%" + normalizedKeyword + "%";
        long exactPlayerId = -1;
        try {
            exactPlayerId = Long.parseLong(normalizedKeyword);
        } catch (NumberFormatException ignored) {
            // Không phải ID: vẫn tìm theo tên nhân vật và username.
        }
        JSONArray results = new JSONArray();

        String sql = "SELECT p.id, p.name, p.gender, p.account_id, a.username, a.ban, a.is_admin, a.tongnap "
                   + "FROM player p INNER JOIN account a ON p.account_id = a.id "
                   + "WHERE p.id = ? OR p.name LIKE ? OR a.username LIKE ? "
                   + "LIMIT 50";

        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, exactPlayerId);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    long pId = rs.getLong("id");
                    obj.put("id", pId);
                    obj.put("name", rs.getString("name"));
                    obj.put("gender", rs.getInt("gender"));
                    obj.put("gender_name", getGenderName((byte) rs.getInt("gender")));
                    obj.put("account_id", rs.getInt("account_id"));
                    obj.put("username", rs.getString("username"));
                    obj.put("ban", rs.getInt("ban"));
                    obj.put("is_admin", rs.getInt("is_admin"));
                    obj.put("tongnap", rs.getLong("tongnap"));

                    Player onlinePl = Client.gI().getPlayer(pId);
                    if (onlinePl != null) {
                        appendRuntimeFields(obj, onlinePl);
                    } else {
                        obj.put("is_online", false);
                        obj.put("map_id", -1);
                        obj.put("map_name", "Offline");
                        obj.put("zone_id", -1);
                    }
                    results.add(obj);
                }
            }
        } catch (SQLException e) {
            JsonResponse.serverError(exchange, "Lỗi tìm kiếm: " + e.getMessage());
            return;
        }

        JSONObject res = new JSONObject();
        res.put("total", results.size());
        res.put("results", results);
        JsonResponse.ok(exchange, res);
    }

    public static void handlePlayerDetail(HttpExchange exchange, AdminUser user, long playerId) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        Player onlinePl = Client.gI().getPlayer(playerId);
        JSONObject detail = new JSONObject();

        if (onlinePl != null) {
            detail.put("id", onlinePl.id);
            detail.put("name", onlinePl.name);
            detail.put("gender", (int) onlinePl.gender);
            detail.put("gender_name", getGenderName(onlinePl.gender));
            detail.put("is_online", true);
            detail.put("account_id", onlinePl.getSession() != null ? onlinePl.getSession().userId : -1);
            detail.put("username", onlinePl.getSession() != null ? onlinePl.getSession().uu : "");
            detail.put("ban", 0);
            detail.put("is_admin", onlinePl.getSession() != null && onlinePl.getSession().isAdmin ? 1 : 0);
            detail.put("ip_address", onlinePl.getSession() != null ? onlinePl.getSession().getIP() : "Unknown");

            if (onlinePl.nPoint != null) {
                detail.put("power", onlinePl.nPoint.power);
                detail.put("tiem_nang", onlinePl.nPoint.tiemNang);
                detail.put("hp", onlinePl.nPoint.hp);
                detail.put("hp_max", onlinePl.nPoint.hpMax);
                detail.put("mp", onlinePl.nPoint.mp);
                detail.put("mp_max", onlinePl.nPoint.mpMax);
                detail.put("dame", onlinePl.nPoint.dame);
                detail.put("def", onlinePl.nPoint.def);
                detail.put("crit", onlinePl.nPoint.crit);
            }

            if (onlinePl.inventory != null) {
                detail.put("gold", onlinePl.inventory.gold);
                detail.put("gem", onlinePl.inventory.gem);
                detail.put("ruby", onlinePl.inventory.ruby);

                JSONArray bagItems = new JSONArray();
                for (Item item : onlinePl.inventory.itemsBag) {
                    if (item != null && item.isNotNullItem()) {
                        JSONObject it = new JSONObject();
                        it.put("id", item.template.id);
                        it.put("name", item.template.name);
                        it.put("quantity", item.quantity);
                        it.put("icon_id", item.template.iconID);
                        bagItems.add(it);
                    }
                }
                detail.put("items_bag", bagItems);
            }

            if (onlinePl.zone != null && onlinePl.zone.map != null) {
                detail.put("map_id", onlinePl.zone.map.mapId);
                detail.put("map_name", onlinePl.zone.map.mapName);
                detail.put("zone_id", onlinePl.zone.zoneId);
            }

            if (onlinePl.pet != null) {
                JSONObject petObj = new JSONObject();
                petObj.put("name", onlinePl.pet.name);
                petObj.put("gender", (int) onlinePl.pet.gender);
                if (onlinePl.pet.nPoint != null) {
                    petObj.put("power", onlinePl.pet.nPoint.power);
                    petObj.put("hp", onlinePl.pet.nPoint.hp);
                    petObj.put("hp_max", onlinePl.pet.nPoint.hpMax);
                    petObj.put("dame", onlinePl.pet.nPoint.dame);
                }
                detail.put("pet", petObj);
            }
        } else {
            // Read from Database
            String sql = "SELECT p.*, a.username, a.ban, a.is_admin FROM player p INNER JOIN account a ON p.account_id = a.id WHERE p.id = ? LIMIT 1";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JsonResponse.notFound(exchange, "Không tìm thấy người chơi có ID: " + playerId);
                        return;
                    }
                    detail.put("id", rs.getLong("id"));
                    detail.put("name", rs.getString("name"));
                    detail.put("gender", rs.getInt("gender"));
                    detail.put("gender_name", getGenderName((byte) rs.getInt("gender")));
                    detail.put("is_online", false);
                    detail.put("account_id", rs.getInt("account_id"));
                    detail.put("username", rs.getString("username"));
                    detail.put("ban", rs.getInt("ban"));
                    detail.put("is_admin", rs.getInt("is_admin"));
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
        }

        JsonResponse.ok(exchange, detail);
    }

    public static void handlePlayerAction(HttpExchange exchange, AdminUser user, long playerId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String action = (String) req.get("action");
        if (action == null || action.trim().isEmpty()) {
            JsonResponse.badRequest(exchange, "Thiếu trường action");
            return;
        }

        Player onlinePl = Client.gI().getPlayer(playerId);
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        switch (action.toLowerCase()) {
            case "kick": {
                if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                    JsonResponse.forbidden(exchange, "Cần quyền ADMIN để Kick");
                    return;
                }
                if (onlinePl != null && onlinePl.getSession() != null) {
                    Client.gI().kickSession(onlinePl.getSession());
                    AuditLogService.gI().log(user, "KICK_PLAYER", "PLAYER", (int) playerId, "{\"name\":\"" + onlinePl.name + "\"}", clientIp);
                    JsonResponse.ok(exchange, null, "Đã ngắt kết nối nhân vật " + onlinePl.name);
                } else {
                    JsonResponse.badRequest(exchange, "Người chơi hiện không online");
                }
                break;
            }
            case "ban": {
                if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                    JsonResponse.forbidden(exchange, "Cần quyền ADMIN để khóa tài khoản");
                    return;
                }
                try (Connection con = LocalManager.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE account a INNER JOIN player p ON p.account_id = a.id SET a.ban = 1 WHERE p.id = ?")) {
                    ps.setLong(1, playerId);
                    int updated = ps.executeUpdate();
                    if (findBanStatus(con, playerId) == null) {
                        JsonResponse.notFound(exchange, "Không tìm thấy người chơi có ID: " + playerId);
                        return;
                    }
                    if (onlinePl != null && onlinePl.getSession() != null) {
                        Client.gI().kickSession(onlinePl.getSession());
                    }
                    AuditLogService.gI().log(user, "BAN_PLAYER", "PLAYER", (int) playerId, "{}", clientIp);
                    JsonResponse.ok(exchange, null, updated > 0
                            ? "Đã khóa tài khoản thành công"
                            : "Tài khoản đã bị khóa từ trước");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
                break;
            }
            case "unban": {
                if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                    JsonResponse.forbidden(exchange, "Cần quyền ADMIN để mở khóa");
                    return;
                }
                try (Connection con = LocalManager.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE account a INNER JOIN player p ON p.account_id = a.id SET a.ban = 0 WHERE p.id = ?")) {
                    ps.setLong(1, playerId);
                    int updated = ps.executeUpdate();
                    if (findBanStatus(con, playerId) == null) {
                        JsonResponse.notFound(exchange, "Không tìm thấy người chơi có ID: " + playerId);
                        return;
                    }
                    AuditLogService.gI().log(user, "UNBAN_PLAYER", "PLAYER", (int) playerId, "{}", clientIp);
                    JsonResponse.ok(exchange, null, updated > 0
                            ? "Đã mở khóa tài khoản thành công"
                            : "Tài khoản đã được mở khóa từ trước");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
                break;
            }
            case "teleport": {
                if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                    JsonResponse.forbidden(exchange, "Cần quyền ADMIN để dịch chuyển");
                    return;
                }
                if (onlinePl == null) {
                    JsonResponse.badRequest(exchange, "Người chơi hiện không online");
                    return;
                }
                final int mapId;
                final int zoneId;
                final int x;
                final int y;
                try {
                    mapId = readInt(req, "map_id", 21 + onlinePl.gender);
                    zoneId = readInt(req, "zone_id", 0);
                    x = readInt(req, "x", 200);
                    y = readInt(req, "y", 336);
                } catch (IllegalArgumentException e) {
                    JsonResponse.badRequest(exchange, e.getMessage());
                    return;
                }
                if (mapId < 0 || zoneId < 0 || x < 0 || y < 0) {
                    JsonResponse.badRequest(exchange, "Map, khu vực và tọa độ không được âm");
                    return;
                }

                Zone targetZone;
                try {
                    targetZone = MapService.gI().getZoneByMapIDAndZoneID(mapId, zoneId);
                } catch (RuntimeException e) {
                    targetZone = null;
                }
                if (targetZone == null || targetZone.map == null
                        || targetZone.map.mapId != mapId || targetZone.zoneId != zoneId) {
                    JsonResponse.badRequest(exchange, "Map hoặc khu vực không tồn tại/không thể tham gia");
                    return;
                }

                ChangeMapService.gI().changeMap(onlinePl, targetZone, x, y);
                if (onlinePl.zone != targetZone) {
                    JsonResponse.badRequest(exchange, "Server game đã từ chối dịch chuyển nhân vật");
                    return;
                }
                AuditLogService.gI().log(user, "TELEPORT_PLAYER", "PLAYER", (int) playerId, "{\"map\":" + mapId + ",\"zone\":" + zoneId + "}", clientIp);
                JsonResponse.ok(exchange, null, "Đã dịch chuyển " + onlinePl.name + " tới map " + mapId + ", khu " + zoneId);
                break;
            }
            case "add_currency": {
                if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                    JsonResponse.forbidden(exchange, "Cần quyền ADMIN để cộng tiền/vật phẩm");
                    return;
                }
                if (onlinePl == null || onlinePl.inventory == null || onlinePl.getSession() == null) {
                    JsonResponse.badRequest(exchange, "Người chơi không online. Hãy dùng chức năng Gửi Quà Hòm Thư để trao thưởng.");
                    return;
                }

                final long gold;
                final long gemValue;
                final long rubyValue;
                try {
                    gold = readLong(req, "gold", 0);
                    gemValue = readLong(req, "gem", 0);
                    rubyValue = readLong(req, "ruby", 0);
                } catch (IllegalArgumentException e) {
                    JsonResponse.badRequest(exchange, e.getMessage());
                    return;
                }
                if (gold < 0 || gemValue < 0 || rubyValue < 0) {
                    JsonResponse.badRequest(exchange, "Số tiền cộng thêm không được âm");
                    return;
                }
                if (gold == 0 && gemValue == 0 && rubyValue == 0) {
                    JsonResponse.badRequest(exchange, "Phải nhập ít nhất một loại tiền lớn hơn 0");
                    return;
                }
                if (gemValue > Integer.MAX_VALUE || rubyValue > Integer.MAX_VALUE) {
                    JsonResponse.badRequest(exchange, "Số ngọc vượt giới hạn cho phép");
                    return;
                }

                synchronized (onlinePl.getSession()) {
                    if (gold > Inventory.LIMIT_GOLD - onlinePl.inventory.gold
                            || gemValue > Integer.MAX_VALUE - (long) onlinePl.inventory.gem
                            || rubyValue > Integer.MAX_VALUE - (long) onlinePl.inventory.ruby) {
                        JsonResponse.badRequest(exchange, "Số dư sau khi cộng vượt giới hạn cho phép");
                        return;
                    }

                    long oldGold = onlinePl.inventory.gold;
                    int oldGem = onlinePl.inventory.gem;
                    int oldRuby = onlinePl.inventory.ruby;
                    onlinePl.inventory.gold += gold;
                    onlinePl.inventory.gem += (int) gemValue;
                    onlinePl.inventory.ruby += (int) rubyValue;

                    if (!PlayerDAO.updatePlayer(onlinePl)) {
                        onlinePl.inventory.gold = oldGold;
                        onlinePl.inventory.gem = oldGem;
                        onlinePl.inventory.ruby = oldRuby;
                        JsonResponse.serverError(exchange, "Không thể lưu số dư người chơi vào database");
                        return;
                    }
                    Service.gI().sendMoney(onlinePl);
                }
                AuditLogService.gI().log(user, "ADD_CURRENCY_ONLINE", "PLAYER", (int) playerId, "{\"gold\":" + gold + ",\"gem\":" + gemValue + ",\"ruby\":" + rubyValue + "}", clientIp);
                JsonResponse.ok(exchange, null, "Đã cộng và lưu tiền tệ cho " + onlinePl.name);
                break;
            }
            default:
                JsonResponse.badRequest(exchange, "Action không được hỗ trợ: " + action);
                break;
        }
    }

    private static void appendRuntimeFields(JSONObject target, Player player) {
        target.put("id", player.id);
        target.put("name", player.name);
        target.put("gender", (int) player.gender);
        target.put("gender_name", getGenderName(player.gender));
        target.put("is_online", true);

        if (player.nPoint != null) {
            target.put("power", player.nPoint.power);
            target.put("tiem_nang", player.nPoint.tiemNang);
            target.put("hp", player.nPoint.hp);
            target.put("hp_max", player.nPoint.hpMax);
            target.put("mp", player.nPoint.mp);
            target.put("mp_max", player.nPoint.mpMax);
        }
        if (player.inventory != null) {
            target.put("gold", player.inventory.gold);
            target.put("gem", player.inventory.gem);
            target.put("ruby", player.inventory.ruby);
        }
        if (player.zone != null && player.zone.map != null) {
            target.put("map_id", player.zone.map.mapId);
            target.put("map_name", player.zone.map.mapName);
            target.put("zone_id", player.zone.zoneId);
            target.put("x", player.location != null ? player.location.x : 0);
            target.put("y", player.location != null ? player.location.y : 0);
        }

        target.put("account_id", player.getSession() != null ? player.getSession().userId : -1);
        target.put("username", player.getSession() != null ? player.getSession().uu : "");
        target.put("ban", 0);
        target.put("is_admin", player.getSession() != null && player.getSession().isAdmin ? 1 : 0);
        target.put("ip_address", player.getSession() != null ? player.getSession().getIP() : "Unknown");
    }

    private static Integer findBanStatus(Connection connection, long playerId) throws SQLException {
        String sql = "SELECT a.ban FROM account a INNER JOIN player p ON p.account_id = a.id WHERE p.id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("ban") : null;
            }
        }
    }

    private static int readInt(JSONObject request, String key, int defaultValue) {
        long value = readLong(request, key, defaultValue);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Giá trị " + key + " vượt giới hạn số nguyên");
        }
        return (int) value;
    }

    private static long readLong(JSONObject request, String key, long defaultValue) {
        Object value = request.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Giá trị " + key + " phải là số");
        }
        return ((Number) value).longValue();
    }

    private static String getGenderName(byte gender) {
        return switch (gender) {
            case 0 -> "Trái Đất";
            case 1 -> "Namếc";
            case 2 -> "Xayda";
            default -> "Không rõ";
        };
    }
}
