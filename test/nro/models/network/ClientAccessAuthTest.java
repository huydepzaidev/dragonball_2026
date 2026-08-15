package nro.models.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import nro.models.server.Manager;

public final class ClientAccessAuthTest {

    private static final String SECRET = "test-client-access-secret";
    private static final String BUILD_ID = "NROVEGETA-TEST";

    private ClientAccessAuthTest() {
    }

    public static void main(String[] args) throws Exception {
        Manager.CLIENT_ACCESS_SECRET = SECRET;
        Manager.CLIENT_ACCESS_BUILD = BUILD_ID;
        Manager.CLIENT_ACCESS_MAX_CLOCK_SKEW_SECONDS = 30;

        testValidProof();
        testReplayIsRejected();
        testWrongSignature();
        testExpiredTimestamp();
        testLegacyEmptyHandshake();
        testOversizedHandshake();
        System.out.println("CLIENT_ACCESS_AUTH_TEST_OK");
    }

    private static void testValidProof() throws Exception {
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        Message message = message(timestamp, nonce(1), false);
        check(ClientAccessAuth.validateProof(message) == null, "Valid proof must pass");
        message.cleanup();
    }

    private static void testReplayIsRejected() throws Exception {
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        byte[] nonce = nonce(2);
        Message first = message(timestamp, nonce, false);
        check(ClientAccessAuth.validateProof(first) == null, "First nonce use must pass");
        first.cleanup();

        Message replay = message(timestamp, nonce, false);
        check("replayed handshake nonce".equals(ClientAccessAuth.validateProof(replay)),
                "Replayed nonce must fail");
        replay.cleanup();
    }

    private static void testWrongSignature() throws Exception {
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        Message message = message(timestamp, nonce(3), true);
        check("invalid access proof".equals(ClientAccessAuth.validateProof(message)),
                "Wrong signature must fail");
        message.cleanup();
    }

    private static void testExpiredTimestamp() throws Exception {
        int timestamp = (int) (System.currentTimeMillis() / 1000L) - 31;
        Message message = message(timestamp, nonce(4), false);
        check("expired handshake timestamp".equals(ClientAccessAuth.validateProof(message)),
                "Expired timestamp must fail");
        message.cleanup();
    }

    private static void testLegacyEmptyHandshake() {
        Message message = new Message((byte) -27, new byte[0]);
        check("invalid handshake payload length".equals(ClientAccessAuth.validateProof(message)),
                "Empty legacy -27 must fail validation");
        message.cleanup();
    }

    private static void testOversizedHandshake() {
        Message message = new Message((byte) -27, new byte[257]);
        check("invalid handshake payload length".equals(ClientAccessAuth.validateProof(message)),
                "Oversized -27 must fail validation");
        message.cleanup();
    }

    private static Message message(int timestamp, byte[] nonce, boolean corruptSignature) throws Exception {
        String nonceHex = toHex(nonce);
        String canonical = "1\n" + BUILD_ID + "\n" + timestamp + "\n" + nonceHex;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        if (corruptSignature) {
            signature[0] ^= 0x01;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(1);
            output.writeUTF(BUILD_ID);
            output.writeInt(timestamp);
            output.writeByte(nonce.length);
            output.write(nonce);
            output.writeByte(signature.length);
            output.write(signature);
        }
        return new Message((byte) -27, bytes.toByteArray());
    }

    private static byte[] nonce(int seed) {
        byte[] value = new byte[16];
        for (int i = 0; i < value.length; i++) {
            value[i] = (byte) (seed + i);
        }
        return value;
    }

    private static String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte current : value) {
            result.append(String.format("%02x", current & 0xFF));
        }
        return result.toString();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
