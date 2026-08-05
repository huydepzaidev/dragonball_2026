package nro.models.boss.wolves;

import nro.models.boss.wolves.WolfEncounterState.Action;
import nro.models.boss.wolves.WolfEncounterState.DefeatResult;

public final class WolfEncounterStateTest {

    private WolfEncounterStateTest() {
    }

    public static void main(String[] args) {
        testInitialSpawnTrigger();
        testRedOrYellowCanFightFirstAndBlueGrayAlwaysFightsLast();
        testCompleteCooldown();
        testIncompleteRoundResetAfterOneOrTwoDefeats();
        testExactlyOneDivineDrop();
        testDragonBallWeights();
        testOneHpPerHitLimit();
        System.out.println("WOLF_ENCOUNTER_STATE_OK members=3 cooldown=10m timeout=15m divine=1/3");
    }

    private static void testInitialSpawnTrigger() {
        WolfEncounterState state = new WolfEncounterState(1_000L);
        assertEquals(Action.NONE, state.poll(999L));
        assertEquals(Action.START_NEXT_ROUND, state.poll(1_000L));
        assertEquals(Action.NONE, state.poll(1_001L));
    }

    private static void testRedOrYellowCanFightFirstAndBlueGrayAlwaysFightsLast() {
        for (int first = 0; first <= 1; first++) {
            WolfEncounterState state = new WolfEncounterState(1L);
            state.startRound(1_000L, 2, first);
            assertEquals(first, state.getActiveMember());
            assertFalse(state.defeat(1 - first, 1_100L).accepted());
            assertEquals(1 - first, state.defeat(first, 1_200L).nextActiveMember());
            assertEquals(2, state.defeat(1 - first, 1_300L).nextActiveMember());
        }
    }

    private static void testCompleteCooldown() {
        WolfEncounterState state = completedState(4_000L);
        assertEquals(Action.NONE,
                state.poll(4_000L + WolfEncounterState.COMPLETE_COOLDOWN_MS - 1));
        assertEquals(Action.START_NEXT_ROUND,
                state.poll(4_000L + WolfEncounterState.COMPLETE_COOLDOWN_MS));
    }

    private static void testIncompleteRoundResetAfterOneOrTwoDefeats() {
        WolfEncounterState oneDefeat = new WolfEncounterState(1L);
        oneDefeat.startRound(1_000L, 0, 0);
        oneDefeat.defeat(0, 2_000L);
        assertEquals(Action.NONE,
                oneDefeat.poll(2_000L + WolfEncounterState.INCOMPLETE_TIMEOUT_MS - 1));
        assertEquals(Action.RESET_INCOMPLETE_ROUND,
                oneDefeat.poll(2_000L + WolfEncounterState.INCOMPLETE_TIMEOUT_MS));

        WolfEncounterState twoDefeats = new WolfEncounterState(1L);
        twoDefeats.startRound(1_000L, 0, 0);
        twoDefeats.defeat(0, 2_000L);
        twoDefeats.defeat(1, 3_000L);
        assertEquals(Action.RESET_INCOMPLETE_ROUND,
                twoDefeats.poll(2_000L + WolfEncounterState.INCOMPLETE_TIMEOUT_MS));
    }

    private static void testExactlyOneDivineDrop() {
        for (int selected = 0; selected < WolfEncounterState.MEMBER_COUNT; selected++) {
            WolfEncounterState state = new WolfEncounterState(1L);
            state.startRound(1_000L, selected, 0);
            int drops = 0;
            for (int member = 0; member < WolfEncounterState.MEMBER_COUNT; member++) {
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
            counts[WolfEncounterState.dragonBallItemForRoll(roll) - 14]++;
        }
        int[] expected = {3, 4, 5, 22, 22, 22, 22};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], counts[i]);
        }
    }

    private static void testOneHpPerHitLimit() {
        assertEquals(0L, WolfBoss.limitedDamage(0));
        assertEquals(1L, WolfBoss.limitedDamage(1));
        assertEquals(1L, WolfBoss.limitedDamage(Long.MAX_VALUE));
    }

    private static WolfEncounterState completedState(long lastDefeatAt) {
        WolfEncounterState state = new WolfEncounterState(1L);
        state.startRound(1_000L, 1, 0);
        state.defeat(0, 2_000L);
        state.defeat(1, 3_000L);
        DefeatResult result = state.defeat(2, lastDefeatAt);
        assertTrue(result.accepted() && result.allDefeated());
        return state;
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
