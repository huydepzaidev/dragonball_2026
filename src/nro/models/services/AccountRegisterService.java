package nro.models.services;

import nro.models.database.AccountDAO;
import nro.models.database.AccountDAO.CreateAccountStatus;
import nro.models.network.MySession;
import nro.models.utils.PasswordHasher;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Service handling account registration validations, packet parsing, and creation.
 */
public final class AccountRegisterService {

    private static final AccountRegisterService INSTANCE = new AccountRegisterService();

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 64;

    public record RegisterPacket(boolean valid, String username, String password, String error) {
        public static RegisterPacket valid(String username, String password) {
            return new RegisterPacket(true, username, password, null);
        }

        public static RegisterPacket invalid(String error) {
            return new RegisterPacket(false, null, null, error);
        }
    }

    public record RegisterResult(boolean success, String message, String username, String password) {
        public static RegisterResult success(String username, String password) {
            return new RegisterResult(true, "Đăng ký tài khoản thành công!", username, password);
        }

        public static RegisterResult fail(String message) {
            return new RegisterResult(false, message, null, null);
        }
    }

    private AccountRegisterService() {
    }

    public static AccountRegisterService gI() {
        return INSTANCE;
    }

    /**
     * Parses the registration packet payload from a DataInputStream.
     * Reads strictly the first two UTF strings (username and password).
     * Any subsequent legacy bytes/strings are cleanly left untouched.
     */
    public static RegisterPacket parseRegisterPacket(DataInputStream dis) {
        if (dis == null) {
            return RegisterPacket.invalid("Dữ liệu đăng ký không hợp lệ.");
        }
        try {
            String username = dis.readUTF();
            String password = dis.readUTF();
            return RegisterPacket.valid(username, password);
        } catch (EOFException e) {
            return RegisterPacket.invalid("Gói tin đăng ký thiếu thông tin tài khoản hoặc mật khẩu.");
        } catch (IOException e) {
            return RegisterPacket.invalid("Dữ liệu đăng ký không hợp lệ.");
        }
    }

    /**
     * Determines whether a registration outcome warrants triggering automatic login.
     */
    public static boolean shouldTriggerAutoLogin(RegisterResult result) {
        return result != null && result.success() && result.username() != null && result.password() != null;
    }

    /**
     * Validates and registers a new account using the global connection pool.
     */
    public RegisterResult register(String rawUsername, String rawPassword, String ip) {
        String validationError = validateCredentials(rawUsername, rawPassword);
        if (validationError != null) {
            return RegisterResult.fail(validationError);
        }

        String username = rawUsername.trim();
        String password = rawPassword;

        // Friendly duplicate check before hashing
        if (AccountDAO.existsByUsername(username)) {
            return RegisterResult.fail("Tên tài khoản đã tồn tại, vui lòng chọn tên khác.");
        }

        // Hash password securely with PBKDF2
        String hashedPassword = PasswordHasher.hashPassword(password);
        String safeIp = (ip != null && !ip.isBlank()) ? ip.trim() : "127.0.0.1";

        // Create account in database
        CreateAccountStatus status = AccountDAO.createAccount(username, hashedPassword, safeIp);
        return switch (status) {
            case SUCCESS -> RegisterResult.success(username, password);
            case DUPLICATE_USERNAME -> RegisterResult.fail("Tên tài khoản đã tồn tại, vui lòng chọn tên khác.");
            case DATABASE_ERROR -> RegisterResult.fail("Đăng ký thất bại do lỗi hệ thống, vui lòng thử lại sau.");
        };
    }

    /**
     * Validates and registers a new account using an explicit database connection (for testing and transactional use).
     */
    public RegisterResult register(Connection con, String rawUsername, String rawPassword, String ip) throws SQLException {
        String validationError = validateCredentials(rawUsername, rawPassword);
        if (validationError != null) {
            return RegisterResult.fail(validationError);
        }

        String username = rawUsername.trim();
        String password = rawPassword;

        if (AccountDAO.existsByUsername(con, username)) {
            return RegisterResult.fail("Tên tài khoản đã tồn tại, vui lòng chọn tên khác.");
        }

        String hashedPassword = PasswordHasher.hashPassword(password);
        String safeIp = (ip != null && !ip.isBlank()) ? ip.trim() : "127.0.0.1";

        CreateAccountStatus status = AccountDAO.createAccount(con, username, hashedPassword, safeIp);
        return switch (status) {
            case SUCCESS -> RegisterResult.success(username, password);
            case DUPLICATE_USERNAME -> RegisterResult.fail("Tên tài khoản đã tồn tại, vui lòng chọn tên khác.");
            case DATABASE_ERROR -> RegisterResult.fail("Đăng ký thất bại do lỗi hệ thống, vui lòng thử lại sau.");
        };
    }

    /**
     * Validates and registers a new account for an active player session.
     */
    public RegisterResult register(MySession session, String rawUsername, String rawPassword) {
        String ip = (session != null) ? session.ipAddress : "127.0.0.1";
        return register(rawUsername, rawPassword, ip);
    }

    /**
     * Performs strict validation on username and password.
     *
     * @return null if valid, or a Vietnamese error message if invalid
     */
    public String validateCredentials(String rawUsername, String rawPassword) {
        if (rawUsername == null || rawUsername.isBlank()) {
            return "Tên tài khoản không được để trống.";
        }

        String username = rawUsername.trim();

        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Tên tài khoản phải từ " + MIN_USERNAME_LENGTH + " đến " + MAX_USERNAME_LENGTH + " ký tự.";
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Tên tài khoản chỉ được chứa chữ cái (a-z, A-Z), chữ số (0-9) và dấu gạch dưới (_).";
        }

        if (rawPassword == null || rawPassword.isEmpty()) {
            return "Mật khẩu không được để trống.";
        }

        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return "Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.";
        }

        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            return "Mật khẩu quá dài (tối đa " + MAX_PASSWORD_LENGTH + " ký tự).";
        }

        // Check for control characters or invalid whitespace in password
        for (int i = 0; i < rawPassword.length(); i++) {
            char c = rawPassword.charAt(i);
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                return "Mật khẩu không được chứa khoảng trắng hoặc ký tự điều khiển.";
            }
        }

        return null;
    }
}
