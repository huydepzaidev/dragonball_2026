package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.AuthManager;
import nro.models.server.control.auth.UserRole;
import nro.models.server.control.http.JsonResponse;
import nro.models.services.Service;
import nro.models.utils.PasswordHasher;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public final class WebAdminHandler {

    // 1. Giftcodes
    public static void handleGiftcodes(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONArray list = new JSONArray();
            String sql = "SELECT id, code, count_left, detail, expired FROM giftcode ORDER BY id DESC LIMIT 100";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject g = new JSONObject();
                    g.put("id", rs.getInt("id"));
                    g.put("code", rs.getString("code"));
                    g.put("count_left", rs.getInt("count_left"));
                    g.put("detail", rs.getString("detail"));
                    g.put("expired", rs.getString("expired"));
                    list.add(g);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JSONObject res = new JSONObject();
            res.put("total", list.size());
            res.put("giftcodes", list);
            JsonResponse.ok(exchange, res);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để tạo giftcode");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            if (req == null) {
                JsonResponse.badRequest(exchange, "JSON không hợp lệ");
                return;
            }
            String code = (String) req.get("code");
            int countLeft = ((Number) req.getOrDefault("count_left", -1)).intValue();
            String detail = (String) req.getOrDefault("detail", "[]");
            String expired = (String) req.getOrDefault("expired", "2030-12-31 23:59:59");

            if (code == null || code.trim().isEmpty()) {
                JsonResponse.badRequest(exchange, "Mã code không được để trống");
                return;
            }

            String sql = "INSERT INTO giftcode (code, count_left, detail, expired) VALUES (?, ?, ?, ?)";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code.trim());
                ps.setInt(2, countLeft);
                ps.setString(3, detail);
                ps.setString(4, expired);
                ps.executeUpdate();

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                AuditLogService.gI().log(user, "CREATE_GIFTCODE", "GIFTCODE", null, "{\"code\":\"" + code + "\"}", clientIp);
                JsonResponse.ok(exchange, null, "Đã tạo mã Giftcode " + code + " thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để xóa giftcode");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            int id = ((Number) req.getOrDefault("id", -1)).intValue();
            if (id == -1) {
                JsonResponse.badRequest(exchange, "Thiếu ID giftcode");
                return;
            }
            String sql = "DELETE FROM giftcode WHERE id = ?";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                AuditLogService.gI().log(user, "DELETE_GIFTCODE", "GIFTCODE", id, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã xóa mã Giftcode thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 2. Mailboxes
    public static void handleMailboxes(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONArray list = new JSONArray();
            String sql = "SELECT m.id, m.account_id, m.player_id, COALESCE(p.name, 'Tất cả') AS player_name, "
                       + "m.title, m.content, m.items_json, m.status, m.created_at "
                       + "FROM player_mailbox m LEFT JOIN player p ON m.player_id = p.id "
                       + "ORDER BY m.id DESC LIMIT 100";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject m = new JSONObject();
                    m.put("id", rs.getInt("id"));
                    m.put("account_id", rs.getInt("account_id"));
                    m.put("player_id", rs.getInt("player_id"));
                    m.put("player_name", rs.getString("player_name"));
                    m.put("title", rs.getString("title"));
                    m.put("message", rs.getString("content"));
                    m.put("rewards_json", rs.getString("items_json"));
                    m.put("status", rs.getString("status"));
                    m.put("created_at", rs.getString("created_at"));
                    list.add(m);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JSONObject res = new JSONObject();
            res.put("total", list.size());
            res.put("mailboxes", list);
            JsonResponse.ok(exchange, res);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để gửi thư và phát quà");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            if (req == null) {
                JsonResponse.badRequest(exchange, "JSON không hợp lệ");
                return;
            }

            int playerId = ((Number) req.getOrDefault("player_id", 0)).intValue();
            String title = (String) req.getOrDefault("title", "Quà tặng từ Admin");
            String message = (String) req.getOrDefault("message", "Chúc bạn chơi game vui vẻ!");
            String rewardsJson = (String) req.getOrDefault("rewards_json", "[]");
            boolean sendAll = (Boolean) req.getOrDefault("send_all", false);

            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

            if (sendAll) {
                String sql = "INSERT INTO player_mailbox (account_id, player_id, title, content, items_json, status) "
                           + "SELECT account_id, id, ?, ?, ?, 'PENDING' FROM player";
                try (Connection con = LocalManager.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setString(2, message);
                    ps.setString(3, rewardsJson);
                    int count = ps.executeUpdate();

                    AuditLogService.gI().log(user, "SEND_MAILBOX_ALL", "MAILBOX", null, "{\"count\":" + count + "}", clientIp);
                    JsonResponse.ok(exchange, null, "Đã gửi quà tới toàn bộ " + count + " người chơi máy chủ");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
            } else {
                if (playerId <= 0) {
                    JsonResponse.badRequest(exchange, "Vui lòng chọn Player ID hợp lệ");
                    return;
                }
                String sql = "INSERT INTO player_mailbox (account_id, player_id, title, content, items_json, status) "
                           + "VALUES ((SELECT account_id FROM player WHERE id = ? LIMIT 1), ?, ?, ?, ?, 'PENDING')";
                try (Connection con = LocalManager.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, playerId);
                    ps.setString(3, title);
                    ps.setString(4, message);
                    ps.setString(5, rewardsJson);
                    ps.executeUpdate();

                    AuditLogService.gI().log(user, "SEND_MAILBOX_SINGLE", "MAILBOX", playerId, "{}", clientIp);
                    JsonResponse.ok(exchange, null, "Đã gửi quà tới người chơi ID: " + playerId + " thành công");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
            }
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để xóa hòm thư");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            int id = ((Number) req.getOrDefault("id", -1)).intValue();
            if (id <= 0) {
                JsonResponse.badRequest(exchange, "Thiếu ID hòm thư");
                return;
            }
            String sql = "DELETE FROM player_mailbox WHERE id = ?";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                AuditLogService.gI().log(user, "DELETE_MAILBOX", "MAILBOX", id, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã xóa thư thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 3. Accounts
    public static void handleAccounts(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONArray list = new JSONArray();
            String sql = "SELECT a.id, a.username, a.is_admin, a.ban, a.active, a.danaptong, a.vnd, "
                       + "a.create_time, a.last_time_login, COALESCE(p.name, 'Chưa tạo NV') AS player_name "
                       + "FROM account a LEFT JOIN player p ON a.id = p.account_id "
                       + "ORDER BY a.id DESC LIMIT 100";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject a = new JSONObject();
                    a.put("id", rs.getInt("id"));
                    a.put("username", rs.getString("username"));
                    a.put("is_admin", rs.getInt("is_admin"));
                    a.put("ban", rs.getInt("ban"));
                    a.put("active", rs.getInt("active"));
                    a.put("danaptong", rs.getInt("danaptong"));
                    a.put("vnd", rs.getInt("vnd"));
                    a.put("create_time", rs.getString("create_time"));
                    a.put("last_time_login", rs.getString("last_time_login"));
                    a.put("player_name", rs.getString("player_name"));
                    list.add(a);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JSONObject res = new JSONObject();
            res.put("total", list.size());
            res.put("accounts", list);
            JsonResponse.ok(exchange, res);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để chỉnh sửa tài khoản");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            if (req == null) {
                JsonResponse.badRequest(exchange, "JSON không hợp lệ");
                return;
            }
            int accountId = ((Number) req.getOrDefault("id", -1)).intValue();
            if (accountId <= 0) {
                JsonResponse.badRequest(exchange, "Thiếu ID tài khoản");
                return;
            }

            String password = (String) req.get("password");
            Number isAdmin = (Number) req.get("is_admin");
            Number danaptong = (Number) req.get("danaptong");
            Number vnd = (Number) req.get("vnd");
            Number ban = (Number) req.get("ban");

            StringBuilder sql = new StringBuilder("UPDATE account SET id = id");
            if (password != null && !password.trim().isEmpty()) sql.append(", password = ?");
            if (isAdmin != null) sql.append(", is_admin = ?");
            if (danaptong != null) sql.append(", danaptong = ?");
            if (vnd != null) sql.append(", vnd = ?");
            if (ban != null) sql.append(", ban = ?");
            sql.append(" WHERE id = ?");

            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql.toString())) {
                int idx = 1;
                if (password != null && !password.trim().isEmpty()) {
                    ps.setString(idx++, PasswordHasher.hashPassword(password.trim()));
                }
                if (isAdmin != null) ps.setInt(idx++, isAdmin.intValue());
                if (danaptong != null) ps.setInt(idx++, danaptong.intValue());
                if (vnd != null) ps.setInt(idx++, vnd.intValue());
                if (ban != null) ps.setInt(idx++, ban.intValue());
                ps.setInt(idx, accountId);
                ps.executeUpdate();

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                JSONObject logReq = new JSONObject();
                logReq.putAll(req);
                if (logReq.containsKey("password")) {
                    logReq.put("password", "******");
                }
                AuditLogService.gI().log(user, "UPDATE_ACCOUNT", "ACCOUNT", accountId, logReq.toJSONString(), clientIp);
                JsonResponse.ok(exchange, null, "Cập nhật tài khoản ID " + accountId + " thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 4. Transactions
    public static void handleTransactions(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONArray list = new JSONArray();
        String sql = "SELECT t.id, t.user_id, a.username, t.pin, t.seri, t.amount, "
                   + "COALESCE(t.final_credited_amount, t.amount) AS final_amount, "
                   + "COALESCE(t.is_credited, 0) AS is_credited, t.status, t.created_at "
                   + "FROM payments t LEFT JOIN account a ON t.user_id = a.id "
                   + "ORDER BY t.id DESC LIMIT 100";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject t = new JSONObject();
                t.put("id", rs.getInt("id"));
                t.put("user_id", rs.getInt("user_id"));
                t.put("username", rs.getString("username"));
                t.put("pin", rs.getString("pin"));
                t.put("seri", rs.getString("seri"));
                t.put("amount", rs.getInt("amount"));
                t.put("final_credited_amount", rs.getInt("final_amount"));
                t.put("is_credited", rs.getInt("is_credited"));
                t.put("status", rs.getInt("status"));
                t.put("created_at", rs.getString("created_at"));
                list.add(t);
            }
        } catch (SQLException e) {
            JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            return;
        }

        JSONObject res = new JSONObject();
        res.put("total", list.size());
        res.put("transactions", list);
        JsonResponse.ok(exchange, res);
    }

    // 5. Posts
    public static void handlePosts(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONArray list = new JSONArray();
            String sql = "SELECT p.id, p.tieude, p.noidung, p.username, p.ghimbai, p.khoa, p.created_at "
                       + "FROM posts p ORDER BY p.ghimbai DESC, p.id DESC LIMIT 100";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject p = new JSONObject();
                    p.put("id", rs.getInt("id"));
                    p.put("tieude", rs.getString("tieude"));
                    p.put("noidung", rs.getString("noidung"));
                    p.put("username", rs.getString("username"));
                    p.put("ghimbai", rs.getInt("ghimbai"));
                    p.put("khoa", rs.getInt("khoa"));
                    p.put("created_at", rs.getString("created_at"));
                    list.add(p);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JSONObject res = new JSONObject();
            res.put("total", list.size());
            res.put("posts", list);
            JsonResponse.ok(exchange, res);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để tạo bài viết");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            int id = ((Number) req.getOrDefault("id", -1)).intValue();
            String tieude = (String) req.get("tieude");
            String noidung = (String) req.get("noidung");
            int ghimbai = ((Number) req.getOrDefault("ghimbai", 0)).intValue();
            int khoa = ((Number) req.getOrDefault("khoa", 0)).intValue();

            if (tieude == null || tieude.trim().isEmpty()) {
                JsonResponse.badRequest(exchange, "Tiêu đề không được để trống");
                return;
            }

            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

            if (id > 0) {
                String sql = "UPDATE posts SET tieude = ?, noidung = ?, ghimbai = ?, khoa = ? WHERE id = ?";
                try (Connection con = LocalManager.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, tieude);
                    ps.setString(2, noidung);
                    ps.setInt(3, ghimbai);
                    ps.setInt(4, khoa);
                    ps.setInt(5, id);
                    ps.executeUpdate();

                    AuditLogService.gI().log(user, "UPDATE_POST", "POST", id, "{}", clientIp);
                    JsonResponse.ok(exchange, null, "Cập nhật bài viết thành công");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
            } else {
                String sql = "INSERT INTO posts (tieude, noidung, username, ghimbai, khoa) VALUES (?, ?, ?, ?, ?)";
                try (Connection con = LocalManager.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, tieude);
                    ps.setString(2, noidung);
                    ps.setString(3, user.getUsername());
                    ps.setInt(4, ghimbai);
                    ps.setInt(5, khoa);
                    ps.executeUpdate();

                    AuditLogService.gI().log(user, "CREATE_POST", "POST", null, "{}", clientIp);
                    JsonResponse.ok(exchange, null, "Đã đăng bài viết mới thành công");
                } catch (SQLException e) {
                    JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                }
            }
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để xóa bài viết");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            int id = ((Number) req.getOrDefault("id", -1)).intValue();
            if (id <= 0) {
                JsonResponse.badRequest(exchange, "Thiếu ID bài viết");
                return;
            }
            String sql = "DELETE FROM posts WHERE id = ?";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                AuditLogService.gI().log(user, "DELETE_POST", "POST", id, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã xóa bài viết thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 6. Settings
    public static void handleSettings(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONObject settings = new JSONObject();
            String sql = "SELECT Title, ServerName, Fanpage, `Group`, Zalo, EmailSupport, AccountBank, NumberBank, NameBank, Android, Windows, IPhone, Java FROM settings LIMIT 1";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    settings.put("title", rs.getString("Title"));
                    settings.put("server_name", rs.getString("ServerName"));
                    settings.put("fanpage", rs.getString("Fanpage"));
                    settings.put("group", rs.getString("Group"));
                    settings.put("zalo", rs.getString("Zalo"));
                    settings.put("email_support", rs.getString("EmailSupport"));
                    settings.put("account_bank", rs.getString("AccountBank"));
                    settings.put("number_bank", rs.getString("NumberBank"));
                    settings.put("name_bank", rs.getString("NameBank"));
                    settings.put("android", rs.getString("Android"));
                    settings.put("windows", rs.getString("Windows"));
                    settings.put("iphone", rs.getString("IPhone"));
                    settings.put("java", rs.getString("Java"));
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JsonResponse.ok(exchange, settings);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để chỉnh sửa cài đặt website");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            if (req == null) {
                JsonResponse.badRequest(exchange, "JSON không hợp lệ");
                return;
            }

            String title = (String) req.getOrDefault("title", "Ngọc Rồng Online");
            String serverName = (String) req.getOrDefault("server_name", "Ngọc Rồng");
            String fanpage = (String) req.getOrDefault("fanpage", "");
            String group = (String) req.getOrDefault("group", "");
            String zalo = (String) req.getOrDefault("zalo", "");
            String email = (String) req.getOrDefault("email_support", "");
            String accountBank = (String) req.getOrDefault("account_bank", "");
            String numberBank = (String) req.getOrDefault("number_bank", "");
            String nameBank = (String) req.getOrDefault("name_bank", "");
            String android = (String) req.getOrDefault("android", "");
            String windows = (String) req.getOrDefault("windows", "");
            String iphone = (String) req.getOrDefault("iphone", "");
            String java = (String) req.getOrDefault("java", "");

            String sql = "UPDATE settings SET Title=?, ServerName=?, Fanpage=?, `Group`=?, Zalo=?, EmailSupport=?, AccountBank=?, NumberBank=?, NameBank=?, Android=?, Windows=?, IPhone=?, Java=?";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setString(2, serverName);
                ps.setString(3, fanpage);
                ps.setString(4, group);
                ps.setString(5, zalo);
                ps.setString(6, email);
                ps.setString(7, accountBank);
                ps.setString(8, numberBank);
                ps.setString(9, nameBank);
                ps.setString(10, android);
                ps.setString(11, windows);
                ps.setString(12, iphone);
                ps.setString(13, java);
                ps.executeUpdate();

                // Sync download links to adminpanel table
                try (PreparedStatement ps2 = con.prepareStatement("UPDATE adminpanel SET android=?, windows=?, iphone=?, java=?")) {
                    ps2.setString(1, android);
                    ps2.setString(2, windows);
                    ps2.setString(3, iphone);
                    ps2.setString(4, java);
                    ps2.executeUpdate();
                } catch (SQLException ignored) {}

                String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                AuditLogService.gI().log(user, "UPDATE_SETTINGS", "SETTINGS", null, "{}", clientIp);
                JsonResponse.ok(exchange, null, "Đã lưu cài đặt website & link tải game thành công");
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 7. Events (from events.php)
    public static void handleEvents(HttpExchange exchange, AdminUser user) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            JSONArray list = new JSONArray();
            String sql = "SELECT e.event_key, e.event_name, e.description, e.enabled, e.sort_order, e.updated_by, "
                       + "(SELECT COUNT(*) FROM game_event_item i WHERE i.event_key = e.event_key) AS item_count, "
                       + "(SELECT COUNT(*) FROM game_event_npc n WHERE n.event_key = e.event_key) AS npc_count, "
                       + "(SELECT COUNT(*) FROM game_event_boss b WHERE b.event_key = e.event_key) AS boss_count, "
                       + "(SELECT c.status FROM game_event_command c WHERE c.event_key = e.event_key AND c.status IN ('PENDING','PROCESSING') ORDER BY c.id DESC LIMIT 1) AS pending_status "
                       + "FROM game_event_catalog e ORDER BY e.sort_order, e.event_name";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject ev = new JSONObject();
                    ev.put("event_key", rs.getString("event_key"));
                    ev.put("event_name", rs.getString("event_name"));
                    ev.put("description", rs.getString("description"));
                    ev.put("enabled", rs.getInt("enabled") == 1);
                    ev.put("sort_order", rs.getInt("sort_order"));
                    ev.put("updated_by", rs.getString("updated_by"));
                    ev.put("item_count", rs.getInt("item_count"));
                    ev.put("npc_count", rs.getInt("npc_count"));
                    ev.put("boss_count", rs.getInt("boss_count"));
                    ev.put("pending_status", rs.getString("pending_status"));
                    list.add(ev);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
                return;
            }
            JSONObject res = new JSONObject();
            res.put("total", list.size());
            res.put("events", list);
            JsonResponse.ok(exchange, res);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if (!user.getRole().hasPermission(UserRole.ADMIN)) {
                JsonResponse.forbidden(exchange, "Cần quyền ADMIN để bật/tắt sự kiện");
                return;
            }
            JSONObject req = AuthHandler.parseRequestBody(exchange);
            if (req == null) {
                JsonResponse.badRequest(exchange, "JSON không hợp lệ");
                return;
            }
            String eventKey = (String) req.get("event_key");
            Boolean targetEnabled = (Boolean) req.get("enabled");

            if (eventKey == null || eventKey.trim().isEmpty() || targetEnabled == null) {
                JsonResponse.badRequest(exchange, "Thiếu event_key hoặc enabled");
                return;
            }

            try (Connection con = LocalManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // Check pending
                    try (PreparedStatement psCheck = con.prepareStatement(
                            "SELECT COUNT(*) FROM game_event_command WHERE event_key = ? AND status IN ('PENDING','PROCESSING')")) {
                        psCheck.setString(1, eventKey);
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                con.rollback();
                                JsonResponse.badRequest(exchange, "Sự kiện này đang có lệnh chờ game server xử lý");
                                return;
                            }
                        }
                    }

                    // Insert command
                    try (PreparedStatement psCmd = con.prepareStatement(
                            "INSERT INTO game_event_command (event_key, target_enabled, requested_by, status) VALUES (?, ?, ?, 'PENDING')")) {
                        psCmd.setString(1, eventKey);
                        psCmd.setInt(2, targetEnabled ? 1 : 0);
                        psCmd.setString(3, user.getUsername());
                        psCmd.executeUpdate();
                    }

                    // Update catalog updated_by
                    try (PreparedStatement psUpd = con.prepareStatement(
                            "UPDATE game_event_catalog SET updated_by = ? WHERE event_key = ?")) {
                        psUpd.setString(1, user.getUsername());
                        psUpd.setString(2, eventKey);
                        psUpd.executeUpdate();
                    }

                    con.commit();

                    String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
                    AuditLogService.gI().log(user, (targetEnabled ? "ENABLE_EVENT" : "DISABLE_EVENT"), "EVENT", null, "{\"event_key\":\"" + eventKey + "\"}", clientIp);
                    JsonResponse.ok(exchange, null, "Đã gửi lệnh " + (targetEnabled ? "BẬT" : "TẮT") + " sự kiện " + eventKey + ". Game server đang tự động reset dữ liệu và kích hoạt");
                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                } finally {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            }
            return;
        }

        JsonResponse.error(exchange, 405, "Method Not Allowed");
    }

    // 8. Audit Logs
    public static void handleLogs(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONArray list = new JSONArray();
        String sql = "SELECT id, admin_id, admin_username, action_name, target_type, target_id, detail_json, ip_address, created_at "
                   + "FROM admin_audit_log ORDER BY id DESC LIMIT 100";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject l = new JSONObject();
                l.put("id", rs.getLong("id"));
                l.put("admin_id", rs.getInt("admin_id"));
                l.put("admin_username", rs.getString("admin_username"));
                l.put("action_name", rs.getString("action_name"));
                l.put("target_type", rs.getString("target_type"));
                l.put("target_id", rs.getInt("target_id"));
                l.put("detail_json", rs.getString("detail_json"));
                l.put("ip_address", rs.getString("ip_address"));
                l.put("created_at", rs.getString("created_at"));
                list.add(l);
            }
        } catch (SQLException e) {
            JsonResponse.serverError(exchange, "Lỗi DB: " + e.getMessage());
            return;
        }

        JSONObject res = new JSONObject();
        res.put("total", list.size());
        res.put("logs", list);
        JsonResponse.ok(exchange, res);
    }
}
