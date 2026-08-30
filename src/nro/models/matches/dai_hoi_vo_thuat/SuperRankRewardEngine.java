package nro.models.matches.dai_hoi_vo_thuat;

import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.MailboxService;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuperRankRewardEngine {

    public static final String DISTRIBUTED_RANK_LOCK = "nro_super_rank_gate";

    public enum Phase2Result {
        SUCCESS,
        ALREADY_COMPLETED,
        FAILED_ALREADY,
        LOCK_TIMEOUT,
        RETRYABLE_DATABASE_ERROR,
        VALIDATION_FAILED,
        CORRUPTED_CYCLE
    }

    @FunctionalInterface
    public interface RankDatabaseAction {
        void execute(Connection con) throws Exception;
    }

    public record RankEntry(int playerId, int accountId, int rank, String name) {}

    public static boolean runWithRankLock(int timeoutSeconds, RankDatabaseAction action) {
        try (Connection con = LocalManager.getConnection()) {
            String lockSql = "SELECT GET_LOCK(?, ?)";
            try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                ps.setString(1, DISTRIBUTED_RANK_LOCK);
                ps.setInt(2, timeoutSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) {
                        return false;
                    }
                }
            }

            try {
                con.setAutoCommit(false);
                action.execute(con);
                con.commit();
                return true;
            } catch (Exception ex) {
                con.rollback();
                Logger.logException(SuperRankRewardEngine.class, ex);
                return false;
            } finally {
                try (PreparedStatement ps = con.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    ps.setString(1, DISTRIBUTED_RANK_LOCK);
                    ps.executeQuery();
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardEngine.class, ex);
            return false;
        }
    }

    public static void processDailyCycle(LocalDate rewardDate) {
        SuperRankRewardDAO.RewardCycle cycle = SuperRankRewardDAO.getCycleByDate(rewardDate);

        if (cycle == null) {
            long cycleId = SuperRankRewardDAO.createCycleTaking(rewardDate);
            if (cycleId <= 0) return;
            cycle = SuperRankRewardDAO.getCycleById(cycleId);
        }

        if (cycle == null) return;

        switch (cycle.status()) {
            case "SNAPSHOT_TAKING":
                long nextAttemptTime = cycle.lastAttemptAt() != null
                        ? cycle.lastAttemptAt().getTime() + (cycle.attempts() * 5000L)
                        : 0L;
                if (System.currentTimeMillis() < nextAttemptTime) {
                    return;
                }

                Phase2Result result = executePhase2Snapshot(cycle.id(), rewardDate);
                switch (result) {
                    case SUCCESS:
                    case ALREADY_COMPLETED:
                        dispatchDeliveries(SuperRankRewardDAO.getCycleById(cycle.id()));
                        break;
                    case FAILED_ALREADY:
                        break;
                    case LOCK_TIMEOUT:
                    case RETRYABLE_DATABASE_ERROR:
                        if (cycle.attempts() < 5) {
                            SuperRankRewardDAO.incrementCycleAttempts(cycle.id());
                        } else {
                            markCycleFailed(cycle.id(), "Vượt quá số lần thử lại tối đa (5 lần) do lock timeout/lỗi DB");
                        }
                        break;
                    case VALIDATION_FAILED:
                        markCycleFailed(cycle.id(), "Validation dữ liệu BXH thất bại (Trùng rank, trùng player_id hoặc thiếu Top 1)");
                        break;
                    case CORRUPTED_CYCLE:
                        markCycleFailed(cycle.id(), "Phát hiện corruption: Cycle SNAPSHOT_TAKING nhưng đã tồn tại ledger");
                        break;
                }
                break;

            case "SNAPSHOT_TAKEN":
            case "DELIVERING":
                dispatchDeliveries(cycle);
                break;

            case "CLOSED_SNAPSHOT":
                retryPendingMailboxes();
                break;

            case "FAILED":
                break;
        }
    }

    public static Phase2Result executePhase2Snapshot(long cycleId, LocalDate rewardDate) {
        try (Connection con = LocalManager.getConnection()) {
            String lockSql = "SELECT GET_LOCK(?, 15)";
            try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                ps.setString(1, DISTRIBUTED_RANK_LOCK);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1) {
                        return Phase2Result.LOCK_TIMEOUT;
                    }
                }
            }

            try {
                con.setAutoCommit(false);
                try {
                    String checkCycleSql = "SELECT status, snapshot_count FROM super_rank_reward_cycle WHERE id = ? FOR UPDATE";
                    String currentStatus = "";
                    try (PreparedStatement ps = con.prepareStatement(checkCycleSql)) {
                        ps.setLong(1, cycleId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                con.rollback();
                                return Phase2Result.CORRUPTED_CYCLE;
                            }
                            currentStatus = rs.getString("status");
                        }
                    }

                    if ("FAILED".equals(currentStatus)) {
                        con.rollback();
                        return Phase2Result.FAILED_ALREADY;
                    }
                    if ("SNAPSHOT_TAKEN".equals(currentStatus) || "DELIVERING".equals(currentStatus) || "CLOSED_SNAPSHOT".equals(currentStatus)) {
                        con.rollback();
                        return Phase2Result.ALREADY_COMPLETED;
                    }
                    if (!"SNAPSHOT_TAKING".equals(currentStatus)) {
                        con.rollback();
                        return Phase2Result.CORRUPTED_CYCLE;
                    }

                    String countLedgerSql = "SELECT COUNT(*) FROM super_rank_reward_ledger WHERE cycle_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(countLedgerSql)) {
                        ps.setLong(1, cycleId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                con.rollback();
                                return Phase2Result.CORRUPTED_CYCLE;
                            }
                        }
                    }

                    String selectSql = "SELECT sr.player_id, p.account_id, sr.rank, sr.name "
                            + "FROM super_rank sr INNER JOIN player p ON sr.player_id = p.id "
                            + "WHERE sr.rank > 0 AND sr.rank <= 1000 ORDER BY sr.rank ASC LOCK IN SHARE MODE";

                    List<RankEntry> entries = new ArrayList<>();
                    Set<Integer> playerIds = new HashSet<>();
                    Set<Integer> ranks = new HashSet<>();
                    boolean hasTop1 = false;
                    long sumRank = 0;
                    long sumPlayerId = 0;

                    try (PreparedStatement ps = con.prepareStatement(selectSql);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int pId = rs.getInt("player_id");
                            int accId = rs.getInt("account_id");
                            int rank = rs.getInt("rank");
                            String name = rs.getString("name");

                            if (!playerIds.add(pId) || !ranks.add(rank)) {
                                con.rollback();
                                return Phase2Result.VALIDATION_FAILED;
                            }
                            if (rank == 1) hasTop1 = true;
                            sumRank += rank;
                            sumPlayerId += pId;
                            entries.add(new RankEntry(pId, accId, rank, name));
                        }
                    }

                    if (entries.isEmpty() || !hasTop1) {
                        con.rollback();
                        return Phase2Result.VALIDATION_FAILED;
                    }

                    int actualCount = entries.size();
                    String checksum = computeChecksum(actualCount + ":" + sumRank + ":" + sumPlayerId);

                    String insertLedgerSql = "INSERT INTO super_rank_reward_ledger "
                            + "(cycle_id, reward_date, player_id, account_id, rank_position, ruby_reward, ruby_status, badge_status, mailbox_status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)";
                    try (PreparedStatement ps = con.prepareStatement(insertLedgerSql)) {
                        for (RankEntry e : entries) {
                            ps.setLong(1, cycleId);
                            ps.setDate(2, java.sql.Date.valueOf(rewardDate));
                            ps.setInt(3, e.playerId);
                            ps.setInt(4, e.accountId);
                            ps.setInt(5, e.rank);
                            ps.setInt(6, calculateRuby(e.rank));
                            ps.setString(7, (e.rank == 1) ? "PENDING" : "NONE");
                            ps.setString(8, (e.rank == 1) ? "PENDING" : "NONE");
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    String updateCycleSql = "UPDATE super_rank_reward_cycle SET status = 'SNAPSHOT_TAKEN', "
                            + "snapshot_count = ?, snapshot_checksum = ?, total_winners = ? "
                            + "WHERE id = ? AND status = 'SNAPSHOT_TAKING'";
                    try (PreparedStatement ps = con.prepareStatement(updateCycleSql)) {
                        ps.setInt(1, actualCount);
                        ps.setString(2, checksum);
                        ps.setInt(3, actualCount);
                        ps.setLong(4, cycleId);
                        int affectedRows = ps.executeUpdate();
                        if (affectedRows != 1) {
                            con.rollback();
                            return Phase2Result.ALREADY_COMPLETED;
                        }
                    }

                    con.commit();
                    Logger.success("[SUPER RANK 20H] Snapshot thành công ngày " + rewardDate + " với " + actualCount + " người chơi.");
                    return Phase2Result.SUCCESS;

                } catch (Exception ex) {
                    con.rollback();
                    Logger.logException(SuperRankRewardEngine.class, ex);
                    return Phase2Result.RETRYABLE_DATABASE_ERROR;
                }
            } finally {
                try (PreparedStatement ps = con.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    ps.setString(1, DISTRIBUTED_RANK_LOCK);
                    ps.executeQuery();
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardEngine.class, ex);
            return Phase2Result.RETRYABLE_DATABASE_ERROR;
        }
    }

    public static void markCycleFailed(long cycleId, String reason) {
        try (Connection con = LocalManager.getConnection()) {
            String sql = "UPDATE super_rank_reward_cycle SET status = 'FAILED', error_message = ?, updated_at = NOW() "
                    + "WHERE id = ? AND status = 'SNAPSHOT_TAKING'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, reason);
                ps.setLong(2, cycleId);
                int affected = ps.executeUpdate();
                if (affected == 1) {
                    Logger.error("[ALERT ADMIN] Cycle " + cycleId + " đã chuyển FAILED: " + reason);
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardEngine.class, ex);
        }
    }

    public static void dispatchDeliveries(SuperRankRewardDAO.RewardCycle cycle) {
        if (cycle == null) return;

        // 1. Dispatch Mailbox Top 1
        retryPendingMailboxes();

        // 2. Dispatch Ruby & Badge cho người chơi đang Online
        List<SuperRankRewardDAO.LedgerEntry> pendingList = getPendingLedgersByCycleId(cycle.id());
        for (SuperRankRewardDAO.LedgerEntry entry : pendingList) {
            Player pl = Client.gI().getPlayer(entry.playerId());
            if (pl != null && pl.getSession() != null) {
                SuperRankRewardDAO.claimRewardAtomic(pl, entry.id());
            }
        }

        // 3. Chuyển cycle sang CLOSED_SNAPSHOT
        try (Connection con = LocalManager.getConnection()) {
            String sql = "UPDATE super_rank_reward_cycle SET status = 'CLOSED_SNAPSHOT', updated_at = NOW() "
                    + "WHERE id = ? AND status IN ('SNAPSHOT_TAKEN', 'DELIVERING')";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, cycle.id());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardEngine.class, ex);
        }
    }

    public static void retryPendingMailboxes() {
        List<SuperRankRewardDAO.LedgerEntry> pendingMails = SuperRankRewardDAO.getPendingMailboxLedgers();
        for (SuperRankRewardDAO.LedgerEntry e : pendingMails) {
            String idempotencyKey = "super_rank_top1_" + e.rewardDate() + "_" + e.playerId();
            String rewardsJson = "[{\"id\":1655,\"quantity\":1,\"options\":[{\"id\":30,\"param\":1}]}]";

            MailboxService.SendMailResult res = MailboxService.sendSystemMailWithIdempotencyKey(
                    e.accountId(), e.playerId(), "Thưởng Top 1 Siêu Hạng",
                    "Chúc mừng bạn đạt Top 1 Giải Siêu Hạng ngày " + e.rewardDate() + "! Phần thưởng: 1 Capsule Kích Hoạt (Khóa).",
                    "Trọng Tài", rewardsJson, idempotencyKey);

            if (res.created() || res.alreadyExisted()) {
                SuperRankRewardDAO.updateMailboxStatus(e.id(), res.mailboxId(), "SENT", null);
                Logger.success("[TOP 1 MAILBOX] Đã gửi Capsule cho Top 1 player_id=" + e.playerId() + " (Mailbox ID: " + res.mailboxId() + ")");
            } else {
                SuperRankRewardDAO.updateMailboxStatus(e.id(), 0, "FAILED", res.errorMessage());
                Logger.error("[TOP 1 MAILBOX ERROR] Gửi thư thất bại cho player_id=" + e.playerId() + ": " + res.errorMessage());
            }
        }
    }

    public static List<SuperRankRewardDAO.LedgerEntry> getPendingLedgersByCycleId(long cycleId) {
        List<SuperRankRewardDAO.LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM super_rank_reward_ledger WHERE cycle_id = ? AND ruby_status = 'PENDING'";
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cycleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long mId = rs.getObject("mailbox_id") != null ? rs.getLong("mailbox_id") : null;
                    list.add(new SuperRankRewardDAO.LedgerEntry(
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
                    ));
                }
            }
        } catch (SQLException ex) {
            Logger.logException(SuperRankRewardEngine.class, ex);
        }
        return list;
    }

    public static int calculateRuby(int rank) {
        if (rank == 1) return 2000;
        if (rank >= 2 && rank <= 5) return 1000;
        if (rank >= 6 && rank <= 10) return 500;
        if (rank >= 11 && rank <= 50) return 200;
        if (rank >= 51 && rank <= 100) return 100;
        if (rank >= 101 && rank <= 1000) return 20;
        return 0;
    }

    public static String computeChecksum(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
