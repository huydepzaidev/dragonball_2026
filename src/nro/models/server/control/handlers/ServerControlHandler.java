package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import nro.models.data.LocalManager;
import nro.models.server.AutoMaintenance;
import nro.models.server.Client;
import nro.models.server.GameConfigService;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.UserRole;
import nro.models.server.control.http.JsonResponse;
import nro.models.services.ChatGlobalService;
import nro.models.services.Service;
import org.json.simple.JSONObject;

public final class ServerControlHandler {

    public static void handleMaintenance(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để kích hoạt bảo trì");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String type = (String) req.getOrDefault("type", "countdown");
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        if ("immediate".equalsIgnoreCase(type)) {
            AuditLogService.gI().log(user, "MAINTENANCE_IMMEDIATE", "SERVER", null, "{}", clientIp);
            JsonResponse.ok(exchange, null, "Bắt đầu bảo trì ngay lập tức");
            Maintenance.gI().startImmediately();
        } else {
            int seconds = ((Number) req.getOrDefault("seconds", 60)).intValue();
            if (seconds < 5) seconds = 5;
            Maintenance.gI().startSeconds(seconds);
            AuditLogService.gI().log(user, "MAINTENANCE_COUNTDOWN", "SERVER", null, "{\"seconds\":" + seconds + "}", clientIp);
            JsonResponse.ok(exchange, null, "Đã bắt đầu đếm ngược bảo trì " + seconds + " giây");
        }
    }

    public static void handleAdminOnly(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để thay đổi chế độ Admin-Only");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        boolean enable = (Boolean) req.getOrDefault("enabled", true);
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        if (enable) {
            Maintenance.gI().enterAdminOnlyMode();
            AuditLogService.gI().log(user, "ENABLE_ADMIN_ONLY", "SERVER", null, "{}", clientIp);
            JsonResponse.ok(exchange, null, "Đã bật chế độ Admin-Only (Người chơi thường đã được ngắt kết nối)");
        } else {
            Maintenance.gI().leaveAdminOnlyMode();
            AuditLogService.gI().log(user, "DISABLE_ADMIN_ONLY", "SERVER", null, "{}", clientIp);
            JsonResponse.ok(exchange, null, "Đã tắt chế độ Admin-Only (Mọi người chơi có thể đăng nhập)");
        }
    }

    public static void handleRates(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để chỉnh tỉ lệ máy chủ");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        Integer expRate = null;
        Integer dropRate = null;

        if (req.containsKey("exp_rate")) {
            Object value = req.get("exp_rate");
            if (!(value instanceof Number)) {
                JsonResponse.badRequest(exchange, "EXP rate phải là số");
                return;
            }
            expRate = ((Number) value).intValue();
            if (expRate < 1 || expRate > 100) {
                JsonResponse.badRequest(exchange, "EXP rate phải từ 1 đến 100");
                return;
            }
        }

        if (req.containsKey("drop_rate_percent")) {
            Object value = req.get("drop_rate_percent");
            if (!(value instanceof Number)) {
                JsonResponse.badRequest(exchange, "Drop rate phải là số");
                return;
            }
            dropRate = ((Number) value).intValue();
            if (dropRate < 0 || dropRate > 1000) {
                JsonResponse.badRequest(exchange, "Drop rate phải từ 0% đến 1000%");
                return;
            }
        }

        if (expRate == null && dropRate == null) {
            JsonResponse.badRequest(exchange, "Cần ít nhất một tỉ lệ để cập nhật");
            return;
        }

        String sql;
        if (expRate != null && dropRate != null) {
            sql = "UPDATE game_server_config SET exp_rate = ?, drop_rate_percent = ?, updated_by = ? WHERE id = 1";
        } else if (expRate != null) {
            sql = "UPDATE game_server_config SET exp_rate = ?, updated_by = ? WHERE id = 1";
        } else {
            sql = "UPDATE game_server_config SET drop_rate_percent = ?, updated_by = ? WHERE id = 1";
        }

        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int parameter = 1;
            if (expRate != null) {
                ps.setInt(parameter++, expRate);
            }
            if (dropRate != null) {
                ps.setInt(parameter++, dropRate);
            }
            ps.setString(parameter, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            JsonResponse.serverError(exchange, "Không thể lưu tỉ lệ vào cơ sở dữ liệu");
            return;
        }

        if (!GameConfigService.gI().loadNow()) {
            JsonResponse.serverError(exchange, "Đã lưu tỉ lệ nhưng không thể nạp cấu hình runtime");
            return;
        }

        if (expRate != null) {
            String message = "Đã tăng EXP toàn server lên x" + Manager.RATE_EXP_SERVER + "!";
            ChatGlobalService.gI().chatAdmin(message);
            Service.gI().sendThongBaoAllPlayer("Admin: " + message);
        }

        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (expRate != null) {
            AuditLogService.gI().log(user, "SET_EXP_RATE", "CONFIG", null, "{\"exp_rate\":" + expRate + "}", clientIp);
        }
        if (dropRate != null) {
            AuditLogService.gI().log(user, "SET_DROP_RATE", "CONFIG", null, "{\"drop_rate_percent\":" + dropRate + "}", clientIp);
        }

        JSONObject data = new JSONObject();
        data.put("exp_rate", Manager.RATE_EXP_SERVER);
        data.put("drop_rate_percent", GameConfigService.gI().getDropRatePercent());
        JsonResponse.ok(exchange, data, "Đã cập nhật tỉ lệ máy chủ thành công");
    }

