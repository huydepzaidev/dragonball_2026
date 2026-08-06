package nro.models.services;

import java.util.List;

public final class MailboxServiceTest {

    private MailboxServiceTest() {
    }

    public static void main(String[] args) {
        List<MailboxService.MailReward> rewards = MailboxService.parseRewards(
                "[{\"id\":-1,\"quantity\":500,\"options\":[]},"
                + "{\"id\":457,\"quantity\":2,\"options\":[{\"id\":30,\"param\":0}]}]");
        assertEquals(2, rewards.size());
        assertEquals(-1, rewards.get(0).id);
        assertEquals(500, rewards.get(0).quantity);
        assertEquals(457, rewards.get(1).id);
        assertEquals(1, rewards.get(1).options.size());
        assertEquals(30, rewards.get(1).options.get(0).id);
        assertInvalid("[]");
        assertInvalid("[{\"id\":-4,\"quantity\":1,\"options\":[]}]");
        assertInvalid("[{\"id\":-1,\"quantity\":1,\"options\":[{\"id\":30,\"param\":0}]}]");
        System.out.println("MAILBOX_REWARD_PARSER_OK");
    }

    private static void assertInvalid(String json) {
        try {
            MailboxService.parseRewards(json);
            throw new AssertionError("Expected invalid rewards: " + json);
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
