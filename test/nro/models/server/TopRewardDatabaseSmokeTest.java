package nro.models.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.data.LocalManager;

public final class TopRewardDatabaseSmokeTest {

    private TopRewardDatabaseSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String[] keys = {
            "top_boss", "summer", "top_power", "top_task",
            "childrens_day", "sugarcane", "fruit_ice_cream", "top_up"
        };
        try (Connection con = LocalManager.getConnection()) {
            for (String key : keys) {
                try (PreparedStatement ps = con.prepareStatement(TopRewardService.rankingSql(key));
                        ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        rs.getInt("account_id");
                        rs.getInt("player_id");
                        rs.getString("player_name");
                        rs.getBigDecimal("score");
                    }
                }
            }
            for (String table : new String[]{
                "player_mailbox", "top_reward_config", "top_reward_command", "top_reward_winner"
            }) {
                try (PreparedStatement ps = con.prepareStatement("SHOW TABLES LIKE ?")) {
                    ps.setString(1, table);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new AssertionError("Missing table " + table);
                        }
                    }
                }
            }
            assertColumn(con, "top_reward_command", "period_type");
            assertColumn(con, "top_reward_command", "ranking_date");
        }
        System.out.println("TOP_REWARD_DATABASE_SMOKE_OK rankings=8 tables=4 period_columns=2");
        LocalManager.close();
    }

    private static void assertColumn(Connection con, String table, String column) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new AssertionError("Missing column " + table + "." + column);
                }
            }
        }
    }
}
