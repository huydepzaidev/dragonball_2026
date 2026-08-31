package nro.models.database;

import nro.models.data.LocalManager;
import nro.models.services.AccountAuthService.AuthResult;
import nro.models.utils.Logger;
import nro.models.utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Data Access Object for account creation, queries, authentication, and safe password updates without query logging.
 */
public final class AccountDAO {

    private AccountDAO() {
    }

    public enum CreateAccountStatus {
        SUCCESS,
        DUPLICATE_USERNAME,
        DATABASE_ERROR
    }

    /**
     * Checks if an account exists with the given username using the global connection pool.
     */
    public static boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        try (Connection con = LocalManager.getConnection()) {
            return existsByUsername(con, username);
        } catch (SQLException e) {
            Logger.logException(AccountDAO.class, e);
            return false;
        }
    }

    /**
     * Checks if an account exists with the given username using a specified connection.
     */
    public static boolean existsByUsername(Connection con, String username) throws SQLException {
        if (con == null || username == null || username.isBlank()) {
            return false;
        }
        String sql = "SELECT id FROM account WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Creates a new account in the database using the global connection pool.
     * Fills all NOT NULL columns explicitly without relying on unspecified database defaults.
     */
    public static CreateAccountStatus createAccount(String username, String hashedPassword, String ipAddress) {
        if (username == null || hashedPassword == null) {
            return CreateAccountStatus.DATABASE_ERROR;
        }
        try (Connection con = LocalManager.getConnection()) {
            return createAccount(con, username, hashedPassword, ipAddress);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"))) {
                return CreateAccountStatus.DUPLICATE_USERNAME;
            }
            Logger.logException(AccountDAO.class, e);
            return CreateAccountStatus.DATABASE_ERROR;
        }
    }

    /**
     * Creates a new account in the database using a specified connection.
     */
    public static CreateAccountStatus createAccount(Connection con, String username, String hashedPassword, String ipAddress) throws SQLException {
        if (con == null || username == null || hashedPassword == null) {
            return CreateAccountStatus.DATABASE_ERROR;
        }

        String trimmedUser = username.trim();
        String safeIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress.trim() : "127.0.0.1";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String sql = "INSERT INTO account ("
                + "username, password, email, token, xsrf_token, newpass, "
                + "create_time, update_time, ban, is_admin, last_time_login, last_time_logout, "
                + "ip_address, active, thoi_vang, server_login, bd_player, is_gift_box, "
                + "gift_time, vnd, tongnap, luotquay, vang, event_point, vip, "
                + "tichdiem, point_post, last_post, baiviet, xacminh, admin"
                + ") VALUES ("
                + "?, ?, '', '', '', '', "
                + "?, ?, 0, 0, ?, ?, "
                + "?, 1, 0, -1, 1, 0, "
                + "'0', 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0"
                + ")";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trimmedUser);
            ps.setString(2, hashedPassword);
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, now);
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            ps.setString(7, safeIp);

            int affected = ps.executeUpdate();
            if (affected == 1) {
                return CreateAccountStatus.SUCCESS;
            }
            return CreateAccountStatus.DATABASE_ERROR;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"))) {
                return CreateAccountStatus.DUPLICATE_USERNAME;
            }
            throw e;
        }
    }

    /**
     * Safely updates the stored password for an account by ID without exposing the hash in query logs.
     */
    public static boolean updatePassword(int accountId, String newHashedPassword) {
        if (accountId <= 0 || newHashedPassword == null || newHashedPassword.isBlank()) {
            return false;
        }
        try (Connection con = LocalManager.getConnection()) {
            return updatePassword(con, accountId, newHashedPassword);
        } catch (SQLException e) {
            Logger.logException(AccountDAO.class, e);
            return false;
        }
    }

    /**
     * Safely updates the stored password for an account by ID using a specified connection.
     */
    public static boolean updatePassword(Connection con, int accountId, String newHashedPassword) throws SQLException {
        if (con == null || accountId <= 0 || newHashedPassword == null || newHashedPassword.isBlank()) {
            return false;
        }
        String sql = "UPDATE account SET password = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Safely updates the stored password for an account by ID and username without exposing the hash in query logs.
     */
    public static boolean updatePassword(int accountId, String username, String newHashedPassword) {
        if (accountId <= 0 || username == null || newHashedPassword == null || newHashedPassword.isBlank()) {
            return false;
        }
        try (Connection con = LocalManager.getConnection()) {
            return updatePassword(con, accountId, username, newHashedPassword);
        } catch (SQLException e) {
            Logger.logException(AccountDAO.class, e);
            return false;
        }
    }

    /**
     * Safely updates the stored password for an account by ID and username using a specified connection.
     */
    public static boolean updatePassword(Connection con, int accountId, String username, String newHashedPassword) throws SQLException {
        if (con == null || accountId <= 0 || username == null || newHashedPassword == null || newHashedPassword.isBlank()) {
            return false;
        }
        String sql = "UPDATE account SET password = ? WHERE id = ? AND username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setInt(2, accountId);
            ps.setString(3, username.trim());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Authenticates an account and automatically rehashes legacy or weak passwords using the global connection pool.
     */
    public static AuthResult authenticateAndRehash(String username, String rawPassword) {
        try (Connection con = LocalManager.getConnection()) {
            return authenticateAndRehash(con, username, rawPassword);
        } catch (SQLException e) {
            Logger.logException(AccountDAO.class, e);
            return AuthResult.fail("Có lỗi xảy ra khi xác thực tài khoản");
        }
    }

    /**
     * Authenticates an account and automatically rehashes legacy or weak passwords using a specified connection.
     */
    public static AuthResult authenticateAndRehash(Connection con, String username, String rawPassword) throws SQLException {
        if (con == null || username == null || rawPassword == null) {
            return AuthResult.fail("Thông tin tài khoản hoặc mật khẩu không chính xác");
        }

        String sql = "SELECT id, password, is_admin, ban, active FROM account WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return AuthResult.fail("Thông tin tài khoản hoặc mật khẩu không chính xác");
                }

                int accountId = rs.getInt("id");
                String dbPassword = rs.getString("password");
                boolean isAdmin = rs.getInt("is_admin") > 0;
                boolean isBanned = rs.getInt("ban") > 0;
                int active = rs.getInt("active");

                if (!PasswordHasher.checkPassword(rawPassword, dbPassword)) {
                    return AuthResult.fail("Thông tin tài khoản hoặc mật khẩu không chính xác");
                }

                boolean rehashed = false;
                if (PasswordHasher.needsRehash(dbPassword)) {
                    String newHash = PasswordHasher.hashPassword(rawPassword);
                    try {
                        rehashed = updatePassword(con, accountId, newHash);
                    } catch (Exception e) {
                        rehashed = false;
                        Logger.logException(AccountDAO.class, e);
                    }
                }

                return AuthResult.success(accountId, isAdmin, isBanned, active, rehashed);
            }
        }
    }
}
