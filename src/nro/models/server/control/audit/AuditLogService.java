package nro.models.server.control.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nro.models.data.LocalManager;
import nro.models.server.control.auth.AdminUser;
import nro.models.utils.Logger;

public final class AuditLogService {

    private static final AuditLogService INSTANCE = new AuditLogService();
    private final ExecutorService auditExecutor = Executors.newSingleThreadExecutor();

    private AuditLogService() {}

    public static AuditLogService gI() {
        return INSTANCE;
    }

    public void log(AdminUser admin, String action, String targetType, Integer targetId, String detailJson, String ip) {
        if (admin == null) return;
        final int adminId = admin.getId();
        final String adminUser = admin.getUsername();

        auditExecutor.submit(() -> {
            String sql = "INSERT INTO admin_audit_log (admin_id, admin_username, action_name, target_type, target_id, detail_json, ip_address, created_at) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, adminId);
                ps.setString(2, adminUser);
                ps.setString(3, action);
                ps.setString(4, targetType != null ? targetType : "SYSTEM");
                if (targetId != null) {
                    ps.setInt(5, targetId);
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
                ps.setString(6, detailJson != null ? detailJson : "{}");
                ps.setString(7, ip != null ? ip : "127.0.0.1");
                ps.executeUpdate();
            } catch (SQLException e) {
                Logger.log(Logger.RED, "Lỗi ghi audit log: " + e.getMessage() + "\n");
            }
        });
    }
}
