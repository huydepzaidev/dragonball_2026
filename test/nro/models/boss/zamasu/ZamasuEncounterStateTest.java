package nro.models.boss.zamasu;

import nro.models.boss.zamasu.ZamasuEncounterState.Action;
import nro.models.boss.zamasu.ZamasuEncounterState.DefeatResult;

public final class ZamasuEncounterStateTest {

    private ZamasuEncounterStateTest() {
    }

    public static void main(String[] args) {
        testInitialSpawnAndCompleteCooldown();
        testAliveRoundReset();
        testExactlyOneDivineDropPerTwoDefeats();
        testDragonBallWeights();
        System.out.println("ZAMASU_ENCOUNTER_STATE_OK cooldown=10m aliveTimeout=15m divine=1/2");
    }

    private static void testInitialSpawnAndCompleteCooldown() {
        ZamasuEncounterState state = new ZamasuEncounterState(1_000L, false);
        assertEquals(Action.NONE, state.poll(999L));
        assertEquals(Action.START_NEXT_ROUND, state.poll(1_000L));
        assertEquals(Action.NONE, state.poll(1_001L));

        state.startRound(2_000L);
        DefeatResult result = state.defeat(3_000L);
        assertTrue(result.accepted());
        assertEquals(Action.NONE,
                state.poll(3_000L + ZamasuEncounterState.COMPLETE_COOLDOWN_MS - 1));
        assertEquals(Action.START_NEXT_ROUND,
                state.poll(3_000L + ZamasuEncounterState.COMPLETE_COOLDOWN_MS));
    }

    private static void testAliveRoundReset() {
        ZamasuEncounterState state = new ZamasuEncounterState(0L, false);
        state.startRound(10_000L);
        assertEquals(Action.NONE,
                state.poll(10_000L + ZamasuEncounterState.ALIVE_TIMEOUT_MS - 1));
        assertEquals(Action.RESET_ALIVE_ROUND,
                state.poll(10_000L + ZamasuEncounterState.ALIVE_TIMEOUT_MS));
        assertEquals(Action.NONE,
                state.poll(10_000L + ZamasuEncounterState.ALIVE_TIMEOUT_MS + 1));
    }

    private static void testExactlyOneDivineDropPerTwoDefeats() {
        for (boolean firstDrops : new boolean[]{false, true}) {
            ZamasuEncounterState state = new ZamasuEncounterState(0L, firstDrops);
            int drops = 0;
            for (int round = 0; round < 4; round++) {
                state.startRound(1_000L + round * 10_000L);
                DefeatResult result = state.defeat(2_000L + round * 10_000L);
                assertTrue(result.accepted());
                if (result.dropDivine()) {
                    drops++;
                }
                assertFalse(state.defeat(2_001L + round * 10_000L).accepted());
            }
            assertEquals(2, drops);
            assertEquals(4, state.getCompletedDefeats());
        }
    }

    private static void testDragonBallWeights() {
        int[] counts = new int[7];
        for (int roll = 0; roll < 100; roll++) {
            counts[ZamasuEncounterState.dragonBallItemForRoll(roll) - 14]++;
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