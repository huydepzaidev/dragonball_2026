package nro.models.server.control.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.data.LocalManager;
import nro.models.server.Client;
import nro.models.server.GameConfigService;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.server.ServerManager;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.http.JsonResponse;
import org.json.simple.JSONObject;

public final class DashboardHandler {

    public static void handle(HttpExchange exchange, AdminUser user) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.error(exchange, 405, "Method Not Allowed");
            return;
        }

        JSONObject data = new JSONObject();

        // 1. Live In-Memory Server Metrics
        int onlinePlayers = Client.gI().getPlayers().size();
        data.put("online_players", onlinePlayers);
        data.put("max_players", Manager.MAX_PLAYER);
        data.put("server_name", ServerManager.NAME);
        data.put("server_ip", ServerManager.IP);
        data.put("server_port", ServerManager.PORT);
        data.put("time_start", ServerManager.timeStart);

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        data.put("uptime_seconds", uptimeMs / 1000);
        data.put("uptime_formatted", formatUptime(uptimeMs));

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        JSONObject memory = new JSONObject();
        memory.put("used_mb", usedMemory / (1024 * 1024));
        memory.put("free_mb", freeMemory / (1024 * 1024));
        memory.put("total_mb", totalMemory / (1024 * 1024));
        memory.put("max_mb", maxMemory / (1024 * 1024));
        data.put("memory", memory);

        data.put("available_processors", runtime.availableProcessors());
        data.put("active_threads", Thread.activeCount());

        // 2. Server Operation Flags
        data.put("maintenance_running", Maintenance.isRunning);
        data.put("admin_only_mode", Maintenance.isAdminOnlyMode());
        data.put("exp_rate", Manager.RATE_EXP_SERVER);
        data.put("drop_rate_percent", GameConfigService.gI().getDropRatePercent());
        data.put("auto_maintenance_enabled", GameConfigService.gI().isAutoMaintenanceEnabled());
        data.put("boss_watchdog_enabled", GameConfigService.gI().isBossWatchdogEnabled());

        // 3. Database Aggregated Metrics
        int totalAccounts = 0;
        int totalPlayers = 0;
        int bannedAccounts = 0;
        int todayAccounts = 0;
        int adminAccounts = 0;
        long totalRevenue = 0;

        String query = "SELECT "
                + "(SELECT COUNT(*) FROM account) as total_acc, "
                + "(SELECT COUNT(*) FROM player) as total_ply, "
                + "(SELECT COUNT(*) FROM account WHERE ban = 1) as banned_acc, "
                + "(SELECT COUNT(*) FROM account WHERE DATE(create_time) = CURDATE()) as today_acc, "
                + "(SELECT COUNT(*) FROM account WHERE is_admin >= 1) as admin_acc";

        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                totalAccounts = rs.getInt("total_acc");
                totalPlayers = rs.getInt("total_ply");
                bannedAccounts = rs.getInt("banned_acc");
                todayAccounts = rs.getInt("today_acc");
                adminAccounts = rs.getInt("admin_acc");
            }
        } catch (SQLException ignored) {}

        try (Connection con = LocalManager.getConnection()) {
            String revSql = "SELECT "
                    + "(SELECT COALESCE(SUM(final_credited_amount), 0) FROM payments WHERE is_credited = 1) + "
                    + "(SELECT COALESCE(SUM(amount), 0) FROM bank_transfers WHERE is_credited = 1) as total_rev";
            try (PreparedStatement ps = con.prepareStatement(revSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getLong("total_rev");
                }
            }
        } catch (SQLException ignored) {}

        JSONObject dbStats = new JSONObject();
        dbStats.put("total_accounts", totalAccounts);
        dbStats.put("total_players", totalPlayers);
        dbStats.put("banned_accounts", bannedAccounts);
        dbStats.put("today_registered", todayAccounts);
        dbStats.put("admin_accounts", adminAccounts);
        dbStats.put("total_revenue", totalRevenue);
        data.put("db_stats", dbStats);

        JsonResponse.ok(exchange, data);
    }

    private static String formatUptime(long uptimeMs) {
        long sec = uptimeMs / 1000;
        long hours = sec / 3600;
        long minutes = (sec % 3600) / 60;
        long seconds = sec % 60;
        long days = hours / 24;
        hours = hours % 24;

        if (days > 0) {
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
