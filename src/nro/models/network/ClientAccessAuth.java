package nro.models.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import nro.models.server.Manager;
import nro.models.utils.Logger;

/**
 * Verifies the access proof carried by the unencrypted -27 handshake packet.
 * The shared secret itself is never sent over the socket.
 */
public final class ClientAccessAuth {

    private static final int PROTOCOL_VERSION = 1;
    private static final int NONCE_LENGTH = 16;
    private static final int SIGNATURE_LENGTH = 32;
    private static final int MAX_PAYLOAD_LENGTH = 256;
    private static final Map<String, Long> USED_NONCES = new ConcurrentHashMap<>();

    private ClientAccessAuth() {
    }

    public static boolean authenticate(MySession session, Message message) {
        Mode mode = Mode.from(Manager.CLIENT_ACCESS_MODE);
        if (mode == Mode.OFF) {
            markVerified(session, "access-check-off");
            return true;
        }

        String rejectionReason = validateProof(message);
        if (rejectionReason == null) {
            markVerified(session, Manager.CLIENT_ACCESS_BUILD);
            return true;
        }

        session.accessFailures++;
        Logger.warning("[CLIENT ACCESS] " + session.ipAddress + " rejected: "
                + rejectionReason + " (mode=" + mode.name().toLowerCase() + ")\n");

        if (mode == Mode.OBSERVE) {
            markVerified(session, "legacy-or-invalid");
            return true;
        }
        return false;
    }

    static String validateProof(Message message) {
        try {
            if (Manager.CLIENT_ACCESS_SECRET == null || Manager.CLIENT_ACCESS_SECRET.isEmpty()) {
                return "server secret is not configured";
            }
            int available = message.reader().available();
            if (available <= 0 || available > MAX_PAYLOAD_LENGTH) {
                return "invalid handshake payload length";
            }

            int protocolVersion = message.reader().readUnsignedByte();
            String buildId = message.reader().readUTF();
            int timestamp = message.reader().readInt();

            int nonceLength = message.reader().readUnsignedByte();
            if (nonceLength != NONCE_LENGTH) {
                return "invalid nonce length";
            }
            byte[] nonce = new byte[nonceLength];
            message.reader().readFully(nonce);

            int signatureLength = message.reader().readUnsignedByte();
            if (signatureLength != SIGNATURE_LENGTH) {
                return "invalid signature length";
            }
            byte[] signature = new byte[signatureLength];
            message.reader().readFully(signature);

            if (message.reader().available() != 0) {
                return "unexpected handshake data";
            }
            if (protocolVersion != PROTOCOL_VERSION) {
                return "unsupported protocol version";
            }
            if (buildId == null || buildId.length() > 64
                    || !Manager.CLIENT_ACCESS_BUILD.equals(buildId)) {
                return "unsupported client build";
            }

            long nowSeconds = System.currentTimeMillis() / 1000L;
            long skew = Math.abs(nowSeconds - (long) timestamp);
            if (skew > Manager.CLIENT_ACCESS_MAX_CLOCK_SKEW_SECONDS) {
                return "expired handshake timestamp";
            }

            String nonceHex = toHex(nonce);
            String canonical = protocolVersion + "\n" + buildId + "\n"
                    + timestamp + "\n" + nonceHex;
            byte[] expectedSignature = hmacSha256(Manager.CLIENT_ACCESS_SECRET, canonical);
            if (!MessageDigest.isEqual(expectedSignature, signature)) {
                return "invalid access proof";
            }

            removeExpiredNonces(nowSeconds);
            String replayKey = buildId + ':' + nonceHex;
            if (USED_NONCES.putIfAbsent(replayKey, nowSeconds) != null) {
                return "replayed handshake nonce";
            }

            return null;
        } catch (Exception exception) {
            return "malformed handshake payload";
        }
    }

    private static byte[] hmacSha256(String secret, String canonical) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static void markVerified(MySession session, String buildId) {
        session.accessVerified = true;
        session.accessBuild = buildId;
        session.accessVerifiedAt = System.currentTimeMillis();
    }

    private static void removeExpiredNonces(long nowSeconds) {
        long lifetime = Math.max(60L, Manager.CLIENT_ACCESS_MAX_CLOCK_SKEW_SECONDS * 2L);
        long cutoff = nowSeconds - lifetime;
        for (Map.Entry<String, Long> entry : USED_NONCES.entrySet()) {
            if (entry.getValue() < cutoff) {
                USED_NONCES.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static String toHex(byte[] value) {
        char[] hex = new char[value.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < value.length; i++) {
            int current = value[i] & 0xFF;
            hex[i * 2] = alphabet[current >>> 4];
            hex[i * 2 + 1] = alphabet[current & 0x0F];
        }
        return new String(hex);
    }

    private enum Mode {
        OFF,
        OBSERVE,
        ENFORCE;

        private static Mode from(String value) {
            if (value == null) {
                return ENFORCE;
            }
            switch (value.trim().toLowerCase()) {
                case "off":
                    return OFF;
                case "observe":
                    return OBSERVE;
                default:
                    return ENFORCE;
            }
        }
    }
}
