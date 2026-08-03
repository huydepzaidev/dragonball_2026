package nro.models.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.data.LocalManager;

/**
 * Smoke test không khởi động socket game. Dùng để kiểm tra schema và khả năng
 * đọc cấu hình thực tế bằng đúng connection pool của server.
 */
public final class GameConfigServiceSmokeTest {

    private GameConfigServiceSmokeTest() {
    }

    public static void main(String[] args) {
        GameConfigService service = GameConfigService.gI();
        try {
            if (!service.loadNow()) {
                throw new AssertionError("Không thể nạp game_server_config");
            }
            if (Manager.RATE_EXP_SERVER < 1 || Manager.RATE_EXP_SERVER > 100) {
                throw new AssertionError("EXP ngoài phạm vi an toàn");
            }
            if (service.getDropRatePercent() < 0 || service.getDropRatePercent() > 1000) {
                throw new AssertionError("Hệ số drop ngoài phạm vi an toàn");
            }
            if (service.getMaintenanceTime() == null) {
                throw new AssertionError("Thiếu giờ bảo trì");
            }
            if (GameConfigService.calculateAdjustedChance(10000, 100) != 10000) {
                throw new AssertionError("Rule 100% không được giữ nguyên");
            }
            for (int roll = 0; roll < 10000; roll++) {
                if (!GameConfigService.passesChance(10000, roll)) {
                    throw new AssertionError("Rule 100% bị trượt ở roll " + roll);
                }
            }
            if (GameConfigService.passesChance(0, 0)) {
                throw new AssertionError("Rule 0% vẫn có thể rơi");
            }
            try (Connection con = LocalManager.getConnection();
                    PreparedStatement ps = con.prepareStatement(
                            "SELECT COUNT(*), COUNT(DISTINCT boss_id), COUNT(DISTINCT boss_key) "
                            + "FROM game_boss_catalog");
                    ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 94 || rs.getInt(2) != 94 || rs.getInt(3) != 94) {
                    throw new AssertionError("Danh mục boss không đủ 94 ID/key duy nhất");
                }
            } catch (Exception e) {
                throw new AssertionError("Không kiểm tra được danh mục boss", e);
            }
            int databaseRuleTotal = 0;
            try (Connection con = LocalManager.getConnection();
                    PreparedStatement ps = con.prepareStatement(
                            "SELECT boss_id, COUNT(*) FROM game_boss_drop d "
                            + "LEFT JOIN item_template i ON i.id=d.item_id "
                            + "WHERE d.enabled=1 AND (d.drop_kind='DIVINE_RANDOM' OR i.id IS NOT NULL) "
                            + "GROUP BY boss_id");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bossId = rs.getInt(1);
                    int count = rs.getInt(2);
                    databaseRuleTotal += count;
                    if (service.getConfiguredDropRuleCount(bossId) != count) {
                        throw new AssertionError("Sai số rule đã nạp của boss " + bossId);
                    }
                }
                if (service.getConfiguredDropRuleTotal() != databaseRuleTotal) {
                    throw new AssertionError("Server chưa nạp đủ rule rơi đồ");
                }
            } catch (Exception e) {
                throw new AssertionError("Không đối chiếu được rule rơi đồ", e);
            }
            if (BossManager.respawnBossesEverywhere(-108) != 0
                    || !BossManager.getAllManagedBosses().isEmpty()) {
                throw new AssertionError("Cứu hộ đã tạo boss vắng mặt ngoài ý muốn");
            }
            System.out.println("GAME_CONFIG_SMOKE_TEST_OK");
        } finally {
            service.markOffline();
        }
        System.exit(0);
    }
}