    public static void handleBroadcast(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để phát thông báo");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String message = (String) req.get("message");
        if (message == null || message.trim().isEmpty()) {
            JsonResponse.badRequest(exchange, "Nội dung thông báo không được để trống");
            return;
        }

        String trimmedMessage = message.trim();
        int targetedPlayers = Client.gI().getPlayers().size();
        Service.gI().sendMessageServer(trimmedMessage);
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        JSONObject detail = new JSONObject();
        detail.put("message", trimmedMessage);
        detail.put("targeted_players", targetedPlayers);
        AuditLogService.gI().log(user, "BROADCAST_MESSAGE", "SERVER", null, detail.toJSONString(), clientIp);

        JSONObject data = new JSONObject();
        data.put("channel", "SERVER_MARQUEE");
        data.put("targeted_players", targetedPlayers);
        String responseMessage = targetedPlayers > 0
                ? "Đã phát thông báo chạy chữ tới " + targetedPlayers + " người chơi"
                : "Đã xử lý thông báo nhưng hiện không có người chơi trực tuyến";
        JsonResponse.ok(exchange, data, responseMessage);
    }

    public static void handleAutoMaintenance(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để cấu hình tự động bảo trì");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        boolean enable = (Boolean) req.getOrDefault("enabled", true);
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        AutoMaintenance.AutoMaintenance = enable;
        updateConfigDb("auto_maintenance_enabled", enable ? 1 : 0, user.getUsername());
        AuditLogService.gI().log(user, "SET_AUTO_MAINTENANCE", "CONFIG", null, "{\"enabled\":" + enable + "}", clientIp);
        JsonResponse.ok(exchange, null, enable ? "Đã bật tự động bảo trì" : "Đã tắt tự động bảo trì");
    }

    public static void handleCommand(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để gửi lệnh Server");
            return;
        }

        JSONObject req = AuthHandler.parseRequestBody(exchange);
        if (req == null) {
            JsonResponse.badRequest(exchange, "Dữ liệu JSON không hợp lệ");
            return;
        }

        String cmd = (String) req.get("command");
        if (cmd == null || cmd.trim().isEmpty()) {
            JsonResponse.badRequest(exchange, "Thiếu trường command");
            return;
        }

        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        String trimmed = cmd.trim().toLowerCase();

        switch (trimmed) {
            case "bt":
                Maintenance.gI().startSeconds(5);
                AuditLogService.gI().log(user, "CMD_MAINTENANCE_5S", "SERVER", null, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã kích hoạt bảo trì 5s");
                break;
            case "bat":
                AutoMaintenance.AutoMaintenance = true;
                AuditLogService.gI().log(user, "CMD_AUTO_MAINTENANCE_ON", "SERVER", null, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã bật chế độ tự động bảo trì");
                break;
            case "tat":
                AutoMaintenance.AutoMaintenance = false;
                AuditLogService.gI().log(user, "CMD_AUTO_MAINTENANCE_OFF", "SERVER", null, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã tắt chế độ tự động bảo trì");
                break;
            default:
                JsonResponse.badRequest(exchange, "Lệnh không nằm trong whitelist cho phép: " + trimmed);
                break;
        }
    }

    private static void updateConfigDb(String field, int value, String adminUser) {
        String sql = "UPDATE game_server_config SET " + field + " = ?, updated_by = ? WHERE id = 1";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.setString(2, adminUser);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
