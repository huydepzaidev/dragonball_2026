package nro.models.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.data.LocalManager;
import nro.models.map.ItemMap;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.ChatGlobalService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Logger;
import nro.models.utils.Util;

/**
 * Cầu nối cấu hình vận hành giữa control panel và game server.
 *
 * Mọi dữ liệu được nạp theo snapshot bất biến. Nếu database tạm lỗi, server tiếp
 * tục chạy bằng snapshot hợp lệ gần nhất thay vì xóa cấu hình đang dùng.
 */
public final class GameConfigService implements Runnable {

    private static final GameConfigService INSTANCE = new GameConfigService();
    private static final String DEFAULT_LOGIN_NOTICE = "X3 Kinh nghiệm đến hết ngày 11/5."
            + "\nSự kiện Goku Day."
            + "\nĐua TOP nhận quà cực khủng."
            + "\nTích điểm đổi quà."
            + "\nChi tiết xem tại diễn đàn, fanpage.";

    private volatile int dropRatePercent = 100;
    private volatile boolean autoMaintenanceEnabled;
    private volatile LocalTime maintenanceTime = LocalTime.of(4, 30);
    private volatile int maintenanceCountdownSeconds = 300;
    private volatile boolean bossWatchdogEnabled = true;
    private volatile int bossStuckSeconds = 120;
    private volatile int refreshSeconds = 5;
    private volatile boolean loginNoticeEnabled = true;
    private volatile String loginNoticeText = DEFAULT_LOGIN_NOTICE;
    private volatile Map<Integer, List<BossDropRule>> bossDrops = Collections.emptyMap();
    private volatile boolean tablesAvailable = true;
    private volatile long lastCatalogSync;
    private volatile String lastError;

    private GameConfigService() {
    }

    public static GameConfigService gI() {
        return INSTANCE;
    }

    public int getDropRatePercent() {
        return dropRatePercent;
    }

    public int getConfiguredDropRuleCount(int bossId) {
        List<BossDropRule> rules = bossDrops.get(bossId);
        return rules == null ? 0 : rules.size();
    }

    public int getConfiguredDropRuleTotal() {
        int total = 0;
        for (List<BossDropRule> rules : bossDrops.values()) {
            total += rules.size();
        }
        return total;
    }

    public boolean isAutoMaintenanceEnabled() {
        return autoMaintenanceEnabled;
    }

    public LocalTime getMaintenanceTime() {
        return maintenanceTime;
    }

    public int getMaintenanceCountdownSeconds() {
        return maintenanceCountdownSeconds;
    }

    public boolean isBossWatchdogEnabled() {
        return bossWatchdogEnabled;
    }

    public int getBossStuckSeconds() {
        return bossStuckSeconds;
    }

    public boolean isLoginNoticeEnabled() {
        return loginNoticeEnabled;
    }

    public String getLoginNoticeText() {
        return loginNoticeText;
    }

    public synchronized boolean loadNow() {
        return loadNow(false);
    }

    private synchronized boolean loadNow(boolean announceExp) {
        try (Connection con = LocalManager.getConnection()) {
            ServerConfigSnapshot config = loadServerConfig(con);
            Map<Integer, List<BossDropRule>> drops = loadBossDrops(con);
            EventControlService.gI().load(con);

            Manager.RATE_EXP_SERVER = config.expRate;
            if (announceExp) {
                String message = "Đã tăng EXP toàn server lên x" + config.expRate + "!";
                ChatGlobalService.gI().chatAdmin(message);
                Service.gI().sendThongBaoAllPlayer("Admin: " + message);
            }
            dropRatePercent = config.dropRatePercent;
            autoMaintenanceEnabled = config.autoMaintenanceEnabled;
            maintenanceTime = config.maintenanceTime;
            maintenanceCountdownSeconds = config.maintenanceCountdownSeconds;
            bossWatchdogEnabled = config.bossWatchdogEnabled;
            bossStuckSeconds = config.bossStuckSeconds;
            refreshSeconds = config.refreshSeconds;
            loginNoticeEnabled = config.loginNoticeEnabled;
            loginNoticeText = config.loginNoticeText;
            bossDrops = drops;
            tablesAvailable = true;
            lastError = null;
            updateRuntime(con, true, null);
            return true;
        } catch (Exception e) {
            tablesAvailable = false;
            lastError = compactError(e);
            Logger.error("Không thể nạp game_server_config, giữ cấu hình gần nhất: " + lastError + "\n");
            return false;
        }
    }

