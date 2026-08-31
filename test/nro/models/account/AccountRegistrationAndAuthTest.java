package nro.models.account;

import nro.models.services.AccountRegisterService;
import nro.models.services.RegisterRateLimiter;
import nro.models.utils.PasswordHasher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

public class AccountRegistrationAndAuthTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println(" Running AccountRegistrationAndAuthTest Suite...");
        System.out.println("==========================================================");

        testPasswordHasherBasicAndLegacy();
        testPasswordHasherDosProtectionAndMalformed();
        testAccountValidation();
        testRegisterRateLimiter();
        testPacketParsingAndCompatibility();
        benchmarkPBKDF2();

        System.out.println("==========================================================");
        System.out.println(" [SUCCESS] All AccountRegistrationAndAuthTest tests passed!");
        System.out.println("==========================================================");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Test failed: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Test failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Test failed: " + message + " (Expected: " + expected + ", Actual: " + actual + ")");
    }

    private static void testPasswordHasherBasicAndLegacy() {
        System.out.println("[1/6] Testing PasswordHasher Basic & Legacy Fallbacks...");

        // 1. Basic hashing and verification (100,000 iterations)
        String rawPass = "Secret123@#";
        String hashed = PasswordHasher.hashPassword(rawPass);
        assertTrue(hashed.startsWith("$pbkdf2-sha256$i=100000,l=32$"), "Hash should start with PBKDF2 100k header");
        assertTrue(PasswordHasher.checkPassword(rawPass, hashed), "PasswordHasher should verify correct password");
        assertFalse(PasswordHasher.checkPassword("WrongPassword", hashed), "PasswordHasher should reject wrong password");
        assertFalse(PasswordHasher.needsRehash(hashed), "Current PBKDF2 format should not need rehash");

        // 2. Legacy Plaintext compatibility
        String plain = "admin123";
        assertTrue(PasswordHasher.checkPassword("admin123", plain), "Plaintext password should match");
        assertFalse(PasswordHasher.checkPassword("admin124", plain), "Plaintext wrong password should fail");
        assertTrue(PasswordHasher.needsRehash(plain), "Plaintext password must be flagged for rehash");

        // 3. Legacy MD5 compatibility
        String md5 = PasswordHasher.hashMD5("123456");
        assertEquals(32, md5.length(), "MD5 length must be 32");
        assertTrue(PasswordHasher.checkPassword("123456", md5), "MD5 hash should match");
        assertFalse(PasswordHasher.checkPassword("654321", md5), "MD5 wrong password should fail");
        assertTrue(PasswordHasher.needsRehash(md5), "MD5 password must be flagged for rehash");

        // 4. Legacy SHA-256 compatibility
        String sha256 = PasswordHasher.hashSHA256("dragonball2026");
        assertEquals(64, sha256.length(), "SHA256 length must be 64");
        assertTrue(PasswordHasher.checkPassword("dragonball2026", sha256), "SHA256 hash should match");
        assertFalse(PasswordHasher.checkPassword("wrongpass", sha256), "SHA256 wrong password should fail");
        assertTrue(PasswordHasher.needsRehash(sha256), "SHA256 password must be flagged for rehash");

        // 5. Null safety
        assertFalse(PasswordHasher.checkPassword(null, hashed), "Null candidate password should return false");
        assertFalse(PasswordHasher.checkPassword("test", null), "Null stored hash should return false");
        assertTrue(PasswordHasher.needsRehash(null), "Null stored hash should need rehash");

        System.out.println("  -> PasswordHasher Basic & Legacy tests passed!");
    }

    private static void testPasswordHasherDosProtectionAndMalformed() {
        System.out.println("[2/6] Testing PasswordHasher DoS Safety Bounds & Malformed Strings...");

        String saltB64 = Base64.getEncoder().encodeToString(new byte[16]);
        String hashB64 = Base64.getEncoder().encodeToString(new byte[32]);

        // Malformed hash formats
        assertFalse(PasswordHasher.checkPassword("pass", "$pbkdf2-sha256$malformed"), "Malformed prefix should return false");
        assertFalse(PasswordHasher.checkPassword("pass", "$pbkdf2-sha256$i=100000$missingParts"), "Missing parts should return false");
        assertFalse(PasswordHasher.checkPassword("pass", "$pbkdf2-sha256$invalidParams$" + saltB64 + "$" + hashB64), "Invalid params should return false");
        assertFalse(PasswordHasher.checkPassword("pass", "$pbkdf2-sha256$i=100000,l=32$not-valid-base64!$" + hashB64), "Invalid salt base64 should return false");

        // DoS bounds protection: iterations too high (> 500,000)
        String hugeIterations = "$pbkdf2-sha256$i=50000000,l=32$" + saltB64 + "$" + hashB64;
        assertFalse(PasswordHasher.checkPassword("pass", hugeIterations), "Huge iterations must be rejected immediately to prevent DoS");

        // DoS bounds protection: iterations too low (< 1,000)
        String tinyIterations = "$pbkdf2-sha256$i=10,l=32$" + saltB64 + "$" + hashB64;
        assertFalse(PasswordHasher.checkPassword("pass", tinyIterations), "Tiny iterations must be rejected");

        // Key length out of bounds (< 16 or > 64)
        String hugeKeyLen = "$pbkdf2-sha256$i=100000,l=1024$" + saltB64 + "$" + hashB64;
        assertFalse(PasswordHasher.checkPassword("pass", hugeKeyLen), "Huge key length must be rejected");

        // Salt size out of bounds (< 8 or > 128)
        String hugeSaltB64 = Base64.getEncoder().encodeToString(new byte[512]);
        String hugeSaltHash = "$pbkdf2-sha256$i=100000,l=32$" + hugeSaltB64 + "$" + hashB64;
        assertFalse(PasswordHasher.checkPassword("pass", hugeSaltHash), "Huge salt must be rejected");

        System.out.println("  -> PasswordHasher DoS safety bounds passed!");
    }

    private static void testAccountValidation() {
        System.out.println("[3/6] Testing AccountRegisterService credential validation...");
        AccountRegisterService service = AccountRegisterService.gI();

        // Valid cases
        assertEquals(null, service.validateCredentials("huydev2026", "123456"), "Valid alphanumeric user and pass");
        assertEquals(null, service.validateCredentials("admin_01", "P@ssw0rd!#%"), "Valid user with underscore and complex pass");
        assertEquals(null, service.validateCredentials("abc", "123456"), "Valid 3-char username");
        assertEquals(null, service.validateCredentials("12345678901234567890", "123456"), "Valid 20-char username");

        // Invalid usernames
        assertTrue(service.validateCredentials(null, "123456") != null, "Null username should fail");
        assertTrue(service.validateCredentials("   ", "123456") != null, "Empty username should fail");
        assertTrue(service.validateCredentials("ab", "123456") != null, "Short username (<3 chars) should fail");
        assertTrue(service.validateCredentials("123456789012345678901", "123456") != null, "Long username (>20 chars) should fail");
        assertTrue(service.validateCredentials("user name", "123456") != null, "Username with space should fail");
        assertTrue(service.validateCredentials("user@mail", "123456") != null, "Username with @ should fail");
        assertTrue(service.validateCredentials("user$#", "123456") != null, "Username with special chars should fail");

        // Invalid passwords
        assertTrue(service.validateCredentials("huydev", null) != null, "Null password should fail");
        assertTrue(service.validateCredentials("huydev", "") != null, "Empty password should fail");
        assertTrue(service.validateCredentials("huydev", "12345") != null, "Short password (<6 chars) should fail");
        StringBuilder longPass = new StringBuilder();
        for (int i = 0; i < 65; i++) longPass.append("a");
        assertTrue(service.validateCredentials("huydev", longPass.toString()) != null, "Long password (>64 chars) should fail");
        assertTrue(service.validateCredentials("huydev", "pass word") != null, "Password with space should fail");
        assertTrue(service.validateCredentials("huydev", "pass\nword") != null, "Password with newline should fail");
        assertTrue(service.validateCredentials("huydev", "pass\tword") != null, "Password with tab should fail");

        System.out.println("  -> Credential validation tests passed!");
    }

    private static void testRegisterRateLimiter() {
        System.out.println("[4/6] Testing RegisterRateLimiter...");
        RegisterRateLimiter limiter = RegisterRateLimiter.gI();
        String testIp = "192.168.1.99";
        limiter.reset(testIp);

        // First attempt -> should succeed
        assertTrue(limiter.tryAcquire(testIp), "First attempt should succeed");

        // Immediate consecutive attempt (<2s) -> should fail cooldown
        assertFalse(limiter.tryAcquire(testIp), "Immediate consecutive attempt must fail cooldown");

        // Reset and test IP isolation
        limiter.reset(testIp);
        assertTrue(limiter.tryAcquire(testIp), "Attempt after reset should succeed");

        String testIp2 = "192.168.1.100";
        limiter.reset(testIp2);
        assertTrue(limiter.tryAcquire(testIp2), "Different IP should not be blocked");

        System.out.println("  -> RegisterRateLimiter tests passed!");
    }

    private static void testPacketParsingAndCompatibility() {
        System.out.println("[5/6] Testing Registration Packet Parsing (Clean, Legacy, Truncated, Malformed)...");

        // 1. Standard packet (2 UTF: username, password)
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("huydev_acc");
            dos.writeUTF("secret123");

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
            AccountRegisterService.RegisterPacket packet = AccountRegisterService.parseRegisterPacket(dis);
            assertTrue(packet.valid(), "Standard packet must be valid");
            assertEquals("huydev_acc", packet.username(), "Username matches");
            assertEquals("secret123", packet.password(), "Password matches");
        } catch (IOException e) {
            throw new AssertionError("Failed testing standard packet", e);
        }

        // 2. Legacy packet with extra UTF fields (username, password, userAo, passAo, version)
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("legacy_user");
            dos.writeUTF("legacy_pass");
            dos.writeUTF("userAo123");
            dos.writeUTF("passAo123");
            dos.writeUTF("2.3.7");

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
            AccountRegisterService.RegisterPacket packet = AccountRegisterService.parseRegisterPacket(dis);
            assertTrue(packet.valid(), "Legacy packet with extra UTF fields must parse validly");
            assertEquals("legacy_user", packet.username(), "Username matches");
            assertEquals("legacy_pass", packet.password(), "Password matches");
            assertTrue(dis.available() > 0, "Extra trailing bytes remain unread cleanly");
        } catch (IOException e) {
            throw new AssertionError("Failed testing legacy packet", e);
        }

        // 3. Truncated packet missing password
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("only_user");
            // password omitted

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
            AccountRegisterService.RegisterPacket packet = AccountRegisterService.parseRegisterPacket(dis);
            assertFalse(packet.valid(), "Truncated packet missing password must be invalid");
            assertTrue(packet.error() != null, "Error message must be present");
        } catch (IOException e) {
            throw new AssertionError("Failed testing truncated packet", e);
        }

        // 4. Malformed / Empty packet
        DataInputStream emptyDis = new DataInputStream(new ByteArrayInputStream(new byte[0]));
        AccountRegisterService.RegisterPacket emptyPacket = AccountRegisterService.parseRegisterPacket(emptyDis);
        assertFalse(emptyPacket.valid(), "Empty packet must be invalid");

        AccountRegisterService.RegisterPacket nullPacket = AccountRegisterService.parseRegisterPacket(null);
        assertFalse(nullPacket.valid(), "Null stream must be invalid");

        // 5. Test shouldTriggerAutoLogin decision
        assertTrue(AccountRegisterService.shouldTriggerAutoLogin(AccountRegisterService.RegisterResult.success("validUser", "validPass")),
                "Auto-login must be true on success result");
        assertFalse(AccountRegisterService.shouldTriggerAutoLogin(AccountRegisterService.RegisterResult.fail("Some error")),
                "Auto-login must be false on fail result");
        assertFalse(AccountRegisterService.shouldTriggerAutoLogin(null),
                "Auto-login must be false on null result");
        assertFalse(AccountRegisterService.shouldTriggerAutoLogin(new AccountRegisterService.RegisterResult(true, "msg", null, "pass")),
                "Auto-login must be false if username is null");
        assertFalse(AccountRegisterService.shouldTriggerAutoLogin(new AccountRegisterService.RegisterResult(true, "msg", "user", null)),
                "Auto-login must be false if password is null");

        System.out.println("  -> Registration Packet Parsing & Auto-login Decision tests passed!");
    }

    private static void benchmarkPBKDF2() {
        System.out.println("[6/6] Benchmarking PBKDF2 Performance on current machine...");
        String testPassword = "BenchmarkPassword@2026";

        // Warmup JVM
        for (int i = 0; i < 3; i++) {
            String h = PasswordHasher.hashPassword(testPassword);
            PasswordHasher.checkPassword(testPassword, h);
        }

        int iterations = 10;
        long totalHashTime = 0;
        long totalVerifyTime = 0;

        for (int i = 0; i < iterations; i++) {
            long t1 = System.nanoTime();
            String hash = PasswordHasher.hashPassword(testPassword);
            long t2 = System.nanoTime();
            boolean valid = PasswordHasher.checkPassword(testPassword, hash);
            long t3 = System.nanoTime();

            assertTrue(valid, "Password verify must be valid");
            totalHashTime += (t2 - t1);
            totalVerifyTime += (t3 - t2);
        }

        double avgHashMs = (totalHashTime / (double) iterations) / 1_000_000.0;
        double avgVerifyMs = (totalVerifyTime / (double) iterations) / 1_000_000.0;

        System.out.printf("  -> PBKDF2 Benchmark Results (Iterations: %d, Rounds: %d):%n", PasswordHasher.DEFAULT_ITERATIONS, iterations);
        System.out.printf("     - Average Hash Time:   %.2f ms%n", avgHashMs);
        System.out.printf("     - Average Verify Time: %.2f ms%n", avgVerifyMs);
    }
}
