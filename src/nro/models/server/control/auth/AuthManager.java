package nro.models.server.control.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import nro.models.data.LocalManager;
import nro.models.server.control.ControlConfig;
import nro.models.utils.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public final class AuthManager {

    private static final AuthManager INSTANCE = new AuthManager();

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockoutUntil = new ConcurrentHashMap<>();
    private final Map<String, TokenData> activeTokens = new ConcurrentHashMap<>();

    private AuthManager() {}

    public static AuthManager gI() {
        return INSTANCE;
    }

    public static class TokenData {
        public final int userId;
        public final String username;
        public final UserRole role;
        public final String tokenType; // ACCESS or REFRESH
        public final long expiresAt;

        public TokenData(int userId, String username, UserRole role, String tokenType, long expiresAt) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.tokenType = tokenType;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final String accessToken;
        public final String refreshToken;
        public final AdminUser user;
        public final long expiresIn;

        public AuthResult(boolean success, String message, String accessToken, String refreshToken, AdminUser user, long expiresIn) {
            this.success = success;
            this.message = message;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.expiresIn = expiresIn;
        }

        public static AuthResult fail(String message) {
            return new AuthResult(false, message, null, null, null, 0);
        }

        public static AuthResult ok(String accessToken, String refreshToken, AdminUser user, long expiresIn) {
            return new AuthResult(true, "Đăng nhập thành công", accessToken, refreshToken, user, expiresIn);
        }
    }

    public boolean isRateLimited(String ip) {
        Long lockout = lockoutUntil.get(ip);
        if (lockout != null) {
            if (System.currentTimeMillis() < lockout) {
                return true;
            } else {
                lockoutUntil.remove(ip);
                failedAttempts.remove(ip);
            }
        }
        return false;
    }

    public void recordFailedAttempt(String ip) {
        int count = failedAttempts.compute(ip, (k, v) -> v == null ? 1 : v + 1);
        if (count >= ControlConfig.MAX_FAILED_LOGINS) {
            lockoutUntil.put(ip, System.currentTimeMillis() + ControlConfig.LOCKOUT_DURATION_MS);
            Logger.log(Logger.RED, "IP " + ip + " bị khóa tạm thời do nhập sai mật khẩu quá " + count + " lần.\n");
        }
    }

    public void clearFailedAttempts(String ip) {
        failedAttempts.remove(ip);
        lockoutUntil.remove(ip);
    }

    public AuthResult login(String username, String password, String ip) {
        if (isRateLimited(ip)) {
            long remainingSec = (lockoutUntil.get(ip) - System.currentTimeMillis()) / 1000;
            return AuthResult.fail("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + remainingSec + " giây.");
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return AuthResult.fail("Tên đăng nhập và mật khẩu không được để trống.");
        }

        String trimmedUser = username.trim();
        String sql = "SELECT id, username, password, is_admin, ban FROM account WHERE username = ? LIMIT 1";

        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trimmedUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    recordFailedAttempt(ip);
                    return AuthResult.fail("Tài khoản hoặc mật khẩu không chính xác.");
                }

                int ban = rs.getInt("ban");
                if (ban == 1) {
                    return AuthResult.fail("Tài khoản này hiện đang bị khóa.");
                }

                int isAdmin = rs.getInt("is_admin");
                if (isAdmin <= 0) {
                    recordFailedAttempt(ip);
                    return AuthResult.fail("Tài khoản không có quyền truy cập Control Panel.");
                }

                String dbPassword = rs.getString("password");
                if (!verifyPassword(password, dbPassword)) {
                    recordFailedAttempt(ip);
                    return AuthResult.fail("Tài khoản hoặc mật khẩu không chính xác.");
                }

                clearFailedAttempts(ip);

                int userId = rs.getInt("id");
                UserRole role = isAdmin >= 1 ? UserRole.SUPER_ADMIN : UserRole.VIEWER;
                AdminUser adminUser = new AdminUser(userId, trimmedUser, role);

                long accessExp = System.currentTimeMillis() + ControlConfig.ACCESS_TOKEN_EXPIRATION_MS;
                long refreshExp = System.currentTimeMillis() + ControlConfig.REFRESH_TOKEN_EXPIRATION_MS;

                String accessToken = createToken(userId, trimmedUser, role, "ACCESS", accessExp);
                String refreshToken = createToken(userId, trimmedUser, role, "REFRESH", refreshExp);

                activeTokens.put(accessToken, new TokenData(userId, trimmedUser, role, "ACCESS", accessExp));
                activeTokens.put(refreshToken, new TokenData(userId, trimmedUser, role, "REFRESH", refreshExp));

                return AuthResult.ok(accessToken, refreshToken, adminUser, ControlConfig.ACCESS_TOKEN_EXPIRATION_MS / 1000);
            }
        } catch (SQLException e) {
            Logger.logException(AuthManager.class, e, "Lỗi kiểm tra đăng nhập DB");
            return AuthResult.fail("Lỗi cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public AuthResult refreshToken(String refreshTokenStr) {
        AdminUser user = validateToken(refreshTokenStr, "REFRESH");
        if (user == null) {
            return AuthResult.fail("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        long accessExp = System.currentTimeMillis() + ControlConfig.ACCESS_TOKEN_EXPIRATION_MS;
        long refreshExp = System.currentTimeMillis() + ControlConfig.REFRESH_TOKEN_EXPIRATION_MS;

        String newAccessToken = createToken(user.getId(), user.getUsername(), user.getRole(), "ACCESS", accessExp);
        String newRefreshToken = createToken(user.getId(), user.getUsername(), user.getRole(), "REFRESH", refreshExp);

        activeTokens.remove(refreshTokenStr);
        activeTokens.put(newAccessToken, new TokenData(user.getId(), user.getUsername(), user.getRole(), "ACCESS", accessExp));
        activeTokens.put(newRefreshToken, new TokenData(user.getId(), user.getUsername(), user.getRole(), "REFRESH", refreshExp));

        return AuthResult.ok(newAccessToken, newRefreshToken, user, ControlConfig.ACCESS_TOKEN_EXPIRATION_MS / 1000);
    }

    public void revokeToken(String token) {
        if (token != null) {
            activeTokens.remove(token);
        }
    }

    public AdminUser validateToken(String token, String expectedType) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        TokenData cached = activeTokens.get(token);
        if (cached != null) {
            if (cached.isExpired() || (expectedType != null && !expectedType.equals(cached.tokenType))) {
                activeTokens.remove(token);
                return null;
            }
            return new AdminUser(cached.userId, cached.username, cached.role);
        }

        // Verify token signature if not in local cache
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String signature = parts[1];

            String expectedSig = hmacSha256(parts[0], ControlConfig.JWT_SECRET);
            if (!constantTimeEquals(signature, expectedSig)) {
                return null;
            }

            JSONObject json = (JSONObject) JSONValue.parse(payloadJson);
            if (json == null) return null;

            long exp = ((Number) json.get("exp")).longValue();
            if (System.currentTimeMillis() > exp) return null;

            String type = (String) json.get("type");
            if (expectedType != null && !expectedType.equals(type)) return null;

            int uid = ((Number) json.get("uid")).intValue();
            String user = (String) json.get("user");
            UserRole role = UserRole.fromString((String) json.get("role"));

            activeTokens.put(token, new TokenData(uid, user, role, type, exp));
            return new AdminUser(uid, user, role);
        } catch (Exception e) {
            return null;
        }
    }

    private String createToken(int userId, String username, UserRole role, String type, long expiresAt) {
        JSONObject payload = new JSONObject();
        payload.put("uid", userId);
        payload.put("user", username);
        payload.put("role", role.name());
        payload.put("type", type);
        payload.put("exp", expiresAt);

        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256(encodedPayload, ControlConfig.JWT_SECRET);
        return encodedPayload + "." + signature;
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA256", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public static boolean verifyPassword(String inputPassword, String dbPassword) {
        if (inputPassword == null || dbPassword == null) return false;
        if (inputPassword.equals(dbPassword)) return true;

        String md5 = hashMD5(inputPassword);
        if (md5 != null && md5.equalsIgnoreCase(dbPassword)) return true;

        String sha256 = hashSHA256(inputPassword);
        if (sha256 != null && sha256.equalsIgnoreCase(dbPassword)) return true;

        return false;
    }

    public static String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String hashSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