    private ServerConfigSnapshot loadServerConfig(Connection con) throws SQLException {
        String sql = "SELECT exp_rate, drop_rate_percent, auto_maintenance_enabled, "
                + "maintenance_time, maintenance_countdown_seconds, boss_watchdog_enabled, "
                + "boss_stuck_seconds, config_refresh_seconds, login_notice_enabled, "
                + "login_notice_text "
                + "FROM game_server_config WHERE id = 1";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("Thiếu dòng game_server_config id=1");
            }
            int expRate = clamp(rs.getInt("exp_rate"), 1, 100);
            int dropRate = clamp(rs.getInt("drop_rate_percent"), 0, 1000);
            Time dbTime = rs.getTime("maintenance_time");
            LocalTime time = dbTime == null ? LocalTime.of(4, 30) : dbTime.toLocalTime();
            return new ServerConfigSnapshot(
                    expRate,
                    dropRate,
                    rs.getBoolean("auto_maintenance_enabled"),
                    time,
                    clamp(rs.getInt("maintenance_countdown_seconds"), 10, 3600),
                    rs.getBoolean("boss_watchdog_enabled"),
                    clamp(rs.getInt("boss_stuck_seconds"), 10, 3600),
                    clamp(rs.getInt("config_refresh_seconds"), 2, 60),
                    rs.getBoolean("login_notice_enabled"),
                    normalizeLoginNotice(rs.getString("login_notice_text"))
            );
        }
    }

    private static String normalizeLoginNotice(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LOGIN_NOTICE;
        }
        return trim(value.replace("\r\n", "\n").replace('\r', '\n'), 1000);
    }

    private Map<Integer, List<BossDropRule>> loadBossDrops(Connection con) throws SQLException {
        String sql = "SELECT d.id, d.boss_id, d.drop_kind, d.item_id, d.chance_bp, "
                + "d.quantity_min, d.quantity_max "
                + "FROM game_boss_drop d "
                + "LEFT JOIN item_template i ON i.id = d.item_id "
                + "WHERE d.enabled = 1 "
                + "AND (d.drop_kind = 'DIVINE_RANDOM' OR i.id IS NOT NULL) "
                + "ORDER BY d.boss_id, d.id";
        Map<Integer, List<BossDropRule>> mutable = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String kind = rs.getString("drop_kind");
                BossDropRule rule = new BossDropRule(
                        rs.getLong("id"),
                        "DIVINE_RANDOM".equals(kind),
                        rs.getInt("item_id"),
                        clamp(rs.getInt("chance_bp"), 0, 10000),
                        clamp(rs.getInt("quantity_min"), 1, 9999),
                        clamp(rs.getInt("quantity_max"), 1, 9999)
                );
                mutable.computeIfAbsent(rs.getInt("boss_id"), key -> new ArrayList<>()).add(rule);
            }
        }
        Map<Integer, List<BossDropRule>> snapshot = new HashMap<>();
        for (Map.Entry<Integer, List<BossDropRule>> entry : mutable.entrySet()) {
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Gọi từ Boss.setDie(), bảo đảm chỉ chạy một lần cho mỗi vòng đời boss.
     */
    public void dropConfiguredRewards(Boss boss, Player killer) {
        Player owner = resolveRewardOwner(killer);
        if (boss == null || owner == null || boss.zone == null) {
            return;
        }
        List<BossDropRule> rules = bossDrops.get((int) boss.id);
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (BossDropRule rule : rules) {
            if (rule.divineRandom && isEncounterManagedDivineBoss((int) boss.id)) {
                continue;
            }
            try {
                int chance = calculateAdjustedChance(rule.chanceBp, dropRatePercent);
                int roll = ThreadLocalRandom.current().nextInt(10000);
                if (!passesChance(chance, roll)) {
                    continue;
                }
                int quantity = rule.quantityMin == rule.quantityMax
                        ? rule.quantityMin : Util.nextInt(rule.quantityMin, rule.quantityMax);
                int x = boss.location == null ? 0 : boss.location.x;
                int rawY = boss.location == null ? 0 : boss.location.y - 24;
                int y = boss.zone.map == null ? rawY : boss.zone.map.yPhysicInTop(x, rawY);
                if (rule.divineRandom) {
                    // Equipment cannot be stacked. quantity=N creates N independent
                    // divine items so every item receives its own random type/options.
                    for (int i = 0; i < quantity; i++) {
                        int dropX = quantity == 1 ? x : x + ThreadLocalRandom.current().nextInt(-25, 26);
                        int dropY = boss.zone.map == null
                                ? y : boss.zone.map.yPhysicInTop(dropX, rawY);
                        ItemMap item = ItemService.gI().randDoTLBoss(
                                boss.zone, 1, dropX, dropY, owner.id);
                        publishConfiguredDrop(boss, owner, rule, item, chance, roll);
                    }
                } else {
                    ItemMap item = new ItemMap(boss.zone, rule.itemId, quantity, x, y, owner.id);
                    publishConfiguredDrop(boss, owner, rule, item, chance, roll);
                }
            } catch (Exception e) {
                reportBossError(boss, "drop rule #" + rule.id, e);
            }
        }
    }

    /**
     * These bosses award exactly one divine item through their encounter state,
     * so their legacy per-kill DIVINE_RANDOM database rule must not roll again.
     */
    static boolean isEncounterManagedDivineBoss(int bossId) {
        return bossId == BossID.XEN_BO_HUNG || bossId == BossID.SIEU_BO_HUNG
                || bossId == BossID.GOD_BILL || bossId == BossID.ANGEL_WHIS
                || bossId == BossID.GOD_CHAMPA || bossId == BossID.ANGEL_VADOS
                || bossId == BossID.PILAP || bossId == BossID.MAI_PILAP
                || bossId == BossID.PU_PILAP
                || bossId == BossID.SOI_DO_VO_TINH
                || bossId == BossID.SOI_VANG_VO_TINH
                || bossId == BossID.SOI_XANH_XAM_VO_TINH
                || bossId == BossID.ZAMASU;
    }

    public boolean dropGuaranteedDivine(Boss boss, Player killer, String encounterName) {
        Player owner = resolveRewardOwner(killer);
        if (boss == null || owner == null || boss.zone == null) {
            return false;
        }
        int x = boss.location == null ? 0 : boss.location.x;
        int rawY = boss.location == null ? 0 : boss.location.y - 24;
        int y = boss.zone.map == null ? rawY : boss.zone.map.yPhysicInTop(x, rawY);
        ItemMap item = ItemService.gI().randDoTLBoss(boss.zone, 1, x, y, owner.id);
        if (item == null || item.itemTemplate == null) {
            return false;
        }
        Service.gI().dropItemMap(boss.zone, item);
        Logger.successln("[ENCOUNTER DIVINE] encounter=" + encounterName
                + " boss=" + boss.id
                + " item=" + item.itemTemplate.id
                + " owner=" + owner.id);
        return true;
    }

    private void publishConfiguredDrop(Boss boss, Player owner, BossDropRule rule,
            ItemMap item, int chance, int roll) {
        if (item == null || item.itemTemplate == null) {
            throw new IllegalStateException("Cannot create item for drop rule #" + rule.id);
        }
        Service.gI().dropItemMap(boss.zone, item);
        Logger.successln("[DB DROP] boss=" + boss.id
                + " rule=" + rule.id
                + " item=" + item.itemTemplate.id
                + " quantity=" + item.quantity
                + " owner=" + owner.id
                + " chance=" + chance + "/10000"
                + " roll=" + roll);
    }

    private static Player resolveRewardOwner(Player attacker) {
        if (attacker == null || attacker.isBot) {
            return null;
        }
        if (attacker instanceof Pet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        if (attacker instanceof NewPet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        return attacker.isPl() ? attacker : null;
    }

    static int calculateAdjustedChance(int chanceBp, int ratePercent) {
        long adjusted = (long) chanceBp * ratePercent / 100L;
        return (int) Math.min(10000L, Math.max(0L, adjusted));
    }

    static boolean passesChance(int chance, int roll) {
        return chance > 0 && roll >= 0 && roll < 10000 && roll < chance;
    }

    public void reportBossError(Boss boss, String phase, Throwable error) {
        String bossInfo = boss == null ? "unknown" : boss.id + "/" + safeBossName(boss);
        lastError = "Boss " + bossInfo + " " + phase + ": " + compactError(error);
        Logger.error(lastError + "\n");
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_server_runtime SET last_error=?, updated_at=CURRENT_TIMESTAMP WHERE id=1")) {
            ps.setString(1, trim(lastError, 500));
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run() {
        long nextConfigLoad = 0L;
        while (ServerManager.isRunning) {
            long started = System.currentTimeMillis();
            try {
                long now = System.currentTimeMillis();
                if (now >= nextConfigLoad) {
                    loadNow();
                    nextConfigLoad = now + Math.max(2, refreshSeconds) * 1000L;
                }
                if (tablesAvailable) {
                    processCommands();
                    EventControlService.gI().processCommands();
                    if (System.currentTimeMillis() - lastCatalogSync >= 30_000L) {
                        syncBossCatalog();
                        lastCatalogSync = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                lastError = compactError(e);
                Logger.error("Lỗi luồng đồng bộ cấu hình game: " + lastError + "\n");
            }
            long wait = Math.max(250L, Math.min(500L,
                    nextConfigLoad - System.currentTimeMillis()));
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        markOffline();
    }

    private void processCommands() {
        String select = "SELECT id, command_type, boss_id FROM game_server_command "
                + "WHERE status='PENDING' ORDER BY id LIMIT 10";
        List<ServerCommand> commands = new ArrayList<>();
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(select);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                commands.add(new ServerCommand(
                        rs.getLong("id"),
                        rs.getString("command_type"),
                        (Integer) rs.getObject("boss_id")
                ));
            }
        } catch (Exception e) {
            Logger.error("Không thể đọc hàng đợi lệnh server: " + compactError(e) + "\n");
            return;
        }
        // Pool hiện tại chỉ có một connection; xử lý sau khi đóng ResultSet/Connection
        // để lệnh không tự chờ chính connection đang bị giữ.
        for (ServerCommand command : commands) {
            processCommand(command);
        }
    }

    private void processCommand(ServerCommand command) {
        if (!claimCommand(command.id)) {
            return;
        }
        boolean success = false;
        String message;
        try {
            switch (command.type) {
                case "RELOAD_CONFIG" -> {
                    success = loadNow(true);
                    message = success ? "Đã nạp lại cấu hình." : "Nạp cấu hình thất bại.";
                }
                case "RESPAWN_BOSS" -> {
                    int count = BossManager.respawnBossesEverywhere(command.bossId);
                    success = count > 0;
                    message = success
                            ? "Đã gọi lại " + count + " boss ID " + command.bossId + "."
                            : "Không tìm thấy hoặc không thể tạo boss ID " + command.bossId + ".";
                }
                case "RESPAWN_ALL" -> {
                    int count = BossManager.respawnAllBossesEverywhere();
                    success = count > 0;
                    message = "Đã làm mới " + count + " boss.";
                }
                default -> message = "Lệnh không được hỗ trợ.";
            }
        } catch (Exception e) {
            message = compactError(e);
        }
        finishCommand(command.id, success, message);
    }

    private boolean claimCommand(long id) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_server_command SET status='PROCESSING', started_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND status='PENDING'")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void finishCommand(long id, boolean success, String message) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_server_command SET status=?, result_message=?, "
                        + "finished_at=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setString(1, success ? "DONE" : "FAILED");
            ps.setString(2, trim(message, 255));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error("Không thể cập nhật kết quả lệnh #" + id + "\n");
        }
    }

    private void syncBossCatalog() {
        Map<Integer, BossCatalogEntry> catalog = new HashMap<>();
        for (Boss boss : BossManager.getAllManagedBosses()) {
            if (boss == null) {
                continue;
            }
            BossCatalogEntry entry = catalog.computeIfAbsent(
                    (int) boss.id, id -> new BossCatalogEntry(id, safeBossName(boss)));
            entry.instances++;
        }
        String sql = "INSERT INTO game_boss_catalog "
                + "(boss_id, boss_name, active_instances, last_seen_at) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE boss_name=VALUES(boss_name), "
                + "active_instances=VALUES(active_instances), last_seen_at=VALUES(last_seen_at)";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement reset = con.prepareStatement("UPDATE game_boss_catalog SET active_instances=0");
                PreparedStatement ps = con.prepareStatement(sql)) {
            con.setAutoCommit(false);
            reset.executeUpdate();
            for (BossCatalogEntry entry : catalog.values()) {
                ps.setInt(1, entry.id);
                ps.setString(2, trim(entry.name, 100));
                ps.setInt(3, entry.instances);
                ps.addBatch();
            }
            ps.executeBatch();
            updateRuntime(con, true, lastError);
            con.commit();
        } catch (Exception e) {
            Logger.error("Không thể đồng bộ danh mục boss: " + compactError(e) + "\n");
        }
    }

    private void updateRuntime(Connection con, boolean online, String error) throws SQLException {
        String sql = "INSERT INTO game_server_runtime "
                + "(id, server_online, boss_count, last_heartbeat, last_config_load, last_error) "
                + "VALUES (1, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?) "
                + "ON DUPLICATE KEY UPDATE server_online=VALUES(server_online), "
                + "boss_count=VALUES(boss_count), last_heartbeat=VALUES(last_heartbeat), "
                + "last_config_load=VALUES(last_config_load), last_error=VALUES(last_error)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, online);
            ps.setInt(2, BossManager.getAllManagedBosses().size());
            ps.setString(3, error == null ? null : trim(error, 500));
            ps.executeUpdate();
        }
    }

    public void markOffline() {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_server_runtime SET server_online=0, updated_at=CURRENT_TIMESTAMP WHERE id=1")) {
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static String safeBossName(Boss boss) {
        try {
            if (boss.name != null && !boss.name.isBlank()) {
                return boss.name;
            }
            if (boss.data != null && boss.data.length > 0) {
                return boss.data[0].getName().replace("%1$s", "").trim();
            }
        } catch (Exception ignored) {
        }
        return "Boss " + boss.id;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String compactError(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        if (message == null || message.isBlank()) {
            message = error == null ? "Unknown error" : error.getClass().getSimpleName();
        }
        return trim(message.replace('\r', ' ').replace('\n', ' '), 500);
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ServerConfigSnapshot(
            int expRate,
            int dropRatePercent,
            boolean autoMaintenanceEnabled,
            LocalTime maintenanceTime,
            int maintenanceCountdownSeconds,
            boolean bossWatchdogEnabled,
            int bossStuckSeconds,
            int refreshSeconds,
            boolean loginNoticeEnabled,
            String loginNoticeText) {
    }

    private record BossDropRule(
            long id,
            boolean divineRandom,
            int itemId,
            int chanceBp,
            int quantityMin,
            int quantityMax) {
    }

    private record ServerCommand(long id, String type, Integer bossId) {
    }

    private static final class BossCatalogEntry {

        private final int id;
        private final String name;
        private int instances;

        private BossCatalogEntry(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
