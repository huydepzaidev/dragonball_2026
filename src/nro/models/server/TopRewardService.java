package nro.models.server;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.data.LocalManager;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Consumes Top reward commands created by the web admin and snapshots winners into mailboxes. */
public final class TopRewardService {

    static final int MAX_REWARD_RANK = 10;
    private static TopRewardService instance;
    private long retryAfter;

    private TopRewardService() {
    }

    public static TopRewardService gI() {
        if (instance == null) {
            instance = new TopRewardService();
        }
        return instance;
    }

    public void processCommands() {
        if (System.currentTimeMillis() < retryAfter) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id FROM top_reward_command WHERE status='PENDING' ORDER BY id LIMIT 5");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong(1));
            }
        } catch (Exception e) {
            retryAfter = System.currentTimeMillis() + 30_000L;
            return;
        }
        for (long id : ids) {
            processCommand(id);
        }
    }

    private void processCommand(long commandId) {
        if (!claimCommand(commandId)) {
            return;
        }
        try (Connection con = LocalManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                CommandData command = loadCommand(con, commandId);
                validateCommandPeriod(command);
                Map<Integer, RewardConfig> configs = loadConfigs(con, command);
                if (configs.size() != MAX_REWARD_RANK) {
                    throw new IllegalStateException("Thiếu cấu hình quà Top 1 đến Top 10.");
                }
                List<Winner> winners = loadWinners(con, command);
                if (winners.isEmpty()) {
                    throw new IllegalStateException("Bảng xếp hạng chưa có người chơi hợp lệ.");
                }

                int sent = 0;
                for (int i = 0; i < winners.size() && i < MAX_REWARD_RANK; i++) {
                    int rank = i + 1;
                    Winner winner = winners.get(i);
                    RewardConfig config = configs.get(rank);
                    long mailboxId = insertMailbox(con, commandId, rank, winner, config, command.requestedBy);
                    insertWinner(con, commandId, rank, winner, mailboxId);
                    sent++;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE top_reward_command SET status='DONE',finished_at=NOW(),result_message=? WHERE id=?")) {
                    ps.setString(1, "Đã khóa " + sent + " người thắng và tạo hòm thư.");
                    ps.setLong(2, commandId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            Logger.logException(TopRewardService.class, e);
            markFailed(commandId, compact(e.getMessage()));
        }
    }

    private boolean claimCommand(long commandId) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE top_reward_command SET status='PROCESSING',started_at=NOW(),result_message=NULL "
                        + "WHERE id=? AND status='PENDING'")) {
            ps.setLong(1, commandId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            Logger.logException(TopRewardService.class, e);
            return false;
        }
    }

    private CommandData loadCommand(Connection con, long commandId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT ranking_key,period_type,ranking_date,config_snapshot_json,requested_by "
                + "FROM top_reward_command WHERE id=? AND status='PROCESSING'")) {
            ps.setLong(1, commandId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Lệnh chốt không còn ở trạng thái xử lý.");
                }
                return new CommandData(
                        rs.getString("ranking_key"),
                        rs.getString("period_type"),
                        rs.getDate("ranking_date"),
                        rs.getString("config_snapshot_json"),
                        (Integer) rs.getObject("requested_by"));
            }
        }
    }

    private Map<Integer, RewardConfig> loadConfigs(Connection con, CommandData command) throws Exception {
        if (command.configSnapshotJson != null && !command.configSnapshotJson.isBlank()) {
            return parseConfigSnapshot(command.configSnapshotJson);
        }
        Map<Integer, RewardConfig> configs = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT rank_position,title,message,sender_name,rewards_json FROM top_reward_config "
                + "WHERE ranking_key=? AND rank_position BETWEEN 1 AND 10 ORDER BY rank_position")) {
            ps.setString(1, command.rankingKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rank = rs.getInt("rank_position");
                    configs.put(rank, new RewardConfig(
                            rs.getString("title"), rs.getString("message"),
                            rs.getString("sender_name"), rs.getString("rewards_json")));
                }
            }
        }
        return configs;
    }

    private Map<Integer, RewardConfig> parseConfigSnapshot(String snapshotJson) {
        Object parsed = JSONValue.parse(snapshotJson);
        if (!(parsed instanceof JSONArray rows)) {
            throw new IllegalStateException("Snapshot cấu hình quà không hợp lệ.");
        }
        Map<Integer, RewardConfig> configs = new HashMap<>();
        for (Object value : rows) {
            if (!(value instanceof JSONObject row)) {
                throw new IllegalStateException("Dòng snapshot cấu hình quà không hợp lệ.");
            }
            int rank;
            try {
                rank = Integer.parseInt(String.valueOf(row.get("rank_position")));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Hạng trong snapshot cấu hình quà không hợp lệ.");
            }
            if (rank < 1 || rank > MAX_REWARD_RANK || configs.containsKey(rank)) {
                throw new IllegalStateException("Snapshot cấu hình quà bị trùng hoặc sai hạng.");
            }
            String title = snapshotString(row, "title");
            String message = snapshotString(row, "message");
            String senderName = snapshotString(row, "sender_name");
            String rewardsJson = snapshotString(row, "rewards_json");
            if (!(JSONValue.parse(rewardsJson) instanceof JSONArray)) {
                throw new IllegalStateException("Phần thưởng trong snapshot không hợp lệ.");
            }
            configs.put(rank, new RewardConfig(title, message, senderName, rewardsJson));
        }
        return configs;
    }

    private String snapshotString(JSONObject row, String key) {
        Object value = row.get(key);
        if (value == null) {
            throw new IllegalStateException("Snapshot cấu hình quà thiếu trường " + key + ".");
        }
        return String.valueOf(value);
    }

    private List<Winner> loadWinners(Connection con, CommandData command) throws Exception {
        boolean fixedWeeklyPeriod = isWeeklyRanking(command.rankingKey);
        String sql = rankingSql(command.rankingKey, fixedWeeklyPeriod);
        List<Winner> winners = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (fixedWeeklyPeriod) {
                ps.setDate(1, command.rankingDate);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && winners.size() < MAX_REWARD_RANK) {
                    winners.add(new Winner(
                            rs.getInt("account_id"), rs.getInt("player_id"),
                            rs.getString("player_name"), rs.getBigDecimal("score")));
                }
            }
        }
        return winners;
    }

    static String rankingSql(String rankingKey) {
        return rankingSql(rankingKey, false);
    }

    static String rankingSql(String rankingKey, boolean fixedWeeklyPeriod) {
        String basePlayer = " INNER JOIN account a ON a.id=p.account_id "
                + "WHERE a.ban=0 AND a.is_admin=0 ";
        String weeklyDate = fixedWeeklyPeriod
                ? "d.ranking_date=? "
                : "d.ranking_date=DATE_SUB(CURDATE(),INTERVAL WEEKDAY(CURDATE()) DAY) ";
        return switch (rankingKey) {
            case "top_boss" -> "SELECT p.account_id,p.id player_id,p.name player_name,d.score "
                    + "FROM daily_ranking_score d INNER JOIN player p ON p.id=d.player_id "
                    + basePlayer
                    + "AND " + weeklyDate
                    + "AND d.ranking_type='BOSS' AND d.score>0 ORDER BY d.score DESC,p.id ASC LIMIT 10";
            case "summer" -> "SELECT p.account_id,p.id player_id,p.name player_name,d.score "
                    + "FROM daily_ranking_score d INNER JOIN player p ON p.id=d.player_id "
                    + basePlayer
                    + "AND " + weeklyDate
                    + "AND d.ranking_type='SUMMER_EVENT' AND d.score>0 ORDER BY d.score DESC,p.id ASC LIMIT 10";
            case "top_power" -> "SELECT p.account_id,p.id player_id,p.name player_name,"
                    + "COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_point,'$[1]')) AS UNSIGNED),0) score "
                    + "FROM player p" + basePlayer + "ORDER BY score DESC,p.id ASC LIMIT 10";
            case "top_task" -> "SELECT p.account_id,p.id player_id,p.name player_name,"
                    + "COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[0]')) AS UNSIGNED),0) score "
                    + "FROM player p" + basePlayer
                    + "ORDER BY score DESC,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[1]')) AS UNSIGNED),0) DESC,"
                    + "COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_task,'$[2]')) AS UNSIGNED),0) DESC,p.id ASC LIMIT 10";
            case "childrens_day" -> playerColumnSql("point_sukien");
            case "sugarcane" -> playerColumnSql("point_sukien1");
            case "fruit_ice_cream" -> playerColumnSql("point_sukien2");
            case "top_up" -> "SELECT p.account_id,p.id player_id,p.name player_name,a.tongnap score "
                    + "FROM player p INNER JOIN account a ON a.id=p.account_id "
                    + "WHERE a.ban=0 AND a.is_admin=0 AND a.tongnap>0 "
                    + "ORDER BY a.tongnap DESC,p.id ASC LIMIT 10";
            default -> throw new IllegalArgumentException("Loại bảng xếp hạng không được hỗ trợ: " + rankingKey);
        };
    }

    private static String playerColumnSql(String column) {
        return "SELECT p.account_id,p.id player_id,p.name player_name,p." + column + " score "
                + "FROM player p INNER JOIN account a ON a.id=p.account_id "
                + "WHERE a.ban=0 AND a.is_admin=0 AND p." + column + ">0 "
                + "ORDER BY p." + column + " DESC,p.id ASC LIMIT 10";
    }

    private static boolean isWeeklyRanking(String rankingKey) {
        return "top_boss".equals(rankingKey) || "summer".equals(rankingKey);
    }

    private static void validateCommandPeriod(CommandData command) {
        if (isWeeklyRanking(command.rankingKey)) {
            if (!"WEEKLY".equals(command.periodType) || command.rankingDate == null) {
                throw new IllegalStateException("Top tuần thiếu ngày bắt đầu kỳ.");
            }
            return;
        }
        if ("top_power".equals(command.rankingKey) || "top_task".equals(command.rankingKey)) {
            if (!"LIFETIME".equals(command.periodType) || command.rankingDate != null) {
                throw new IllegalStateException("Top một lần có kỳ trao thưởng không hợp lệ.");
            }
            return;
        }
        if (!"MANUAL".equals(command.periodType) || command.rankingDate != null) {
            throw new IllegalStateException("Top thủ công có kỳ trao thưởng không hợp lệ.");
        }
    }

    private long insertMailbox(Connection con, long commandId, int rank, Winner winner,
            RewardConfig config, Integer createdBy) throws Exception {
        String sql = "INSERT INTO player_mailbox "
                + "(account_id,player_id,title,message,sender_name,rank_position,rewards_json,source_command_id,created_by) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, winner.accountId);
            ps.setInt(2, winner.playerId);
            ps.setString(3, config.title);
            ps.setString(4, config.message);
            ps.setString(5, config.senderName);
            ps.setInt(6, rank);
            ps.setString(7, config.rewardsJson);
            ps.setLong(8, commandId);
            if (createdBy == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, createdBy);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Không lấy được ID hòm thư.");
                }
                return keys.getLong(1);
            }
        }
    }

    private void insertWinner(Connection con, long commandId, int rank, Winner winner,
            long mailboxId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO top_reward_winner "
                + "(command_id,rank_position,account_id,player_id,player_name,score,mailbox_id) "
                + "VALUES (?,?,?,?,?,?,?)")) {
            ps.setLong(1, commandId);
            ps.setInt(2, rank);
            ps.setInt(3, winner.accountId);
            ps.setInt(4, winner.playerId);
            ps.setString(5, winner.playerName);
            ps.setBigDecimal(6, winner.score == null ? BigDecimal.ZERO : winner.score);
            ps.setLong(7, mailboxId);
            ps.executeUpdate();
        }
    }

    private void markFailed(long commandId, String message) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE top_reward_command SET status='FAILED',finished_at=NOW(),result_message=? "
                        + "WHERE id=? AND status='PROCESSING'")) {
            ps.setString(1, message);
            ps.setLong(2, commandId);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.logException(TopRewardService.class, e);
        }
    }

    private static String compact(String message) {
        if (message == null || message.isBlank()) {
            return "Lỗi không xác định";
        }
        String value = message.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() <= 480 ? value : value.substring(0, 480);
    }

    private static final class CommandData {
        private final String rankingKey;
        private final String periodType;
        private final Date rankingDate;
        private final String configSnapshotJson;
        private final Integer requestedBy;

        private CommandData(String rankingKey, String periodType, Date rankingDate,
                String configSnapshotJson, Integer requestedBy) {
            this.rankingKey = rankingKey;
            this.periodType = periodType;
            this.rankingDate = rankingDate;
            this.configSnapshotJson = configSnapshotJson;
            this.requestedBy = requestedBy;
        }
    }

    private static final class RewardConfig {
        private final String title;
        private final String message;
        private final String senderName;
        private final String rewardsJson;

        private RewardConfig(String title, String message, String senderName, String rewardsJson) {
            this.title = title;
            this.message = message;
            this.senderName = senderName;
            this.rewardsJson = rewardsJson;
        }
    }

    private static final class Winner {
        private final int accountId;
        private final int playerId;
        private final String playerName;
        private final BigDecimal score;

        private Winner(int accountId, int playerId, String playerName, BigDecimal score) {
            this.accountId = accountId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
        }
    }
}
