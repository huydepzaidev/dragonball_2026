package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nro.models.data.LocalManager;
import nro.models.server.control.audit.AuditLogService;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.UserRole;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** JSON API for the native admin UI. Mirrors web2026/admin without a WebView. */
public final class NativeAdminHandler {

    private NativeAdminHandler() {
    }

    public static void handle(HttpExchange exchange, AdminUser user) throws IOException {
        if (!user.getRole().hasPermission(UserRole.ADMIN)) {
            JsonResponse.forbidden(exchange, "Cần quyền ADMIN để sử dụng quản trị đầy đủ");
            return;
        }
        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> query = query(exchange);
                JsonResponse.ok(exchange, load(query.getOrDefault("section", "overview"), query));
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                JSONObject req = AuthHandler.parseRequestBody(exchange);
                if (req == null) throw new IllegalArgumentException("JSON không hợp lệ");
                String section = text(req.get("section"));
                String action = text(req.get("action"));
                String message = act(section, action, req, user);
                AuditLogService.gI().log(user, action.toUpperCase(), section.toUpperCase(),
                        intOrNull(req.get("id")), req.toJSONString(),
                        exchange.getRemoteAddress().getAddress().getHostAddress());
                JsonResponse.ok(exchange, null, message);
                return;
            }
            JsonResponse.error(exchange, 405, "Method Not Allowed");
        } catch (IllegalArgumentException e) {
            JsonResponse.badRequest(exchange, e.getMessage());
        } catch (SQLException e) {
            JsonResponse.serverError(exchange, "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            JsonResponse.serverError(exchange, "Không thể xử lý: " + e.getMessage());
        }
    }

    private static JSONObject load(String section, Map<String, String> q) throws SQLException {
        try (Connection con = LocalManager.getConnection()) {
            JSONObject out = new JSONObject();
            if ("overview".equals(section)) {
                out.put("accounts", scalar(con, "SELECT COUNT(*) FROM account"));
                out.put("players", scalar(con, "SELECT COUNT(*) FROM player"));
                out.put("posts", scalar(con, "SELECT COUNT(*) FROM posts"));
                out.put("active_giftcodes", scalar(con, "SELECT COUNT(*) FROM giftcode WHERE count_left<>0 AND expired>NOW()"));
                out.put("pending_mailboxes", scalar(con, "SELECT COUNT(*) FROM player_mailbox WHERE status IN ('PENDING','PROCESSING')"));
                out.put("recent_accounts", rows(con, "SELECT a.id,a.username,a.is_admin,a.ban,a.active,a.create_time,p.name AS player_name FROM account a LEFT JOIN player p ON p.account_id=a.id ORDER BY a.id DESC LIMIT 10"));
                out.put("recent_logs", rows(con, "SELECT * FROM admin_audit_log ORDER BY id DESC LIMIT 10"));
                return out;
            }
            if ("accounts".equals(section)) return accounts(con, q);
            if ("giftcodes".equals(section)) {
                out.put("giftcodes", rows(con, "SELECT id,code,count_left,detail,expired,CASE WHEN count_left<>0 AND expired>NOW() THEN 1 ELSE 0 END AS active FROM giftcode ORDER BY id DESC"));
                out.put("active_count", scalar(con, "SELECT COUNT(*) FROM giftcode WHERE count_left<>0 AND expired>NOW()"));
                return out;
            }
            if ("posts".equals(section)) {
                String search = text(q.get("q"));
                if (search.isEmpty()) out.put("posts", rows(con, "SELECT p.*,(SELECT COUNT(*) FROM comments c WHERE c.post_id=p.id) AS comment_count FROM posts p ORDER BY p.ghimbai DESC,p.id DESC LIMIT 300"));
                else out.put("posts", rows(con, "SELECT p.*,(SELECT COUNT(*) FROM comments c WHERE c.post_id=p.id) AS comment_count FROM posts p WHERE p.tieude LIKE ? OR p.username LIKE ? ORDER BY p.ghimbai DESC,p.id DESC LIMIT 300", "%" + search + "%", "%" + search + "%"));
                return out;
            }
            if ("transactions".equals(section)) return transactions(con, q);
            if ("settings".equals(section)) {
                out.put("public", row(con, "SELECT * FROM settings LIMIT 1"));
                out.put("server", row(con, "SELECT * FROM adminpanel LIMIT 1"));
                return out;
            }
            if ("events".equals(section)) return events(con, q.get("event"));
            if ("game_server".equals(section)) return gameServer(con);
            if ("rewards".equals(section)) return rewards(con, q);
            if ("logs".equals(section)) {
                String search = text(q.get("q"));
                if (search.isEmpty()) out.put("logs", rows(con, "SELECT * FROM admin_audit_log ORDER BY id DESC LIMIT 300"));
                else {
                    String like = "%" + search + "%";
                    out.put("logs", rows(con, "SELECT * FROM admin_audit_log WHERE admin_username LIKE ? OR action_name LIKE ? OR target_type LIKE ? OR ip_address LIKE ? ORDER BY id DESC LIMIT 300", like, like, like, like));
                }
                return out;
            }
            throw new IllegalArgumentException("Nhóm quản trị không hợp lệ");
        }
    }

    private static JSONObject accounts(Connection con, Map<String, String> q) throws SQLException {
        String search = text(q.get("q"));
        String status = text(q.get("status"));
        String where = " WHERE 1=1";
        JSONArray args = new JSONArray();
        if (!search.isEmpty()) {
            where += " AND (a.username LIKE ? OR a.email LIKE ? OR p.name LIKE ?)";
            args.add("%" + search + "%"); args.add("%" + search + "%"); args.add("%" + search + "%");
        }
        if ("admin".equals(status)) where += " AND a.is_admin=1";
        if ("banned".equals(status)) where += " AND a.ban=1";
        if ("member".equals(status)) where += " AND a.active=1";
        if ("normal".equals(status)) where += " AND a.ban=0 AND a.is_admin=0";
        JSONObject out = new JSONObject();
        out.put("accounts", rows(con, "SELECT a.id,a.username,a.email,a.is_admin,a.ban,a.active,a.vnd,a.tongnap,a.vang,a.thoi_vang,a.event_point,a.vip,a.tichdiem,a.create_time,a.last_time_login,a.ip_address,p.id AS player_id,p.name AS player_name,p.gender,p.rank FROM account a LEFT JOIN player p ON p.account_id=a.id" + where + " ORDER BY a.id DESC LIMIT 300", args.toArray()));
        out.put("total", scalar(con, "SELECT COUNT(DISTINCT a.id) FROM account a LEFT JOIN player p ON p.account_id=a.id" + where, args.toArray()));
        return out;
    }

    private static JSONObject transactions(Connection con, Map<String, String> q) throws SQLException {
        String tab = text(q.get("tab"));
        if (!"bank".equals(tab) && !"cards".equals(tab)) tab = "payments";
        String search = text(q.get("q"));
        String status = text(q.get("status"));
        String table = "payments";
        String searchable = "(name LIKE ? OR refNo LIKE ? OR card_serial LIKE ?)";
        String credited = "is_credited";
        if ("bank".equals(tab)) { table = "bank_transfers"; searchable = "(username LIKE ? OR transaction_id LIKE ? OR description LIKE ?)"; }
        if ("cards".equals(tab)) { table = "napthe"; searchable = "(user_nap LIKE ? OR serial LIKE ? OR request_id LIKE ?)"; credited = "status"; }
        String where = " WHERE 1=1";
        if ("credited".equals(status)) where += " AND " + credited + "=1";
        if ("pending".equals(status)) where += " AND " + credited + "<>1";
        Object[] args = new Object[0];
        if (!search.isEmpty()) { where += " AND " + searchable; String like = "%" + search + "%"; args = new Object[]{like, like, like}; }
        JSONObject totals = new JSONObject();
        totals.put("payments", scalar(con, "SELECT COALESCE(SUM(final_credited_amount),0) FROM payments WHERE is_credited=1"));
        totals.put("bank", scalar(con, "SELECT COALESCE(SUM(amount),0) FROM bank_transfers WHERE is_credited=1"));
        totals.put("cards", scalar(con, "SELECT COALESCE(SUM(amount),0) FROM napthe WHERE status=1"));
        totals.put("pending", scalar(con, "SELECT (SELECT COUNT(*) FROM payments WHERE is_credited=0)+(SELECT COUNT(*) FROM bank_transfers WHERE is_credited=0)+(SELECT COUNT(*) FROM napthe WHERE status<>1)"));
        JSONObject out = new JSONObject(); out.put("tab", tab); out.put("totals", totals);
        out.put("rows", rows(con, "SELECT * FROM `" + table + "`" + where + " ORDER BY id DESC LIMIT 300", args));
        out.put("total", scalar(con, "SELECT COUNT(*) FROM `" + table + "`" + where, args));
        return out;
    }

    private static JSONObject events(Connection con, String requested) throws SQLException {
        JSONArray list = rows(con, "SELECT e.*,(SELECT COUNT(*) FROM game_event_item i WHERE i.event_key=e.event_key) AS item_count,(SELECT COUNT(*) FROM game_event_npc n WHERE n.event_key=e.event_key) AS npc_count,(SELECT COUNT(*) FROM game_event_boss b WHERE b.event_key=e.event_key) AS boss_count,(SELECT c.status FROM game_event_command c WHERE c.event_key=e.event_key AND c.status IN ('PENDING','PROCESSING') ORDER BY c.id DESC LIMIT 1) AS pending_status FROM game_event_catalog e ORDER BY e.sort_order,e.event_name");
        String key = text(requested);
        if (key.isEmpty() && !list.isEmpty()) key = text(((JSONObject) list.get(0)).get("event_key"));
        JSONObject out = new JSONObject(); out.put("events", list); out.put("selected_event", key);
        if (!key.isEmpty()) {
            out.put("items", rows(con, "SELECT ei.*,i.NAME AS item_name,i.type FROM game_event_item ei LEFT JOIN item_template i ON i.id=ei.item_id WHERE ei.event_key=? ORDER BY ei.item_role,i.NAME", key));
            out.put("npcs", rows(con, "SELECT en.*,n.NAME AS npc_name,m.NAME AS map_name FROM game_event_npc en LEFT JOIN npc_template n ON n.id=en.npc_id LEFT JOIN map_template m ON m.id=en.map_id WHERE en.event_key=? ORDER BY en.managed_runtime DESC,en.npc_id", key));
            out.put("bosses", rows(con, "SELECT eb.*,bc.boss_name,bc.active_instances,bc.last_seen_at FROM game_event_boss eb LEFT JOIN game_boss_catalog bc ON bc.boss_id=eb.boss_id WHERE eb.event_key=? ORDER BY eb.boss_id", key));
            out.put("commands", rows(con, "SELECT c.*,(SELECT COUNT(*) FROM game_event_player_backup b WHERE b.command_id=c.id) AS backup_players FROM game_event_command c WHERE c.event_key=? ORDER BY c.id DESC LIMIT 30", key));
        }
        return out;
    }

    private static JSONObject gameServer(Connection con) throws SQLException {
        JSONObject out = new JSONObject();
        out.put("config", row(con, "SELECT * FROM game_server_config WHERE id=1"));
        out.put("runtime", row(con, "SELECT * FROM game_server_runtime WHERE id=1"));
        out.put("divine_turn", row(con, "SELECT * FROM game_divine_turn_config WHERE id=1"));
        out.put("bosses", rows(con, "SELECT c.*,COUNT(d.id) AS drop_count FROM game_boss_catalog c LEFT JOIN game_boss_drop d ON d.boss_id=c.boss_id GROUP BY c.boss_id,c.boss_key,c.boss_name,c.boss_group,c.active_instances,c.last_seen_at,c.updated_at ORDER BY c.boss_group,c.boss_name,c.boss_id"));
        out.put("drops", rows(con, "SELECT d.*,c.boss_name,c.boss_key,c.boss_group,i.NAME AS item_name FROM game_boss_drop d LEFT JOIN game_boss_catalog c ON c.boss_id=d.boss_id LEFT JOIN item_template i ON i.id=d.item_id ORDER BY c.boss_name,d.id"));
        out.put("commands", rows(con, "SELECT * FROM game_server_command ORDER BY id DESC LIMIT 50"));
        return out;
    }

    private static JSONObject rewards(Connection con, Map<String, String> q) throws SQLException {
        String ranking = ranking(text(q.get("ranking")));
        JSONObject period = rankingPeriod(ranking, text(q.get("batch_key")), text(q.get("ranking_date")));
        String date = text(period.get("ranking_date"));
        JSONObject out = new JSONObject(); out.put("rankings", rankingDefinitions()); out.put("selected_ranking", ranking); out.put("ranking_date", date); out.put("period", period);
        out.put("preview", rankingPreview(con, ranking, date));
        out.put("configs", rows(con, "SELECT * FROM top_reward_config WHERE ranking_key=? ORDER BY rank_position", ranking));
        out.put("commands", rows(con, "SELECT c.*,(SELECT COUNT(*) FROM top_reward_winner w WHERE w.command_id=c.id) AS winner_count FROM top_reward_command c WHERE c.ranking_key=? ORDER BY c.id DESC LIMIT 30", ranking));
        out.put("winners", rows(con, "SELECT w.*,m.status AS mail_status,m.claimed_at FROM top_reward_winner w INNER JOIN player_mailbox m ON m.id=w.mailbox_id WHERE w.command_id=(SELECT id FROM top_reward_command WHERE ranking_key=? AND status='DONE' ORDER BY id DESC LIMIT 1) ORDER BY w.rank_position", ranking));
        out.put("mailboxes", rows(con, "SELECT m.*,p.name AS player_name,a.username FROM player_mailbox m INNER JOIN player p ON p.id=m.player_id INNER JOIN account a ON a.id=m.account_id ORDER BY m.id DESC LIMIT 200"));
        out.put("mailbox_counts", rows(con, "SELECT status,COUNT(*) AS total FROM player_mailbox GROUP BY status"));
        out.put("activation_configs", rows(con, "SELECT * FROM activation_reward_config ORDER BY planet"));
        if ("top_boss".equals(ranking) || "summer".equals(ranking)) {
            String rankingType = "top_boss".equals(ranking) ? "BOSS" : "SUMMER_EVENT";
            JSONArray weeklyPeriods = rows(con, "SELECT ranking_date,COUNT(*) AS player_count,SUM(score) AS total_score FROM daily_ranking_score WHERE ranking_type=? GROUP BY ranking_date ORDER BY ranking_date DESC LIMIT 12", rankingType);
            String currentWeek = monday();
            boolean hasCurrentWeek = weeklyPeriods.stream().anyMatch(value ->
                    currentWeek.equals(text(((JSONObject) value).get("ranking_date"))));
            if (!hasCurrentWeek) {
                JSONObject current = new JSONObject();
                current.put("ranking_date", currentWeek);
                current.put("player_count", 0L);
                current.put("total_score", 0L);
                weeklyPeriods.add(0, current);
            }
            out.put("weekly_periods", weeklyPeriods);
        } else {
            out.put("weekly_periods", new JSONArray());
        }
        out.put("existing_period_command", existingPeriodCommand(con, ranking, period));
        return out;
    }

    private static String act(String section, String action, JSONObject req, AdminUser user) throws SQLException {
        try (Connection con = LocalManager.getConnection()) {
            if ("accounts".equals(section)) return accountAct(con, action, req, user);
            if ("giftcodes".equals(section)) return giftcodeAct(con, action, req);
            if ("posts".equals(section)) return postAct(con, action, req, user);
            if ("settings".equals(section)) return settingsAct(con, action, req);
            if ("events".equals(section)) return eventAct(con, action, req, user);
            if ("game_server".equals(section)) return gameAct(con, action, req, user);
            if ("rewards".equals(section)) return rewardAct(con, action, req, user);
            throw new IllegalArgumentException("Nhóm thao tác không hợp lệ");
        }
    }

    private static String accountAct(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        int id = positive(r.get("id"), "ID tài khoản");
        if ("save_resources".equals(action)) {
            execute(con, "UPDATE account SET vnd=?,tongnap=?,vang=?,thoi_vang=?,event_point=?,vip=?,tichdiem=?,update_time=NOW() WHERE id=?", nonnegative(r.get("vnd")), nonnegative(r.get("tongnap")), nonnegativeLong(r.get("vang")), nonnegative(r.get("thoi_vang")), nonnegative(r.get("event_point")), nonnegative(r.get("vip")), nonnegative(r.get("tichdiem")), id);
            return "Đã lưu tài nguyên tài khoản";
        }
        String field;
        if ("toggle_ban".equals(action)) field = "ban"; else if ("toggle_active".equals(action)) field = "active"; else if ("toggle_admin".equals(action)) field = "is_admin"; else throw new IllegalArgumentException("Thao tác tài khoản không hợp lệ");
        if (id == user.getId() && !"toggle_active".equals(action)) throw new IllegalArgumentException("Không thể tự khóa hoặc tự gỡ quyền tài khoản đang đăng nhập");
        execute(con, "UPDATE account SET " + field + "=IF(" + field + "=1,0,1),update_time=NOW() WHERE id=?", id);
        return "Đã cập nhật trạng thái tài khoản";
    }

    private static String giftcodeAct(Connection con, String action, JSONObject r) throws SQLException {
        if ("delete".equals(action)) { execute(con, "DELETE FROM giftcode WHERE id=?", positive(r.get("id"), "ID giftcode")); return "Đã xóa giftcode"; }
        if (!"save".equals(action)) throw new IllegalArgumentException("Thao tác giftcode không hợp lệ");
        String code = required(r, "code"); if (!code.matches("[A-Za-z0-9_-]{3,50}")) throw new IllegalArgumentException("Giftcode không hợp lệ");
        Integer id = intOrNull(r.get("id"));
        int countLeft = integer(r.get("count_left"), -1);
        if (countLeft < -1) throw new IllegalArgumentException("Lượt còn phải từ 0 trở lên hoặc -1 là không giới hạn");
        String expired = required(r, "expired");
        try {
            LocalDateTime.parse(expired, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Hết hạn phải đúng định dạng YYYY-MM-DD HH:mm:ss");
        }
        long duplicate = id != null && id > 0
                ? scalar(con, "SELECT COUNT(*) FROM giftcode WHERE code=? AND id<>?", code, id).longValue()
                : scalar(con, "SELECT COUNT(*) FROM giftcode WHERE code=?", code).longValue();
        if (duplicate > 0) throw new IllegalArgumentException("Mã giftcode đã tồn tại");
        String detail = normalizeRewards(con, r.get("detail"), null);
        if (id != null && id > 0) { execute(con, "UPDATE giftcode SET code=?,count_left=?,detail=?,expired=? WHERE id=?", code, countLeft, detail, expired, id); return "Đã cập nhật giftcode"; }
        execute(con, "INSERT INTO giftcode(code,count_left,detail,expired) VALUES(?,?,?,?)", code, countLeft, detail, expired); return "Đã tạo giftcode";
    }

    private static String postAct(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        Integer id = intOrNull(r.get("id"));
        if ("delete".equals(action)) { execute(con, "DELETE FROM comments WHERE post_id=?", id); execute(con, "DELETE FROM posts WHERE id=?", id); return "Đã xóa bài viết và bình luận"; }
        if ("toggle_pin".equals(action)) { execute(con, "UPDATE posts SET ghimbai=IF(ghimbai=1,0,1) WHERE id=?", id); return "Đã đổi trạng thái ghim"; }
        if (!"save".equals(action)) throw new IllegalArgumentException("Thao tác bài viết không hợp lệ");
        String title = required(r, "title"), content = required(r, "content"); if (title.length() < 3 || title.length() > 75 || content.length() < 10) throw new IllegalArgumentException("Tiêu đề 3-75 ký tự, nội dung tối thiểu 10 ký tự");
        if (id != null && id > 0) { execute(con, "UPDATE posts SET tieude=?,noidung=?,theloai=?,ghimbai=?,image=? WHERE id=?", title, content, nonnegative(r.get("category")), bool(r.get("pinned")) ? 1 : 0, text(r.get("image")), id); return "Đã cập nhật bài viết"; }
        execute(con, "INSERT INTO posts(tieude,noidung,username,theloai,ghimbai,image,trangthai,tinhtrang,`like`) VALUES(?,?,?,?,?,?,0,0,0)", title, content, user.getUsername(), nonnegative(r.get("category")), bool(r.get("pinned")) ? 1 : 0, text(r.get("image"))); return "Đã đăng bài viết";
    }

    private static String settingsAct(Connection con, String action, JSONObject r) throws SQLException {
        if ("save_public".equals(action)) {
            execute(con, "UPDATE settings SET Title=?,ServerName=?,Fanpage=?,`Group`=?,Zalo=?,EmailSupport=?,AccountBank=?,NumberBank=?,NameBank=?,Android=?,Windows=?,IPhone=?,Java=?", required(r, "title"), required(r, "server_name"), text(r.get("fanpage")), text(r.get("group")), text(r.get("zalo")), text(r.get("email_support")), text(r.get("account_bank")), text(r.get("number_bank")), text(r.get("name_bank")), text(r.get("android")), text(r.get("windows")), text(r.get("iphone")), text(r.get("java")));
            execute(con, "UPDATE adminpanel SET android=?,windows=?,iphone=?,java=?", text(r.get("android")), text(r.get("windows")), text(r.get("iphone")), text(r.get("java"))); return "Đã lưu thông tin website và link tải";
        }
        if ("save_server".equals(action)) { String state = text(r.get("state")); if (!"hoatdong".equals(state) && !"baotri".equals(state)) throw new IllegalArgumentException("Trạng thái không hợp lệ"); execute(con, "UPDATE adminpanel SET title=?,tenmaychu=?,domain=?,trangthai=?,giatri=?", required(r, "title"), required(r, "server_name"), text(r.get("domain")), state, nonnegative(r.get("exchange_value"))); return "Đã lưu cấu hình máy chủ website"; }
        throw new IllegalArgumentException("Thao tác cài đặt không hợp lệ");
    }

    private static String eventAct(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        if (!"toggle".equals(action)) throw new IllegalArgumentException("Thao tác sự kiện không hợp lệ"); String key = required(r, "event_key");
        if (scalar(con, "SELECT COUNT(*) FROM game_event_command WHERE event_key=? AND status IN ('PENDING','PROCESSING')", key).longValue() > 0) throw new IllegalArgumentException("Sự kiện đang có lệnh chờ");
        int target = scalar(con, "SELECT enabled FROM game_event_catalog WHERE event_key=?", key).intValue() == 1 ? 0 : 1;
        execute(con, "INSERT INTO game_event_command(event_key,target_enabled,requested_by) VALUES(?,?,?)", key, target, user.getUsername()); execute(con, "UPDATE game_event_catalog SET updated_by=? WHERE event_key=?", user.getUsername(), key); return target == 1 ? "Đã gửi lệnh bật/reset sự kiện" : "Đã gửi lệnh tắt/reset sự kiện";
    }

    private static String gameAct(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        if ("save_config".equals(action)) {
            execute(con, "INSERT INTO game_server_config(id,exp_rate,drop_rate_percent,auto_maintenance_enabled,maintenance_time,maintenance_countdown_seconds,boss_watchdog_enabled,boss_stuck_seconds,config_refresh_seconds,updated_by) VALUES(1,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE exp_rate=VALUES(exp_rate),drop_rate_percent=VALUES(drop_rate_percent),auto_maintenance_enabled=VALUES(auto_maintenance_enabled),maintenance_time=VALUES(maintenance_time),maintenance_countdown_seconds=VALUES(maintenance_countdown_seconds),boss_watchdog_enabled=VALUES(boss_watchdog_enabled),boss_stuck_seconds=VALUES(boss_stuck_seconds),config_refresh_seconds=VALUES(config_refresh_seconds),updated_by=VALUES(updated_by)", between(r.get("exp_rate"),1,100,"EXP"), between(r.get("drop_rate_percent"),0,1000,"Drop"), bool(r.get("auto_maintenance_enabled"))?1:0, required(r,"maintenance_time"), between(r.get("maintenance_countdown_seconds"),10,3600,"Đếm ngược"), bool(r.get("boss_watchdog_enabled"))?1:0, between(r.get("boss_stuck_seconds"),10,3600,"Boss stuck"), between(r.get("config_refresh_seconds"),2,60,"Chu kỳ"), user.getUsername()); queue(con,"RELOAD_CONFIG",null,user); return "Đã lưu và gửi lệnh nạp lại cấu hình";
        }
        if ("save_login_notice".equals(action)) { execute(con, "INSERT INTO game_server_config(id,login_notice_enabled,login_notice_text,updated_by) VALUES(1,?,?,?) ON DUPLICATE KEY UPDATE login_notice_enabled=VALUES(login_notice_enabled),login_notice_text=VALUES(login_notice_text),updated_by=VALUES(updated_by)", bool(r.get("login_notice_enabled"))?1:0, text(r.get("login_notice_text")), user.getUsername()); queue(con,"RELOAD_CONFIG",null,user); return "Đã lưu thông báo đăng nhập"; }
        if ("save_divine_turn".equals(action)) {
            String[] f={"one_zero_bp","one_one_bp","one_two_bp","two_zero_bp","two_one_bp","two_two_bp","multi_zero_bp","multi_one_bp","multi_two_bp","multi_three_bp"}; int[] v=new int[10]; for(int i=0;i<10;i++)v[i]=between(r.get(f[i]),0,10000,f[i]); if(v[0]+v[1]+v[2]!=10000||v[3]+v[4]+v[5]!=10000||v[6]+v[7]+v[8]+v[9]!=10000)throw new IllegalArgumentException("Mỗi nhóm tỷ lệ phải cộng đúng 10000");
            execute(con,"INSERT INTO game_divine_turn_config(id,enabled,one_zero_bp,one_one_bp,one_two_bp,two_zero_bp,two_one_bp,two_two_bp,multi_zero_bp,multi_one_bp,multi_two_bp,multi_three_bp,pity_blank_turns,updated_by) VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),one_zero_bp=VALUES(one_zero_bp),one_one_bp=VALUES(one_one_bp),one_two_bp=VALUES(one_two_bp),two_zero_bp=VALUES(two_zero_bp),two_one_bp=VALUES(two_one_bp),two_two_bp=VALUES(two_two_bp),multi_zero_bp=VALUES(multi_zero_bp),multi_one_bp=VALUES(multi_one_bp),multi_two_bp=VALUES(multi_two_bp),multi_three_bp=VALUES(multi_three_bp),pity_blank_turns=VALUES(pity_blank_turns),updated_by=VALUES(updated_by)",bool(r.get("enabled"))?1:0,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],between(r.get("pity_blank_turns"),0,100,"Pity"),user.getUsername()); queue(con,"RELOAD_CONFIG",null,user); return "Đã lưu tỷ lệ đồ Thần Linh";
        }
        if ("add_drop".equals(action)) { String kind=text(r.get("drop_kind")); if(!"ITEM".equals(kind)&&!"DIVINE_RANDOM".equals(kind))throw new IllegalArgumentException("Loại rơi không hợp lệ"); Integer item="DIVINE_RANDOM".equals(kind)?null:nonnegative(r.get("item_id")); int min=between(r.get("quantity_min"),1,2000000000,"Số lượng"); execute(con,"INSERT INTO game_boss_drop(boss_id,drop_kind,item_id,chance_bp,quantity_min,quantity_max,enabled,created_by) VALUES(?,?,?,?,?,?,1,?)",integer(r.get("boss_id"),0),kind,item,between(r.get("chance_bp"),0,10000,"Tỷ lệ"),min,between(r.get("quantity_max"),min,2000000000,"Số lượng"),user.getUsername()); queue(con,"RELOAD_CONFIG",null,user); return "Đã thêm vật phẩm rơi boss"; }
        if ("toggle_drop".equals(action)) { execute(con,"UPDATE game_boss_drop SET enabled=IF(enabled=1,0,1) WHERE id=?",positive(r.get("id"),"ID cấu hình")); queue(con,"RELOAD_CONFIG",null,user); return "Đã đổi trạng thái rơi đồ"; }
        if ("delete_drop".equals(action)) { execute(con,"DELETE FROM game_boss_drop WHERE id=?",positive(r.get("id"),"ID cấu hình")); queue(con,"RELOAD_CONFIG",null,user); return "Đã xóa cấu hình rơi đồ"; }
        if ("server_command".equals(action)) { String cmd=required(r,"command_type"); if(!"RELOAD_CONFIG".equals(cmd)&&!"RESPAWN_BOSS".equals(cmd)&&!"RESPAWN_ALL".equals(cmd)&&!"START_MAINTENANCE".equals(cmd)&&!"STOP_MAINTENANCE".equals(cmd))throw new IllegalArgumentException("Lệnh không hợp lệ"); Integer boss="RESPAWN_BOSS".equals(cmd)?intOrNull(r.get("boss_id")):null; if("RESPAWN_BOSS".equals(cmd)&&boss==null)throw new IllegalArgumentException("Chưa chọn boss"); queue(con,cmd,boss,user); return "Đã gửi lệnh game server"; }
        throw new IllegalArgumentException("Thao tác game server không hợp lệ");
    }

    private static String rewardAct(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        boolean originalAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            String message = rewardActTransactional(con, action, r, user);
            con.commit();
            return message;
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(originalAutoCommit);
        }
    }

    private static String rewardActTransactional(Connection con, String action, JSONObject r, AdminUser user) throws SQLException {
        if ("send_mail".equals(action)) {
            String playerName = required(r, "player_name");
            JSONObject player = row(con,
                    "SELECT p.id AS player_id,p.account_id,p.name FROM player p INNER JOIN account a ON a.id=p.account_id WHERE LOWER(p.name)=LOWER(?) LIMIT 1",
                    playerName);
            if (player == null) throw new IllegalArgumentException("Không tìm thấy nhân vật \"" + playerName + "\"");
            String title = validatedText(r.get("title"), "Tiêu đề thư", 3, 120, false);
            String message = validatedText(r.get("message"), "Nội dung thư", 0, 500, true);
            String sender = validatedText(r.get("sender_name"), "Tên người gửi", 2, 50, false);
            Integer rankPosition = intOrNull(r.get("rank_position"));
            if (rankPosition != null && (rankPosition < 1 || rankPosition > 10)) {
                throw new IllegalArgumentException("Hạng xếp hạng chỉ nhận giá trị từ 1 đến 10");
            }
            String rewardsJson = normalizeRewards(con, r.get("rewards_json"), null);
            execute(con,"INSERT INTO player_mailbox(account_id,player_id,title,message,sender_name,rank_position,rewards_json,created_by) VALUES(?,?,?,?,?,?,?,?)",
                    player.get("account_id"), player.get("player_id"), title, message, sender,
                    rankPosition, rewardsJson, user.getId());
            return "Đã gửi quà hòm thư cho " + player.get("name");
        }
        if ("cancel_mail".equals(action)) {
            int mailId = positive(r.get("id"), "ID thư");
            if (execute(con,"UPDATE player_mailbox SET status='CANCELLED',cancelled_at=NOW() WHERE id=? AND status='PENDING'",mailId)!=1) {
                throw new IllegalArgumentException("Chỉ thu hồi được thư đang chờ nhận");
            }
            return "Đã thu hồi thư #" + mailId;
        }
        if ("save_top_config".equals(action)) {
            String rankingKey = ranking(text(r.get("ranking")));
            String group = required(r,"rank_group");
            int firstRank;
            int lastRank;
            if ("1".equals(group) || "2".equals(group) || "3".equals(group)) {
                firstRank = Integer.parseInt(group); lastRank = firstRank;
            } else if ("4-10".equals(group)) {
                firstRank = 4; lastRank = 10;
            } else {
                throw new IllegalArgumentException("Chỉ được cấu hình Top 1, Top 2, Top 3 hoặc nhóm Top 4-10");
            }
            String title = validatedText(r.get("title"), "Tiêu đề thư", 3, 120, false);
            String message = validatedText(r.get("message"), "Nội dung thư", 0, 500, true);
            String sender = validatedText(r.get("sender_name"), "Tên người gửi", 2, 50, false);
            String rewardsJson = normalizeRewards(con, r.get("rewards_json"), firstRank == 1 ? 1 : firstRank);
            for (int rank = firstRank; rank <= lastRank; rank++) {
                execute(con,"INSERT INTO top_reward_config(ranking_key,rank_position,title,message,sender_name,rewards_json,updated_by) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title),message=VALUES(message),sender_name=VALUES(sender_name),rewards_json=VALUES(rewards_json),updated_by=VALUES(updated_by)",
                        rankingKey,rank,title,message,sender,rewardsJson,user.getId());
            }
            return "Đã lưu bộ quà Top " + group;
        }
        if ("save_activation_config".equals(action)) {
            int planet=between(r.get("planet"),0,2,"Hành tinh"); String name=planet==0?"Trái Đất":planet==1?"Namếc":"Xayda";
            execute(con,"INSERT INTO activation_reward_config(planet,planet_name,activation_options_json,activation_weights_json,bonus_options_json,enabled,updated_by) VALUES(?,?,?,?,?,1,?) ON DUPLICATE KEY UPDATE planet_name=VALUES(planet_name),activation_options_json=VALUES(activation_options_json),activation_weights_json=VALUES(activation_weights_json),bonus_options_json=VALUES(bonus_options_json),enabled=1,updated_by=VALUES(updated_by)",planet,name,json(r.get("activation_options_json"),"Option kích hoạt"),json(r.get("activation_weights_json"),"Trọng số"),json(r.get("bonus_options_json"),"Option cộng thêm"),user.getId());
            return "Đã lưu Hộp Set/Capsule "+name;
        }
        if ("finalize_top".equals(action)) {
            String rankingKey = ranking(text(r.get("ranking")));
            JSONObject period = rankingPeriod(rankingKey, text(r.get("batch_key")), text(r.get("ranking_date")));
            JSONArray configs = rows(con,"SELECT rank_position,title,message,sender_name,rewards_json FROM top_reward_config WHERE ranking_key=? AND rank_position BETWEEN 1 AND 10 ORDER BY rank_position FOR UPDATE",rankingKey);
            if (configs.size()!=10) throw new IllegalArgumentException("Phải cấu hình đủ bộ quà từ Top 1 đến Top 10 trước khi chốt");
            for (int index = 0; index < configs.size(); index++) {
                JSONObject config = (JSONObject) configs.get(index);
                int rankPosition = integer(config.get("rank_position"), -1);
                if (rankPosition != index + 1) throw new IllegalArgumentException("Cấu hình quà bị thiếu hoặc trùng hạng Top");
                config.put("title", validatedText(config.get("title"), "Tiêu đề Top " + rankPosition, 3, 120, false));
                config.put("message", validatedText(config.get("message"), "Nội dung Top " + rankPosition, 0, 500, true));
                config.put("sender_name", validatedText(config.get("sender_name"), "Tên người gửi Top " + rankPosition, 2, 50, false));
                config.put("rewards_json", normalizeRewards(con, config.get("rewards_json"), rankPosition));
            }
            if (rankingPreview(con, rankingKey, text(period.get("ranking_date"))).isEmpty()) {
                throw new IllegalArgumentException("Bảng xếp hạng của kỳ này chưa có người chơi hợp lệ");
            }
            JSONObject existing = existingPeriodCommand(con, rankingKey, period);
            if (existing != null) {
                throw new IllegalArgumentException("Kỳ này đã được chốt hoặc đang xử lý ở lệnh #" + existing.get("id"));
            }
            if (scalar(con,"SELECT COUNT(*) FROM top_reward_command WHERE ranking_key=? AND status IN ('PENDING','PROCESSING')",rankingKey).longValue()>0) {
                throw new IllegalArgumentException("Bảng xếp hạng đang có một lệnh chốt chờ game server xử lý");
            }
            String key = text(period.get("key"));
            JSONObject sameBatch = row(con,"SELECT id,status FROM top_reward_command WHERE ranking_key=? AND batch_key=? LIMIT 1",rankingKey,key);
            String batchTitle = validatedText(r.get("batch_title"), "Tên đợt chốt", 3, 120, false);
            String periodType = text(period.get("type"));
            String rankingDate = text(period.get("ranking_date"));
            Object dateValue = rankingDate.isEmpty() ? null : rankingDate;
            if (sameBatch != null && "FAILED".equals(text(sameBatch.get("status")))) {
                int updated = execute(con,"UPDATE top_reward_command SET period_type=?,batch_title=?,ranking_date=?,config_snapshot_json=?,requested_by=?,requested_by_name=?,status='PENDING',started_at=NULL,finished_at=NULL,result_message=NULL WHERE id=? AND status='FAILED'",
                        periodType,batchTitle,dateValue,configs.toJSONString(),user.getId(),user.getUsername(),sameBatch.get("id"));
                if (updated != 1) throw new IllegalArgumentException("Không thể gửi lại lệnh chốt đã lỗi");
                return "Đã gửi lại lệnh chốt Top #" + sameBatch.get("id");
            }
            execute(con,"INSERT INTO top_reward_command(ranking_key,period_type,batch_key,batch_title,ranking_date,config_snapshot_json,requested_by,requested_by_name) VALUES(?,?,?,?,?,?,?,?)",
                    rankingKey,periodType,key,batchTitle,dateValue,configs.toJSONString(),user.getId(),user.getUsername());
            return "Đã gửi lệnh chốt Top 1-10; game server sẽ khóa người thắng và tạo hòm thư";
        }
        throw new IllegalArgumentException("Thao tác quà/Top không hợp lệ");
    }

    private static void queue(Connection con,String type,Integer boss,AdminUser user)throws SQLException{ Number n=boss==null?scalar(con,"SELECT COUNT(*) FROM game_server_command WHERE command_type=? AND boss_id IS NULL AND status IN ('PENDING','PROCESSING')",type):scalar(con,"SELECT COUNT(*) FROM game_server_command WHERE command_type=? AND boss_id=? AND status IN ('PENDING','PROCESSING')",type,boss); if(n.longValue()==0)execute(con,"INSERT INTO game_server_command(command_type,boss_id,requested_by) VALUES(?,?,?)",type,boss,user.getUsername()); }

    private static JSONObject rankingDefinitions(){JSONObject o=new JSONObject();o.put("top_boss",def("Đại Thiên Sứ · Top săn Boss","WEEKLY"));o.put("summer",def("Đại Thiên Sứ · Top sự kiện","WEEKLY"));o.put("top_power",def("Đại Thiên Sứ · Top sức mạnh","LIFETIME"));o.put("top_task",def("Đại Thiên Sứ · Top nhiệm vụ","LIFETIME"));o.put("childrens_day",def("Quốc tế Thiếu nhi","MANUAL"));o.put("sugarcane",def("Nước mía","MANUAL"));o.put("fruit_ice_cream",def("Kem trái cây","MANUAL"));o.put("top_up",def("Đua Top nạp","MANUAL"));return o;}
    private static JSONObject def(String n,String p){JSONObject o=new JSONObject();o.put("name",n);o.put("period",p);return o;}
    private static String ranking(String k){if(k.isEmpty())k="top_boss";if(!rankingDefinitions().containsKey(k))throw new IllegalArgumentException("Bảng xếp hạng không hợp lệ");return k;}
    private static JSONArray rankingPreview(Connection c,String k,String d)throws SQLException{if("top_up".equals(k))return rows(c,"SELECT p.id AS player_id,p.account_id,p.name,a.username,a.tongnap AS score FROM account a INNER JOIN player p ON p.account_id=a.id WHERE a.tongnap>0 AND a.ban=0 AND a.is_admin=0 ORDER BY a.tongnap DESC,p.id ASC LIMIT 10");if("top_boss".equals(k)||"summer".equals(k))return rows(c,"SELECT p.id AS player_id,p.account_id,p.name,a.username,s.score FROM daily_ranking_score s INNER JOIN player p ON p.id=s.player_id INNER JOIN account a ON a.id=p.account_id WHERE s.ranking_date=? AND s.ranking_type=? AND s.score>0 AND a.ban=0 AND a.is_admin=0 ORDER BY s.score DESC,p.id ASC LIMIT 10",d,"top_boss".equals(k)?"BOSS":"SUMMER_EVENT");if("top_power".equals(k))return rows(c,"SELECT p.id AS player_id,p.account_id,p.name,a.username,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_point,'$[1]')) AS UNSIGNED),0) AS score FROM player p INNER JOIN account a ON a.id=p.account_id WHERE a.ban=0 AND a.is_admin=0 ORDER BY score DESC,p.id ASC LIMIT 10");if("top_task".equals(k))return rows(c,"SELECT p.id AS player_id,p.account_id,p.name,a.username,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[0]')) AS UNSIGNED),0) AS score FROM player p INNER JOIN account a ON a.id=p.account_id WHERE a.ban=0 AND a.is_admin=0 ORDER BY score DESC,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[1]')) AS UNSIGNED),0) DESC,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[2]')) AS UNSIGNED),0) DESC,p.id ASC LIMIT 10");String col="childrens_day".equals(k)?"point_sukien":"sugarcane".equals(k)?"point_sukien1":"point_sukien2";return rows(c,"SELECT p.id AS player_id,p.account_id,p.name,a.username,p.`"+col+"` AS score FROM player p INNER JOIN account a ON a.id=p.account_id WHERE p.`"+col+"`>0 AND a.ban=0 AND a.is_admin=0 ORDER BY p.`"+col+"` DESC,p.id ASC LIMIT 10");}
    private static String monday(){LocalDate d=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));while(d.getDayOfWeek()!=DayOfWeek.MONDAY)d=d.minusDays(1);return d.toString();}

    private static JSONObject rankingPeriod(String rankingKey, String manualKey, String weeklyDate) {
        JSONObject definition = (JSONObject) rankingDefinitions().get(rankingKey);
        String type = text(definition.get("period"));
        JSONObject period = new JSONObject();
        period.put("type", type);
        if ("LIFETIME".equals(type)) {
            period.put("key", "lifetime"); period.put("ranking_date", null);
            period.put("label", "Toàn máy chủ · chỉ trao một lần");
            return period;
        }
        if ("WEEKLY".equals(type)) {
            String selected = weeklyDate.isEmpty() ? monday() : weeklyDate;
            LocalDate start;
            try { start = LocalDate.parse(selected); } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Kỳ tuần không đúng định dạng YYYY-MM-DD");
            }
            LocalDate current = LocalDate.parse(monday());
            if (start.getDayOfWeek() != DayOfWeek.MONDAY || start.isAfter(current)) {
                throw new IllegalArgumentException("Kỳ tuần phải là ngày thứ Hai và không được ở tương lai");
            }
            period.put("key", "week-" + selected.replace("-", "")); period.put("ranking_date", selected);
            period.put("label", "Tuần " + selected + " đến " + start.plusDays(6));
            return period;
        }
        String key = manualKey.isEmpty() ? rankingKey + "-" + System.currentTimeMillis() : manualKey;
        if (!key.matches("[A-Za-z0-9_-]{3,80}")) {
            throw new IllegalArgumentException("Mã đợt chỉ gồm chữ, số, gạch ngang/gạch dưới và dài 3-80 ký tự");
        }
        period.put("key", key); period.put("ranking_date", null); period.put("label", "Đợt do Admin đặt");
        return period;
    }

    private static JSONObject existingPeriodCommand(Connection con, String rankingKey, JSONObject period) throws SQLException {
        String type = text(period.get("type"));
        if ("LIFETIME".equals(type)) return row(con,"SELECT id,status FROM top_reward_command WHERE ranking_key=? AND status<>'FAILED' ORDER BY id LIMIT 1",rankingKey);
        if ("WEEKLY".equals(type)) return row(con,"SELECT id,status FROM top_reward_command WHERE ranking_key=? AND ranking_date=? AND status<>'FAILED' ORDER BY id LIMIT 1",rankingKey,period.get("ranking_date"));
        return row(con,"SELECT id,status FROM top_reward_command WHERE ranking_key=? AND batch_key=? AND status<>'FAILED' LIMIT 1",rankingKey,period.get("key"));
    }

    private static String normalizeRewards(Connection con, Object raw, Integer topRank) throws SQLException {
        Object parsed = raw instanceof JSONArray ? raw : JSONValue.parse(text(raw));
        if (!(parsed instanceof JSONArray rewards) || rewards.isEmpty()) throw new IllegalArgumentException("Phải có ít nhất một phần thưởng");
        if (rewards.size() > 50) throw new IllegalArgumentException("Chỉ được chọn tối đa 50 phần thưởng");
        Set<Long> seenItems = new HashSet<>(); Set<Long> itemIds = new HashSet<>(); Set<Long> optionIds = new HashSet<>();
        JSONArray normalized = new JSONArray();
        for (Object value : rewards) {
            if (!(value instanceof JSONObject) && !(value instanceof Map)) throw new IllegalArgumentException("Dữ liệu phần thưởng không hợp lệ");
            Map<?,?> reward = (Map<?,?>) value;
            long id = strictLong(reward.get("id"), "ID vật phẩm");
            long quantity = strictLong(reward.get("quantity"), "Số lượng");
            if (id < -3 || id > Integer.MAX_VALUE) throw new IllegalArgumentException("ID vật phẩm không hợp lệ");
            if (quantity < 1 || quantity > 2_000_000_000L) throw new IllegalArgumentException("Số lượng phải từ 1 đến 2.000.000.000");
            if (!seenItems.add(id)) throw new IllegalArgumentException("Một vật phẩm chỉ được xuất hiện một lần trong bộ quà");
            if (id >= 0) itemIds.add(id);
            Object optionsValue = reward.get("options");
            JSONArray options = optionsValue == null ? new JSONArray() : optionsValue instanceof JSONArray ? (JSONArray) optionsValue : null;
            if (options == null || options.size() > 30) throw new IllegalArgumentException("Danh sách option không hợp lệ");
            if (id < 0 && !options.isEmpty()) throw new IllegalArgumentException("Vàng/ngọc không sử dụng option");
            int[][] preset = narutoPreset((int) id);
            if (topRank != null && preset != null) {
                if (topRank != 1) throw new IllegalArgumentException("Vật phẩm trong Rương hợp tác Naruto chỉ được cấu hình cho Top 1");
                options = presetOptions(preset);
            }
            JSONArray normalizedOptions = new JSONArray(); Set<Long> seenOptions = new HashSet<>();
            for (Object optionValue : options) {
                if (!(optionValue instanceof JSONObject) && !(optionValue instanceof Map)) throw new IllegalArgumentException("Dữ liệu option không hợp lệ");
                Map<?,?> option = (Map<?,?>) optionValue;
                long optionId = strictLong(option.get("id"), "ID option"); long param = strictLong(option.get("param"), "Chỉ số option");
                if (optionId < 0 || optionId > Integer.MAX_VALUE || param < Integer.MIN_VALUE || param > Integer.MAX_VALUE) throw new IllegalArgumentException("ID hoặc param option không hợp lệ");
                if (!seenOptions.add(optionId)) throw new IllegalArgumentException("Một vật phẩm không thể có hai option cùng loại");
                optionIds.add(optionId); JSONObject normalizedOption = new JSONObject(); normalizedOption.put("id", optionId); normalizedOption.put("param", param); normalizedOptions.add(normalizedOption);
            }
            JSONObject normalizedReward = new JSONObject(); normalizedReward.put("id", id); normalizedReward.put("quantity", quantity); normalizedReward.put("options", normalizedOptions); normalized.add(normalizedReward);
        }
        validateCatalogIds(con, "item_template", itemIds, "Có vật phẩm không tồn tại trong database");
        validateCatalogIds(con, "item_option_template", optionIds, "Có option không tồn tại trong database");
        return normalized.toJSONString();
    }

    private static void validateCatalogIds(Connection con, String table, Set<Long> ids, String message) throws SQLException {
        if (ids.isEmpty()) return;
        List<Object> args = new ArrayList<>(ids); StringBuilder placeholders = new StringBuilder();
        for (int i=0;i<args.size();i++) { if(i>0) placeholders.append(','); placeholders.append('?'); }
        if (scalar(con,"SELECT COUNT(*) FROM `"+table+"` WHERE id IN ("+placeholders+")",args.toArray()).longValue()!=ids.size()) throw new IllegalArgumentException(message);
    }

    private static int[][] narutoPreset(int id) {
        return switch(id) {
            case 2019 -> new int[][]{{50,15},{77,15},{103,15},{204,10},{14,7},{30,0}};
            case 2026 -> new int[][]{{77,25},{22,35},{94,15},{30,0}};
            case 2027 -> new int[][]{{50,25},{0,12000},{14,15},{5,20},{30,0}};
            case 2030 -> new int[][]{{50,20},{77,20},{103,20},{95,10},{96,10},{14,15},{30,0}};
            case 2039 -> new int[][]{{50,22},{77,22},{103,22},{101,55},{14,10},{30,0}};
            default -> null;
        };
    }

    private static JSONArray presetOptions(int[][] preset) { JSONArray out=new JSONArray(); for(int[] pair:preset){JSONObject option=new JSONObject();option.put("id",pair[0]);option.put("param",pair[1]);out.add(option);}return out; }
    private static long strictLong(Object value,String label){if(value instanceof Number)return((Number)value).longValue();try{return Long.parseLong(text(value));}catch(Exception e){throw new IllegalArgumentException(label+" không hợp lệ");}}
    private static String validatedText(Object value,String label,int min,int max,boolean emptyAllowed){String result=text(value);int length=result.codePointCount(0,result.length());if((!emptyAllowed&&length<min)||length>max)throw new IllegalArgumentException(label+" phải dài "+min+"-"+max+" ký tự");return result;}

    private static Map<String,String> query(HttpExchange e){Map<String,String>m=new HashMap<>();String q=e.getRequestURI().getRawQuery();if(q==null)return m;for(String p:q.split("&")){String[]x=p.split("=",2);m.put(URLDecoder.decode(x[0],StandardCharsets.UTF_8),x.length>1?URLDecoder.decode(x[1],StandardCharsets.UTF_8):"");}return m;}
    private static JSONArray rows(Connection c, String s, Object... a) throws SQLException {
        JSONArray rows = new JSONArray();
        try (PreparedStatement statement = c.prepareStatement(s)) {
            bind(statement, a);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                while (result.next()) {
                    JSONObject row = new JSONObject();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        Object value = result.getObject(i);
                        if (value instanceof byte[] bytes) {
                            value = new String(bytes, StandardCharsets.UTF_8);
                        } else if (value instanceof java.util.Date
                                || value instanceof java.time.temporal.TemporalAccessor) {
                            value = value.toString();
                        }
                        row.put(metadata.getColumnLabel(i), value);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
    private static JSONObject row(Connection c,String s,Object...a)throws SQLException{JSONArray l=rows(c,s,a);return l.isEmpty()?null:(JSONObject)l.get(0);}
    private static Number scalar(Connection c,String s,Object...a)throws SQLException{try(PreparedStatement p=c.prepareStatement(s)){bind(p,a);try(ResultSet r=p.executeQuery()){if(!r.next())return 0L;Object v=r.getObject(1);return v instanceof Number?(Number)v:0L;}}}
    private static int execute(Connection c,String s,Object...a)throws SQLException{try(PreparedStatement p=c.prepareStatement(s)){bind(p,a);return p.executeUpdate();}}
    private static void bind(PreparedStatement p,Object...a)throws SQLException{for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);}
    private static String text(Object v){return v==null?"":String.valueOf(v).trim();}
    private static String required(JSONObject r,String k){String v=text(r.get(k));if(v.isEmpty())throw new IllegalArgumentException("Thiếu dữ liệu: "+k);return v;}
    private static int integer(Object v,int f){if(v instanceof Number)return((Number)v).intValue();try{return Integer.parseInt(text(v));}catch(Exception e){return f;}}
    private static Integer intOrNull(Object v){if(v==null||text(v).isEmpty())return null;int n=integer(v,Integer.MIN_VALUE);return n==Integer.MIN_VALUE?null:n;}
    private static int nonnegative(Object v){int n=integer(v,-1);if(n<0)throw new IllegalArgumentException("Giá trị phải là số không âm");return n;}
    private static long nonnegativeLong(Object v){long n;if(v instanceof Number)n=((Number)v).longValue();else try{n=Long.parseLong(text(v));}catch(Exception e){n=-1;}if(n<0)throw new IllegalArgumentException("Giá trị phải là số không âm");return n;}
    private static int positive(Object v,String l){int n=integer(v,-1);if(n<=0)throw new IllegalArgumentException(l+" không hợp lệ");return n;}
    private static int between(Object v,int a,int b,String l){int n=integer(v,a-1);if(n<a||n>b)throw new IllegalArgumentException(l+" phải từ "+a+" đến "+b);return n;}
    private static boolean bool(Object v){return v instanceof Boolean?(Boolean)v:"1".equals(text(v))||"true".equalsIgnoreCase(text(v));}
    private static String json(Object v,String l){String s=text(v);if(s.isEmpty()||JSONValue.parse(s)==null)throw new IllegalArgumentException(l+" không phải JSON hợp lệ");return s;}
    private static String jsonArray(Object v,String l){String s=json(v,l);if(!(JSONValue.parse(s) instanceof JSONArray))throw new IllegalArgumentException(l+" phải là mảng JSON");return s;}
}
