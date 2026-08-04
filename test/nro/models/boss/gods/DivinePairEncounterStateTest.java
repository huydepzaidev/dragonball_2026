package nro.models.boss.gods;

import nro.models.boss.gods.DivinePairEncounterState.Action;
import nro.models.boss.gods.DivinePairEncounterState.DefeatResult;
import nro.models.boss.gods.DivinePairEncounterState.Member;

public final class DivinePairEncounterStateTest {

    private DivinePairEncounterStateTest() { }

    public static void main(String[] args) {
        testInitialAndCompleteCooldown();
        testIncompleteRoundReset();
        testExactlyOneDivineDrop();
        testDragonBallWeights();
        System.out.println("DIVINE_PAIR_ENCOUNTER_STATE_OK cooldown=10m timeout=15m dragonBalls=3/4/5/22/22/22/22");
    }

    private static void testInitialAndCompleteCooldown() {
        long createdAt = 1_000L;
        DivinePairEncounterState state = new DivinePairEncounterState(createdAt);
        assertEquals(Action.NONE, state.poll(createdAt - 1));
        assertEquals(Action.START_NEXT_ROUND, state.poll(createdAt));
        assertEquals(Action.NONE, state.poll(createdAt + 1));

        state.startRound(700_000L, Member.LEADER);
        DefeatResult first = state.defeat(Member.LEADER, 710_000L);
        DefeatResult second = state.defeat(Member.PARTNER, 720_000L);
        assertTrue(first.accepted() && first.dropDivine() && !first.bothDefeated());
        assertTrue(second.accepted() && !second.dropDivine() && second.bothDefeated());
        assertEquals(Action.NONE, state.poll(720_000L + DivinePairEncounterState.COMPLETE_COOLDOWN_MS - 1));
        assertEquals(Action.START_NEXT_ROUND,
                state.poll(720_000L + DivinePairEncounterState.COMPLETE_COOLDOWN_MS));
    }

    private static void testIncompleteRoundReset() {
        DivinePairEncounterState state = new DivinePairEncounterState(0L);
        state.startRound(1_000L, Member.PARTNER);
        state.defeat(Member.LEADER, 2_000L);
        assertEquals(Action.NONE, state.poll(2_000L + DivinePairEncounterState.INCOMPLETE_TIMEOUT_MS - 1));
        assertEquals(Action.RESET_INCOMPLETE_ROUND,
                state.poll(2_000L + DivinePairEncounterState.INCOMPLETE_TIMEOUT_MS));
        assertEquals(Action.NONE, state.poll(2_000L + DivinePairEncounterState.INCOMPLETE_TIMEOUT_MS + 1));
    }

    private static void testExactlyOneDivineDrop() {
        DivinePairEncounterState state = new DivinePairEncounterState(0L);
        state.startRound(1_000L, Member.PARTNER);
        DefeatResult leader = state.defeat(Member.LEADER, 2_000L);
        DefeatResult duplicate = state.defeat(Member.LEADER, 2_001L);
        DefeatResult partner = state.defeat(Member.PARTNER, 3_000L);
        assertTrue(leader.accepted() && !leader.dropDivine());
        assertTrue(!duplicate.accepted() && !duplicate.dropDivine());
        assertTrue(partner.accepted() && partner.dropDivine() && partner.bothDefeated());
    }

    private static void testDragonBallWeights() {
        int[] counts = new int[7];
        for (int roll = 0; roll < 100; roll++) {
            counts[DivinePairEncounterState.dragonBallItemForRoll(roll) - 14]++;
        }
        int[] expected = {3, 4, 5, 22, 22, 22, 22};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], counts[i]);
    }

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("condition was false");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
