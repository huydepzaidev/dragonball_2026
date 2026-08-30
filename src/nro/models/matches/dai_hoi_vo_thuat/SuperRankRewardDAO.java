package nro.models.matches.dai_hoi_vo_thuat;

import nro.models.consts.ConstTaskBadges;
import nro.models.data.LocalManager;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.task.BadgesTask;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SuperRankRewardDAO {

    public static final long MAX_RUBY_LIMIT = 2_000_000_000L;

    public record ClaimResult(boolean success, int grantedRuby, String message) {
        public static ClaimResult success(int grantedRuby) {
            return new ClaimResult(true, grantedRuby, "Success");
        }
        public static ClaimResult failed(String message) {
            return new ClaimResult(false, 0, message);
        }
    }

    public record RewardCycle(long id, LocalDate rewardDate, String status, Integer snapshotCount,
                              String snapshotChecksum, int totalWinners, int processedWinners,
                              int attempts, Date lastAttemptAt, String errorMessage) {}

    public record LedgerEntry(long id, long cycleId, LocalDate rewardDate, int playerId,
                              int accountId, int rankPosition, int rubyReward, int rubyGranted,
                              String rubyStatus, String badgeStatus, Long mailboxId,
                              String mailboxStatus, String errorMessage) {}

    public static ClaimResult claimRewardAtomic(Player player, long ledgerId) {
        if (player == null) {
            return ClaimResult.failed("Player is null");
        }

        synchronized (player) {
            int grantedRuby = 0;
            int nextRubyValue = 0;
            int nextTaskCount = 0;
            boolean isTop1 = false;

            int curRuby = player.inventory.ruby;
            if (curRuby < 0 || curRuby > MAX_RUBY_LIMIT) {
                Logger.error("[CLAIM ERROR] curRuby ngoài phạm vi [0..2 tỷ]: " + curRuby + " cho player_id=" + player.id);
                return ClaimResult.failed("curRuby out of range [0..2B]");
            }

            try (Connection con = LocalManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    String sqlLedger = "SELECT rank_position, ruby_reward, ruby_status FROM super_rank_reward_ledger WHERE id = ? AND player_id = ? FOR UPDATE";
                    int rubyReward = 0;
                    int rankPosition = 0;

                    try (PreparedStatement ps = con.prepareStatement(sqlLedger)) {
                        ps.setLong(1, ledgerId);
                        ps.setLong(2, player.id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !"PENDING".equals(rs.getString("ruby_status"))) {
                                con.rollback();
                                return ClaimResult.failed("Ledger row not pending or not found");
                            }
                            rubyReward = rs.getInt("ruby_reward");
                            rankPosition = rs.getInt("rank_position");
                        }
                    }

                    if (rubyReward < 0) {
                        con.rollback();
                        Logger.error("[CLAIM ERROR] ruby_reward bị âm: " + rubyReward + " cho player_id=" + player.id + ", ledger_id=" + ledgerId);
                        return ClaimResult.failed("Invalid negative ruby_reward");
                    }

                    isTop1 = (rankPosition == 1);

                    long calculated = Math.min((long) curRuby + (long) rubyReward, MAX_RUBY_LIMIT);
                    if (calculated < 0 || calculated > MAX_RUBY_LIMIT) {
                        con.rollback();
                        Logger.error("[CLAIM ERROR] nextRuby ngoài phạm vi [0..2 tỷ]: " + calculated + " cho player_id=" + player.id);
                        return ClaimResult.failed("Calculated ruby out of range [0..2B]");
                    }
                    grantedRuby = (int) (calculated - curRuby);
                    if (grantedRuby < 0) {
                        con.rollback();
                        Logger.error("[CLAIM ERROR] grantedRuby bị âm: " + grantedRuby + " cho player_id=" + player.id);
                        return ClaimResult.failed("Invalid negative grantedRuby");
                    }
                    nextRubyValue = (int) calculated;

                    String newBadgesJson = null;
                    if (isTop1) {
                        if (player.dataTaskBadges == null || player.dataTaskBadges.isEmpty()) {
                            con.rollback();
                            Logger.error("[CLAIM ERROR] dataTaskBadges của player_id=" + player.id + " bị null hoặc rỗng");
                            return ClaimResult.failed("dataTaskBadges is empty or malformed");
                        }
                        boolean foundTask = false;
                        for (BadgesTask task : player.dataTaskBadges) {
                            if (task.id == ConstTaskBadges.CAO_THU_SIEU_HANG) {
                                nextTaskCount = Math.min(task.count + 1, task.countMax);
                                foundTask = true;
                                break;
                            }
                        }
                        if (!foundTask) {
                            con.rollback();
                            Logger.error("[CLAIM ERROR] Không tìm thấy Task CAO_THU_SIEU_HANG (ID 5) của player_id=" + player.id);
                            return ClaimResult.failed("Task CAO_THU_SIEU_HANG (ID 5) not found");
                        }
                        newBadgesJson = serializeBadgesTaskWithCount(player.dataTaskBadges, ConstTaskBadges.CAO_THU_SIEU_HANG, nextTaskCount);
                        if (newBadgesJson == null || newBadgesJson.trim().isEmpty() || !newBadgesJson.startsWith("[")) {
                            con.rollback();
                            Logger.error("[CLAIM ERROR] newBadgesJson bị malformed: " + newBadgesJson + " cho player_id=" + player.id);
                            return ClaimResult.failed("newBadgesJson is invalid");
                        }
                    }

                    String sqlUpdatePlayer = isTop1
                            ? "UPDATE `player` SET `data_inventory` = JSON_SET(`data_inventory`, '$[2]', ?), `dataTaskBadges` = ? "
                            + "WHERE `id` = ? AND JSON_VALID(`data_inventory`) = 1 AND JSON_LENGTH(`data_inventory`) >= 3"
                            : "UPDATE `player` SET `data_inventory` = JSON_SET(`data_inventory`, '$[2]', ?) "
                            + "WHERE `id` = ? AND JSON_VALID(`data_inventory`) = 1 AND JSON_LENGTH(`data_inventory`) >= 3";

                    try (PreparedStatement ps = con.prepareStatement(sqlUpdatePlayer)) {
                        ps.setInt(1, nextRubyValue);
                        if (isTop1) {
                            ps.setString(2, newBadgesJson);
                            ps.setLong(3, player.id);
                        } else {
                            ps.setLong(2, player.id);
                        }
                        int rowsPlayer = ps.executeUpdate();
                        if (rowsPlayer != 1) {
                            con.rollback();
                            Logger.error("[CLAIM ERROR] UPDATE player thất bại (affectedRows=" + rowsPlayer + ") cho player_id=" + player.id
                                    + ", ledger_id=" + ledgerId + ". Lý do nghi ngờ: player không tồn tại hoặc data_inventory malformed/length < 3");
                            return ClaimResult.failed("Player record missing or data_inventory malformed (affectedRows != 1)");
                        }
                    }

                    String sqlUpdateLedger = "UPDATE super_rank_reward_ledger SET "
                            + "ruby_status = 'CLAIMED', ruby_granted = ?, ruby_claimed_at = NOW(), "
                            + "badge_status = (CASE WHEN badge_status = 'PENDING' THEN 'CLAIMED' ELSE badge_status END), "
                            + "badge_claimed_at = (CASE WHEN badge_status = 'PENDING' THEN NOW() ELSE badge_claimed_at END) "
                            + "WHERE id = ? AND ruby_status = 'PENDING'";
                    try (PreparedStatement ps = con.prepareStatement(sqlUpdateLedger)) {
                        ps.setInt(1, grantedRuby);
                        ps.setLong(2, ledgerId);
                        int rowsLedger = ps.executeUpdate();
                        if (rowsLedger != 1) {
                            con.rollback();
                            Logger.error("[CLAIM ERROR] UPDATE ledger thất bại (affectedRows=" + rowsLedger + ") cho ledger_id=" + ledgerId);
                            return ClaimResult.failed("Concurrent claim collision (ledger affectedRows != 1)");
                        }
                    }

                    con.commit();

                } catch (Exception ex) {
                    con.rollback();
                    Logger.logException(SuperRankRewardDAO.class, ex);
                    return ClaimResult.failed(ex.getMessage());
                }
            } catch (SQLException ex) {
                Logger.logException(SuperRankRewardDAO.class, ex);
                return ClaimResult.failed(ex.getMessage());
            }

            player.inventory.ruby = nextRubyValue;
            if (isTop1) {
                for (BadgesTask task : player.dataTaskBadges) {
                    if (task.id == ConstTaskBadges.CAO_THU_SIEU_HANG) {
                        task.count = nextTaskCount;
                        break;
                    }
                }
            }

            if (player.getSession() != null) {
                Service.gI().sendMoney(player);
                if (grantedRuby > 0) {
                    Service.gI().sendThongBao(player, "Bạn nhận được " + Util.formatNumber(grantedRuby) + " Hồng Ngọc từ Giải Siêu Hạng!");
                } else {
                    Service.gI().sendThongBao(player, "Hồng Ngọc của bạn đã đạt tối đa (2 tỷ), không thể nhận thêm!");
                }
            }

            return ClaimResult.success(grantedRuby);
        }
    }

    public static String serializeBadgesTaskWithCount(List<BadgesTask> list, int targetTaskId, int newCount) {
        JSONArray array = new JSONArray();
        for (BadgesTask task : list) {
            JSONObject obj = new JSONObject();
            obj.put("id", task.id);
            obj.put("count", (task.id == targetTaskId) ? newCount : task.count);
            obj.put("countMax", task.countMax);
            obj.put("idBadgesReward", task.idBadgesReward);
            array.add(obj);
        }
        return array.toJSONString();
    }

    public static RewardCycle getCycleByDate(LocalDate date) {
        String sql = "SELECT * FROM super_rank_reward_cycle WHERE reward_date = ?";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCycle(rs);
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return null;
    }

    public static RewardCycle getCycleById(long id) {
        String sql = "SELECT * FROM super_rank_reward_cycle WHERE id = ?";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCycle(rs);
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return null;
    }

    public static long createCycleTaking(LocalDate date) {
        String sql = "INSERT INTO super_rank_reward_cycle (reward_date, snapshot_at, status, attempts, last_attempt_at) "
                   + "VALUES (?, NOW(), 'SNAPSHOT_TAKING', 1, NOW())";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            int affected = ps.executeUpdate();
            if (affected == 1) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                return -1; // Duplicate key from concurrent process
            }
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return -1;
    }

    public static int countLedgerByCycleId(long cycleId) {
        String sql = "SELECT COUNT(*) FROM super_rank_reward_ledger WHERE cycle_id = ?";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cycleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return 0;
    }

    public static void incrementCycleAttempts(long cycleId) {
        String sql = "UPDATE super_rank_reward_cycle SET attempts = attempts + 1, last_attempt_at = NOW() WHERE id = ?";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cycleId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
    }

    public static List<LedgerEntry> getPendingMailboxLedgers() {
        List<LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM super_rank_reward_ledger WHERE rank_position = 1 AND mailbox_status IN ('PENDING', 'FAILED')";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapLedger(rs));
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return list;
    }

    public static void updateMailboxStatus(long ledgerId, long mailboxId, String status, String error) {
        String sql = "UPDATE super_rank_reward_ledger SET mailbox_id = ?, mailbox_status = ?, error_message = ? WHERE id = ?";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (mailboxId > 0) {
                ps.setLong(1, mailboxId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, status);
            ps.setString(3, error);
            ps.setLong(4, ledgerId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
    }

    public static List<LedgerEntry> getPendingLedgersByPlayerId(long playerId) {
        List<LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM super_rank_reward_ledger WHERE player_id = ? AND ruby_status = 'PENDING' ORDER BY id ASC";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLedger(rs));
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardDAO.class, ex);
        }
        return list;
    }

    public static void claimPendingRewardOnLogin(Player player) {
        if (player == null) return;
        List<LedgerEntry> pending = getPendingLedgersByPlayerId(player.id);
        for (LedgerEntry entry : pending) {
            ClaimResult res = claimRewardAtomic(player, entry.id);
            if (res.success) {
                Logger.success("[LOGIN CLAIM] Player " + player.name + " (" + player.id + ") đã nhận bù " + res.grantedRuby + " Ruby từ kỳ " + entry.rewardDate);
            } else {
                Logger.error("[LOGIN CLAIM] Player " + player.name + " (" + player.id + ") claim bù thất bại kỳ " + entry.rewardDate + ": " + res.message);
            }
        }
    }

    private static RewardCycle mapCycle(ResultSet rs) throws SQLException {
        Integer snapCount = rs.getObject("snapshot_count") != null ? rs.getInt("snapshot_count") : null;
        return new RewardCycle(
                rs.getLong("id"),
                rs.getDate("reward_date").toLocalDate(),
                rs.getString("status"),
                snapCount,
                rs.getString("snapshot_checksum"),
                rs.getInt("total_winners"),
                rs.getInt("processed_winners"),
                rs.getInt("attempts"),
                rs.getTimestamp("last_attempt_at"),
                rs.getString("error_message")
        );
    }

    private static LedgerEntry mapLedger(ResultSet rs) throws SQLException {
        Long mId = rs.getObject("mailbox_id") != null ? rs.getLong("mailbox_id") : null;
        return new LedgerEntry(
                rs.getLong("id"),
                rs.getLong("cycle_id"),
                rs.getDate("reward_date").toLocalDate(),
                rs.getInt("player_id"),
                rs.getInt("account_id"),
                rs.getInt("rank_position"),
                rs.getInt("ruby_reward"),
                rs.getInt("ruby_granted"),
                rs.getString("ruby_status"),
                rs.getString("badge_status"),
                mId,
                rs.getString("mailbox_status"),
                rs.getString("error_message")
        );
    }
}
