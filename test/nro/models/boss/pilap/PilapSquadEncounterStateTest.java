package nro.models.boss.pilap;

import nro.models.boss.pilap.PilapSquadEncounterState.Action;
import nro.models.boss.pilap.PilapSquadEncounterState.DefeatResult;

public final class PilapSquadEncounterStateTest {

    private PilapSquadEncounterStateTest() {
    }

    public static void main(String[] args) {
        testInitialSpawnAndSequentialCombat();
        testCompleteCooldown();
        testIncompleteRoundResetAfterOneOrTwoDefeats();
        testExactlyOneDivineDropForEverySelection();
        testDragonBallWeights();
        System.out.println("PILAP_SQUAD_STATE_OK members=3 cooldown=10m timeout=15m divine=1/3");
    }

    private static void testInitialSpawnAndSequentialCombat() {
        long createdAt = 1_000L;
        PilapSquadEncounterState state = new PilapSquadEncounterState(createdAt);
        assertEquals(Action.NONE, state.poll(createdAt - 1));
        assertEquals(Action.START_NEXT_ROUND, state.poll(createdAt));
        assertEquals(Action.NONE, state.poll(createdAt + 1));

        state.startRound(2_000L, 2);
        assertEquals(0, state.getActiveMember());
        assertFalse(state.defeat(1, 2_100L).accepted());
        assertEquals(1, state.defeat(0, 2_200L).nextActiveMember());
        assertFalse(state.defeat(2, 2_300L).accepted());
        assertEquals(2, state.defeat(1, 2_400L).nextActiveMember());
    }

    private static void testCompleteCooldown() {
        PilapSquadEncounterState state = new PilapSquadEncounterState(1L);
        state.startRound(1_000L, 1);
        state.defeat(0, 2_000L);
        state.defeat(1, 3_000L);
        DefeatResult last = state.defeat(2, 4_000L);
        assertTrue(last.accepted() && last.allDefeated());
        assertEquals(Action.NONE,
                state.poll(4_000L + PilapSquadEncounterState.COMPLETE_COOLDOWN_MS - 1));
        assertEquals(Action.START_NEXT_ROUND,
                state.poll(4_000L + PilapSquadEncounterState.COMPLETE_COOLDOWN_MS));
    }

    private static void testIncompleteRoundResetAfterOneOrTwoDefeats() {
        PilapSquadEncounterState oneDefeat = new PilapSquadEncounterState(1L);
        oneDefeat.startRound(1_000L, 0);
        oneDefeat.defeat(0, 2_000L);
        assertEquals(Action.NONE,
                oneDefeat.poll(2_000L + PilapSquadEncounterState.INCOMPLETE_TIMEOUT_MS - 1));
        assertEquals(Action.RESET_INCOMPLETE_ROUND,
                oneDefeat.poll(2_000L + PilapSquadEncounterState.INCOMPLETE_TIMEOUT_MS));

        PilapSquadEncounterState twoDefeats = new PilapSquadEncounterState(1L);
        twoDefeats.startRound(1_000L, 0);
        twoDefeats.defeat(0, 2_000L);
        twoDefeats.defeat(1, 3_000L);
        assertEquals(Action.RESET_INCOMPLETE_ROUND,
                twoDefeats.poll(2_000L + PilapSquadEncounterState.INCOMPLETE_TIMEOUT_MS));
    }

    private static void testExactlyOneDivineDropForEverySelection() {
        for (int selected = 0; selected < PilapSquadEncounterState.MEMBER_COUNT; selected++) {
            PilapSquadEncounterState state = new PilapSquadEncounterState(1L);
            state.startRound(1_000L, selected);
            int drops = 0;
            for (int member = 0; member < PilapSquadEncounterState.MEMBER_COUNT; member++) {
                if (state.defeat(member, 2_000L + member).dropDivine()) {
                    drops++;
                }
            }
            assertEquals(1, drops);
        }
    }

    private static void testDragonBallWeights() {
        int[] counts = new int[7];
        for (int roll = 0; roll < 100; roll++) {
            counts[PilapSquadEncounterState.dragonBallItemForRoll(roll) - 14]++;
        }
        int[] expected = {3, 4, 5, 22, 22, 22, 22};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], counts[i]);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("condition was false");
        }
    }

    private static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
