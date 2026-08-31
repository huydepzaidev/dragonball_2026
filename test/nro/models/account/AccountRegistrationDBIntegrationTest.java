package nro.models.account;

import nro.models.database.AccountDAO;
import nro.models.services.AccountAuthService;
import nro.models.services.AccountAuthService.AuthResult;
import nro.models.services.AccountRegisterService;
import nro.models.services.AccountRegisterService.RegisterPacket;
import nro.models.services.AccountRegisterService.RegisterResult;
import nro.models.utils.PasswordHasher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Full MariaDB Database Integration Test verifying production account creation,
 * authentication, duplicate prevention, concurrent race conditions, and legacy auto-rehash.
 *
 * Runs strictly against a temporary test database (e.g. test_teamobi2026_reg_<timestamp>).
 * Never touches runtime team2026 database.
 */
public class AccountRegistrationDBIntegrationTest {

    private static String DB_HOST = "localhost";
    private static String DB_PORT = "3306";
    private static String DB_USER = "root";
    private static String DB_PASS = "";

    public static void main(String[] args) {
        String runEnv = System.getenv("RUN_DB_INTEGRATION");
        String runProp = System.getProperty("RUN_DB_INTEGRATION");
        boolean runRequired = "1".equals(runEnv) || "true".equalsIgnoreCase(runEnv)
                || "1".equals(runProp) || "true".equalsIgnoreCase(runProp);

        if (!runRequired) {
            System.out.println("==========================================================");
            System.out.println(" [SKIPPED] Database integration tests are skipped by default.");
            System.out.println(" Set environment variable RUN_DB_INTEGRATION=1 to run against local MariaDB.");
            System.out.println("==========================================================");
            return;
        }

        System.out.println("==========================================================");
        System.out.println(" Running MariaDB Database Integration Tests (RUN_DB_INTEGRATION=1)...");
        System.out.println("==========================================================");

        loadDbProperties();

        String tempDbName = "test_teamobi2026_reg_" + System.currentTimeMillis();
        if (!tempDbName.matches("^test_teamobi2026_reg_\\d+$")) {
            throw new IllegalStateException("Generated temporary DB name is unsafe: " + tempDbName);
        }

        String rootUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false";
        String tempDbUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + tempDbName + "?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("[ERROR] MySQL/MariaDB JDBC driver not found in classpath!");
                System.exit(1);
                return;
            }
        }

        // Verify MariaDB connection is live
        try (Connection con = DriverManager.getConnection(rootUrl, DB_USER, DB_PASS)) {
            System.out.println("[DB Check] Connected to local database server successfully.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Database server not accessible at " + rootUrl + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            // 1. Create temporary database
            System.out.println("[1/8] Creating fresh temporary database: " + tempDbName);
            try (Connection con = DriverManager.getConnection(rootUrl, DB_USER, DB_PASS);
                 Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + tempDbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }

            // 2. Validate and import full bundle sql/teamobi2026.sql
            System.out.println("[2/8] Validating and importing canonical schema bundle sql/teamobi2026.sql...");
            executeSqlFile(tempDbUrl, new File("sql/teamobi2026.sql"));
            System.out.println("  -> Canonical schema bundle imported successfully!");

            // 3. Validate standalone migration idempotency
            System.out.println("[3/8] Validating standalone migration idempotency...");
            File migrationFile = new File("sql/migrations/2026_08_30_1430_expand_account_password.sql");
            if (!migrationFile.exists()) {
                throw new AssertionError("Migration file not found: " + migrationFile.getPath());
            }
            executeSqlFile(tempDbUrl, migrationFile);
            executeSqlFile(tempDbUrl, migrationFile); // Second run verifies rerun safety
            System.out.println("  -> Standalone migration executed twice idempotently!");

            // 4. Test Production Registration and Database Schema Integrity
            System.out.println("[4/8] Testing Production AccountRegisterService.register() and NOT NULL schema integrity...");
            try (Connection con = DriverManager.getConnection(tempDbUrl, DB_USER, DB_PASS)) {
                String testUser = "hero_test_01";
                String testPass = "PassSecret@2026";

                RegisterResult regResult = AccountRegisterService.gI().register(con, testUser, testPass, "127.0.0.1");
                assertTrue(regResult.success(), "Production registration must succeed");
                assertTrue(AccountRegisterService.shouldTriggerAutoLogin(regResult), "Auto-login decision must be true for successful registration");

                // Verify exact DB row written by AccountDAO
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM account WHERE username = ?")) {
                    ps.setString(1, testUser);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next(), "Account row must exist in DB");
                        assertEquals(testUser, rs.getString("username"), "Username must match");
                        String dbHash = rs.getString("password");
                        assertTrue(dbHash.startsWith("$pbkdf2-sha256$i=100000,l=32$"), "Stored password must be PBKDF2 with 100k iterations");
                        assertEquals("", rs.getString("email"), "Email must be empty string (NOT NULL)");
                        assertEquals("", rs.getString("token"), "Token must be empty string (NOT NULL)");
                        assertEquals("", rs.getString("xsrf_token"), "xsrf_token must be empty string (NOT NULL)");
                        assertEquals("", rs.getString("newpass"), "newpass must be empty string (NOT NULL)");
                        assertEquals(0, rs.getInt("ban"), "Ban must be 0");
                        assertEquals(1, rs.getInt("active"), "Active must be 1");
                    }
                }

                // Verify duplicate registration rejection via production service
                RegisterResult dupResult = AccountRegisterService.gI().register(con, testUser, "AnotherPassword123", "127.0.0.1");
                assertFalse(dupResult.success(), "Duplicate username registration must fail");
                assertFalse(AccountRegisterService.shouldTriggerAutoLogin(dupResult), "Auto-login decision must be false for duplicate failure");
                assertTrue(dupResult.message().contains("đã tồn tại"), "Duplicate error message must be user-friendly");
                System.out.println("  -> Production registration and duplicate rejection verified!");
            }

            // 5. Test Concurrent Registration Race Condition
            System.out.println("[5/8] Testing Concurrent Registration Race Condition with Production Service...");
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger duplicateCount = new AtomicInteger(0);
            String raceUser = "concurrent_race_user";

            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                tasks.add(() -> {
                    try (Connection threadCon = DriverManager.getConnection(tempDbUrl, DB_USER, DB_PASS)) {
                        RegisterResult res = AccountRegisterService.gI().register(threadCon, raceUser, "PassSecret@2026", "127.0.0.1");
                        if (res.success()) {
                            successCount.incrementAndGet();
                        } else {
                            duplicateCount.incrementAndGet();
                        }
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
            executor.shutdown();

            assertEquals(1, successCount.get(), "Exactly 1 thread must succeed inserting the username");
            assertEquals(threadCount - 1, duplicateCount.get(), "All other concurrent threads must fail with duplicate key");
            System.out.println("  -> Concurrent race condition safety verified (1 success, 4 duplicates)!");

            // 6. Test Production AccountAuthService Authentication & Auto-Rehash
            System.out.println("[6/8] Testing Production AccountAuthService Authentication & Auto-Rehash...");
            try (Connection con = DriverManager.getConnection(tempDbUrl, DB_USER, DB_PASS)) {
                // A. Plaintext legacy account
                insertLegacyAccountDirectly(con, "legacy_plain", "mypass123");

                // Wrong password -> auth fails, hash remains plaintext
                AuthResult failPlain = AccountAuthService.authenticateAndRehash(con, "legacy_plain", "wrongpass");
                assertFalse(failPlain.authenticated(), "Wrong password must fail authentication");
                assertEquals("mypass123", getStoredPassword(con, "legacy_plain"), "Failed login must NOT modify stored hash");

                // Correct password -> auth succeeds, hash is upgraded to PBKDF2 in DB
                AuthResult okPlain = AccountAuthService.authenticateAndRehash(con, "legacy_plain", "mypass123");
                assertTrue(okPlain.authenticated(), "Correct password must authenticate");
                assertTrue(okPlain.rehashed(), "Legacy password must trigger rehash flag");
                String upgradedPlainHash = getStoredPassword(con, "legacy_plain");
                assertTrue(upgradedPlainHash.startsWith("$pbkdf2-sha256$i=100000,l=32$"), "Stored hash must now be upgraded PBKDF2");

                // Subsequent login -> authenticates with upgraded hash, no further rehash needed
                AuthResult recheckPlain = AccountAuthService.authenticateAndRehash(con, "legacy_plain", "mypass123");
                assertTrue(recheckPlain.authenticated(), "Subsequent login with upgraded hash must succeed");
                assertFalse(recheckPlain.rehashed(), "Already upgraded hash must not trigger rehash again");

                // B. MD5 legacy account
                String md5Hash = PasswordHasher.hashMD5("md5secret99");
                insertLegacyAccountDirectly(con, "legacy_md5", md5Hash);

                AuthResult failMd5 = AccountAuthService.authenticateAndRehash(con, "legacy_md5", "wrongpass");
                assertFalse(failMd5.authenticated(), "Wrong password must fail MD5 auth");
                assertEquals(md5Hash, getStoredPassword(con, "legacy_md5"), "Failed login must NOT modify MD5 hash");

                AuthResult okMd5 = AccountAuthService.authenticateAndRehash(con, "legacy_md5", "md5secret99");
                assertTrue(okMd5.authenticated(), "Correct password must authenticate MD5 account");
                assertTrue(okMd5.rehashed(), "MD5 account must trigger rehash flag");
                assertTrue(getStoredPassword(con, "legacy_md5").startsWith("$pbkdf2-sha256$i=100000,l=32$"), "MD5 hash must be upgraded to PBKDF2");

                // C. SHA-256 legacy account
                String sha256Hash = PasswordHasher.hashSHA256("shaSecret2026");
                insertLegacyAccountDirectly(con, "legacy_sha256", sha256Hash);

                AuthResult failSha = AccountAuthService.authenticateAndRehash(con, "legacy_sha256", "wrongpass");
                assertFalse(failSha.authenticated(), "Wrong password must fail SHA-256 auth");

                AuthResult okSha = AccountAuthService.authenticateAndRehash(con, "legacy_sha256", "shaSecret2026");
                assertTrue(okSha.authenticated(), "Correct password must authenticate SHA-256 account");
                assertTrue(okSha.rehashed(), "SHA-256 account must trigger rehash flag");
                assertTrue(getStoredPassword(con, "legacy_sha256").startsWith("$pbkdf2-sha256$i=100000,l=32$"), "SHA-256 hash must be upgraded to PBKDF2");

                System.out.println("  -> Production AccountAuthService and auto-rehash verified across all formats!");
            }

            // 7. Test Password Update via Production AccountDAO
            System.out.println("[7/8] Testing Password Update via Production AccountDAO.updatePassword()...");
            try (Connection con = DriverManager.getConnection(tempDbUrl, DB_USER, DB_PASS)) {
                String userToChange = "user_change_pass";
                RegisterResult res = AccountRegisterService.gI().register(con, userToChange, "InitialPass123", "127.0.0.1");
                assertTrue(res.success(), "Account creation must succeed");

                int accountId = getAccountId(con, userToChange);
                String newHashedPass = PasswordHasher.hashPassword("BrandNewPass456");

                boolean updated = AccountDAO.updatePassword(con, accountId, userToChange, newHashedPass);
                assertTrue(updated, "AccountDAO.updatePassword must return true");

                // Old password must fail
                AuthResult oldAuth = AccountAuthService.authenticateAndRehash(con, userToChange, "InitialPass123");
                assertFalse(oldAuth.authenticated(), "Old password must fail authentication after change");

                // New password must succeed
                AuthResult newAuth = AccountAuthService.authenticateAndRehash(con, userToChange, "BrandNewPass456");
                assertTrue(newAuth.authenticated(), "New password must succeed authentication");

                System.out.println("  -> Password update flow verified!");
            }

            // 8. Test Packet Parser and Character Creation workflow precondition
            System.out.println("[8/8] Testing Packet Parsing edge cases & Character Creation workflow precondition...");
            testPacketParserCases();

            try (Connection con = DriverManager.getConnection(tempDbUrl, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM player WHERE account_id = ?")) {
                ps.setInt(1, 99999);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Result set should have count");
                    assertEquals(0, rs.getInt(1), "New account should have 0 existing players");
                }
            }

            System.out.println("==========================================================");
            System.out.println(" [PASSED] All MariaDB Database Integration Tests passed!");
            System.out.println("==========================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Database Integration Test encountered an unexpected failure: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Drop temporary database to clean up
            if (tempDbName != null && tempDbName.startsWith("test_teamobi2026_reg_")) {
                System.out.println("[Cleanup] Dropping verified temporary database: " + tempDbName);
                try (Connection con = DriverManager.getConnection(rootUrl, DB_USER, DB_PASS);
                     Statement stmt = con.createStatement()) {
                    stmt.executeUpdate("DROP DATABASE IF EXISTS `" + tempDbName + "`");
                    System.out.println("  -> Temporary database dropped successfully.");
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed dropping temporary database: " + e.getMessage());
                    System.exit(1);
                }
            }
        }
    }

    private static void testPacketParserCases() throws IOException {
        // A. Standard clean packet
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(baos1);
        dos1.writeUTF("std_user");
        dos1.writeUTF("std_pass");
        RegisterPacket p1 = AccountRegisterService.parseRegisterPacket(new DataInputStream(new ByteArrayInputStream(baos1.toByteArray())));
        assertTrue(p1.valid(), "Standard packet must be valid");
        assertEquals("std_user", p1.username(), "Username matches");
        assertEquals("std_pass", p1.password(), "Password matches");

        // B. Legacy packet with extra trailing UTF fields
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        DataOutputStream dos2 = new DataOutputStream(baos2);
        dos2.writeUTF("leg_user");
        dos2.writeUTF("leg_pass");
        dos2.writeUTF("userAo");
        dos2.writeUTF("passAo");
        dos2.writeUTF("2.3.7");
        DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(baos2.toByteArray()));
        RegisterPacket p2 = AccountRegisterService.parseRegisterPacket(dis2);
        assertTrue(p2.valid(), "Legacy packet with extra fields must parse validly");
        assertEquals("leg_user", p2.username(), "Username matches");
        assertEquals("leg_pass", p2.password(), "Password matches");
        assertTrue(dis2.available() > 0, "Extra trailing fields must remain available unread");

        // C. Truncated packet (missing password)
        ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        DataOutputStream dos3 = new DataOutputStream(baos3);
        dos3.writeUTF("only_user");
        RegisterPacket p3 = AccountRegisterService.parseRegisterPacket(new DataInputStream(new ByteArrayInputStream(baos3.toByteArray())));
        assertFalse(p3.valid(), "Truncated packet must be invalid");
        assertTrue(p3.error() != null, "Error message must be present");

        // D. Null / empty stream
        RegisterPacket pNull = AccountRegisterService.parseRegisterPacket(null);
        assertFalse(pNull.valid(), "Null stream must be invalid");

        RegisterPacket pEmpty = AccountRegisterService.parseRegisterPacket(new DataInputStream(new ByteArrayInputStream(new byte[0])));
        assertFalse(pEmpty.valid(), "Empty stream must be invalid");
    }

    private static void insertLegacyAccountDirectly(Connection con, String username, String storedPassword) throws SQLException {
        String sql = "INSERT INTO account ("
                + "username, password, email, token, xsrf_token, newpass, "
                + "create_time, update_time, ban, is_admin, last_time_login, last_time_logout, "
                + "ip_address, active, thoi_vang, server_login, bd_player, is_gift_box, "
                + "gift_time, vnd, tongnap, luotquay, vang, event_point, vip, "
                + "tichdiem, point_post, last_post, baiviet, xacminh, admin"
                + ") VALUES ("
                + "?, ?, '', '', '', '', "
                + "NOW(), NOW(), 0, 0, NOW(), NOW(), "
                + "'127.0.0.1', 1, 0, -1, 1, 0, "
                + "'0', 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0"
                + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, storedPassword);
            ps.executeUpdate();
        }
    }

    private static String getStoredPassword(Connection con, String username) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT password FROM account WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
                throw new IllegalStateException("Account not found: " + username);
            }
        }
    }

    private static int getAccountId(Connection con, String username) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM account WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
                throw new IllegalStateException("Account not found: " + username);
            }
        }
    }

    private static void executeSqlFile(String dbUrl, File file) throws Exception {
        try (Connection con = DriverManager.getConnection(dbUrl, DB_USER, DB_PASS);
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            String delimiter = ";";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || (trimmed.startsWith("/*") && trimmed.endsWith("*/"))) {
                    continue;
                }
                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }
                sb.append(line).append("\n");
                if (sb.toString().trim().endsWith(delimiter)) {
                    String stmtStr = sb.toString().trim();
                    stmtStr = stmtStr.substring(0, stmtStr.length() - delimiter.length()).trim();
                    if (!stmtStr.isEmpty()) {
                        try (Statement s = con.createStatement()) {
                            s.execute(stmtStr);
                        }
                    }
                    sb.setLength(0);
                }
            }
            if (sb.length() > 0 && !sb.toString().trim().isEmpty()) {
                try (Statement s = con.createStatement()) {
                    s.execute(sb.toString().trim());
                }
            }
        }
    }

    private static void loadDbProperties() {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("Config.properties")) {
            p.load(fis);
            if (p.containsKey("database.host")) DB_HOST = p.getProperty("database.host");
            if (p.containsKey("database.port")) DB_PORT = p.getProperty("database.port");
            if (p.containsKey("database.user")) DB_USER = p.getProperty("database.user");
            if (p.containsKey("database.pass")) DB_PASS = p.getProperty("database.pass");
        } catch (Exception ignored) {
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion failed: " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
    }
}
