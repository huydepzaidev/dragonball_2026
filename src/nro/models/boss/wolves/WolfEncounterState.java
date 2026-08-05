package nro.models.boss.wolves;

/** Pure state machine for one Ba Con Soi Vo Tinh encounter. */
public final class WolfEncounterState {

    public static final int MEMBER_COUNT = 3;
    public static final int BLUE_GRAY_MEMBER = 2;
    public static final long COMPLETE_COOLDOWN_MS = 10 * 60 * 1000L;
    public static final long INCOMPLETE_TIMEOUT_MS = 15 * 60 * 1000L;

    public enum Action {
        NONE, START_NEXT_ROUND, RESET_INCOMPLETE_ROUND
    }

    public record DefeatResult(
            boolean accepted,
            boolean dropDivine,
            boolean allDefeated,
            int nextActiveMember) {

        private static final DefeatResult IGNORED
                = new DefeatResult(false, false, false, -1);
    }

    private final boolean[] defeated = new boolean[MEMBER_COUNT];
    private final int[] combatOrder = new int[MEMBER_COUNT];
    private boolean roundActive;
    private boolean divineClaimed;
    private boolean actionDispatched;
    private int activeStep;
    private int activeMember = -1;
    private int divineMember;
    private int defeatedCount;
    private long firstDefeatAt;
    private long nextRoundAt;

    public WolfEncounterState(long createdAt) {
        nextRoundAt = createdAt;
    }

    public synchronized void startRound(long now, int selectedDivineMember, int firstMember) {
        if (!isValidMember(selectedDivineMember)) {
            throw new IllegalArgumentException("selectedDivineMember must be in [0, 2]");
        }
        if (firstMember != 0 && firstMember != 1) {
            throw new IllegalArgumentException("firstMember must be the red or yellow wolf");
        }
        for (int i = 0; i < defeated.length; i++) {
            defeated[i] = false;
        }
        combatOrder[0] = firstMember;
        combatOrder[1] = 1 - firstMember;
        combatOrder[2] = BLUE_GRAY_MEMBER;
        roundActive = true;
        divineClaimed = false;
        actionDispatched = false;
        activeStep = 0;
        activeMember = combatOrder[activeStep];
        divineMember = selectedDivineMember;
        defeatedCount = 0;
        firstDefeatAt = 0L;
        nextRoundAt = 0L;
    }

    public synchronized DefeatResult defeat(int member, long now) {
        if (!roundActive || !isValidMember(member)
                || member != activeMember || defeated[member]) {
            return DefeatResult.IGNORED;
        }

        defeated[member] = true;
        defeatedCount++;
        if (firstDefeatAt == 0L) {
            firstDefeatAt = now;
        }

        boolean dropDivine = !divineClaimed && member == divineMember;
        if (dropDivine) {
            divineClaimed = true;
        }

        boolean allDefeated = defeatedCount == MEMBER_COUNT;
        if (allDefeated) {
            activeMember = -1;
            nextRoundAt = now + COMPLETE_COOLDOWN_MS;
        } else {
            activeMember = combatOrder[++activeStep];
        }
        return new DefeatResult(true, dropDivine, allDefeated, activeMember);
    }

    public synchronized Action poll(long now) {
        if (actionDispatched) {
            return Action.NONE;
        }

        Action action = Action.NONE;
        if (!roundActive && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && defeatedCount == MEMBER_COUNT
                && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && defeatedCount > 0 && defeatedCount < MEMBER_COUNT
                && firstDefeatAt > 0L
                && now - firstDefeatAt >= INCOMPLETE_TIMEOUT_MS) {
            action = Action.RESET_INCOMPLETE_ROUND;
        }

        if (action != Action.NONE) {
            actionDispatched = true;
            roundActive = false;
        }
        return action;
    }

    public synchronized int getActiveMember() {
        return activeMember;
    }

    public synchronized int getDefeatedCount() {
        return defeatedCount;
    }

    private static boolean isValidMember(int member) {
        return member >= 0 && member < MEMBER_COUNT;
    }

    /** Item IDs 14..20 are dragon balls 1..7 stars; stars 1..3 stay rare. */
    public static int dragonBallItemForRoll(int roll) {
        if (roll < 0 || roll >= 100) {
            throw new IllegalArgumentException("roll must be in [0, 99]");
        }
        if (roll < 3) {
            return 14;
        }
        if (roll < 7) {
            return 15;
        }
        if (roll < 12) {
            return 16;
        }
        return 17 + (roll - 12) / 22;
    }
}
