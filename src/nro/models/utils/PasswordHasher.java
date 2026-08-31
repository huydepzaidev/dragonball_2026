package nro.models.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Utility for hashing and verifying passwords securely using PBKDF2WithHmacSHA256,
 * while maintaining backward compatibility with legacy plaintext, MD5, and SHA-256 accounts.
 * Includes strict boundary checks to prevent CPU/memory exhaustion DoS attacks.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_PREFIX = "$pbkdf2-sha256$";

    // Modern secure defaults
    public static final int DEFAULT_ITERATIONS = 100_000;
    public static final int SALT_BYTES = 16;
    public static final int KEY_LENGTH_BYTES = 32;
    public static final int KEY_LENGTH_BITS = KEY_LENGTH_BYTES * 8;

    // Safety bounds for parsing stored hashes (DoS mitigation)
    public static final int MIN_ALLOWED_ITERATIONS = 1_000;
    public static final int MAX_ALLOWED_ITERATIONS = 500_000;
    public static final int MIN_ALLOWED_KEY_LENGTH = 16;
    public static final int MAX_ALLOWED_KEY_LENGTH = 64;
    public static final int MIN_SALT_BYTES = 8;
    public static final int MAX_SALT_BYTES = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hashes a plaintext password into a PBKDF2-HMAC-SHA256 formatted string.
     * Format: $pbkdf2-sha256$i=100000,l=32$<salt_base64>$<hash_base64>
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        return HASH_PREFIX + "i=" + DEFAULT_ITERATIONS + ",l=" + KEY_LENGTH_BYTES + "$" + saltB64 + "$" + hashB64;
    }

    /**
     * Checks whether a candidate raw password matches the stored password.
     * Supports PBKDF2 ($pbkdf2-sha256$), plain text, MD5, and SHA-256 legacy formats.
     */
    public static boolean checkPassword(String candidate, String storedPassword) {
        if (candidate == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith(HASH_PREFIX)) {
            return verifyPbkdf2(candidate, storedPassword);
        }

        // Legacy plain text check (constant time)
        if (MessageDigest.isEqual(candidate.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        // Legacy MD5 check (32 hex characters)
        if (storedPassword.length() == 32) {
            String md5 = hashMD5(candidate);
            if (md5 != null && MessageDigest.isEqual(md5.toLowerCase().getBytes(StandardCharsets.UTF_8), storedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        // Legacy SHA-256 check (64 hex characters)
        if (storedPassword.length() == 64) {
            String sha256 = hashSHA256(candidate);
            if (sha256 != null && MessageDigest.isEqual(sha256.toLowerCase().getBytes(StandardCharsets.UTF_8), storedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the stored password should be upgraded to the latest PBKDF2 scheme.
     */
    public static boolean needsRehash(String storedPassword) {
        if (storedPassword == null) {
            return true;
        }
        String currentConfigPrefix = HASH_PREFIX + "i=" + DEFAULT_ITERATIONS + ",l=" + KEY_LENGTH_BYTES + "$";
        return !storedPassword.startsWith(currentConfigPrefix);
    }

    private static boolean verifyPbkdf2(String candidate, String storedPassword) {
        try {
            // Expected format: $pbkdf2-sha256$i=100000,l=32$<salt>$<hash>
            String content = storedPassword.substring(HASH_PREFIX.length());
            String[] parts = content.split("\\$");
            if (parts.length != 3) {
                return false;
            }

            String params = parts[0];
            String saltB64 = parts[1];
            String hashB64 = parts[2];

            int iterations = DEFAULT_ITERATIONS;
            int keyLenBytes = KEY_LENGTH_BYTES;

            for (String param : params.split(",")) {
                if (param.startsWith("i=")) {
                    iterations = Integer.parseInt(param.substring(2));
                } else if (param.startsWith("l=")) {
                    keyLenBytes = Integer.parseInt(param.substring(2));
                }
            }

            // Enforce bounds to prevent CPU / memory exhaustion attacks
            if (iterations < MIN_ALLOWED_ITERATIONS || iterations > MAX_ALLOWED_ITERATIONS) {
                return false;
            }
            if (keyLenBytes < MIN_ALLOWED_KEY_LENGTH || keyLenBytes > MAX_ALLOWED_KEY_LENGTH) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(saltB64);
            if (salt.length < MIN_SALT_BYTES || salt.length > MAX_SALT_BYTES) {
                return false;
            }

            byte[] expectedHash = Base64.getDecoder().decode(hashB64);
            if (expectedHash.length != keyLenBytes) {
                return false;
            }

            byte[] computedHash = pbkdf2(candidate.toCharArray(), salt, iterations, keyLenBytes * 8);

            return MessageDigest.isEqual(computedHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not compute PBKDF2 hash", e);
        }
    }

    public static String hashMD5(String input) {
        if (input == null) return null;
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
        if (input == null) return null;
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
