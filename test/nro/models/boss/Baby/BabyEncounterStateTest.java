package nro.models.boss.Baby;

import java.util.Arrays;
import nro.models.boss.Baby.BabyEncounterState.Action;

public final class BabyEncounterStateTest {

    private BabyEncounterStateTest() {
    }

    public static void main(String[] args) {
        verifySpawnMaps();
        verifyCompleteRound();
        verifyIncompleteRound();
    }

    private static void verifySpawnMaps() {
        int[] expected = {
            1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13,
            15, 16, 17, 18, 19, 20, 27, 28, 29, 30,
            31, 32, 33, 34, 35, 36, 37, 38
        };
        require(Arrays.equals(expected, BabyEncounterState.spawnMapIds()));
    }

    private static void verifyCompleteRound() {
        BabyEncounterState state = startedState();
        require(state.defeat(0, 2_000L).accepted());
        require(!state.defeat(0, 2_100L).accepted());
        require(state.defeat(1, 3_000L).accepted());
        require(state.defeat(2, 4_000L).allDefeated());
        require(state.poll(4_000L + BabyEncounterState.COMPLETE_COOLDOWN_MS - 1)
                == Action.NONE);
        require(state.poll(4_000L + BabyEncounterState.COMPLETE_COOLDOWN_MS)
                == Action.START_NEXT_ROUND);
    }

    private static void verifyIncompleteRound() {
        BabyEncounterState state = startedState();
        state.defeat(0, 2_000L);
        state.defeat(1, 3_000L);
        require(state.poll(2_000L + BabyEncounterState.INCOMPLETE_TIMEOUT_MS)
                == Action.RESET_INCOMPLETE_ROUND);
    }

    private static BabyEncounterState startedState() {
        BabyEncounterState state = new BabyEncounterState(1L);
        require(state.poll(1L) == Action.START_NEXT_ROUND);
        state.startRound();
        return state;
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
