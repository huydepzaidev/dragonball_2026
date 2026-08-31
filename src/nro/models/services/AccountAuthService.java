package nro.models.services;

import nro.models.database.AccountDAO;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service handling account authentication, password verification, and automatic hash upgrades.
 */
public final class AccountAuthService {

    public record AuthResult(
            boolean authenticated,
            boolean rehashed,
            int accountId,
            boolean isAdmin,
            boolean isBanned,
            int active,
            String errorMessage
    ) {
        public static AuthResult success(int accountId, boolean isAdmin, boolean isBanned, int active, boolean rehashed) {
            return new AuthResult(true, rehashed, accountId, isAdmin, isBanned, active, null);
        }

        public static AuthResult fail(String errorMessage) {
            return new AuthResult(false, false, -1, false, false, 0, errorMessage);
        }
    }

    private AccountAuthService() {
    }

    /**
     * Authenticates an account using global database connection pool and auto-rehashes legacy passwords.
     */
    public static AuthResult authenticateAndRehash(String username, String rawPassword) {
        if (username == null || rawPassword == null || username.isBlank()) {
            return AuthResult.fail("Thông tin tài khoản hoặc mật khẩu không chính xác");
        }
        return AccountDAO.authenticateAndRehash(username.trim(), rawPassword);
    }

    /**
     * Authenticates an account using an explicit database connection and auto-rehashes legacy passwords.
     */
    public static AuthResult authenticateAndRehash(Connection con, String username, String rawPassword) throws SQLException {
        if (username == null || rawPassword == null || username.isBlank()) {
            return AuthResult.fail("Thông tin tài khoản hoặc mật khẩu không chính xác");
        }
        return AccountDAO.authenticateAndRehash(con, username.trim(), rawPassword);
    }
}
