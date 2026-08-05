package nro.models.boss.pilap;

/** Pure state machine for one three-member Pilap Squad encounter. */
public final class PilapSquadEncounterState {

    public static final int MEMBER_COUNT = 3;
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
    private boolean roundActive;
    private boolean divineClaimed;
    private boolean actionDispatched;
    private int activeMember;
    private int divineMember;
    private int defeatedCount;
    private long firstDefeatAt;
    private long nextRoundAt;

    public PilapSquadEncounterState(long createdAt) {
        nextRoundAt = createdAt;
    }

    public synchronized void startRound(long now, int selectedDivineMember) {
        if (!isValidMember(selectedDivineMember)) {
            throw new IllegalArgumentException("selectedDivineMember must be in [0, 2]");
        }
        for (int i = 0; i < defeated.length; i++) {
            defeated[i] = false;
        }
        roundActive = true;
        divineClaimed = false;
        actionDispatched = false;
        activeMember = 0;
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
        activeMember = allDefeated ? -1 : findNextActiveMember(member + 1);
        if (allDefeated) {
            nextRoundAt = now + COMPLETE_COOLDOWN_MS;
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

    public synchronized boolean isRoundActive() {
        return roundActive;
    }

    public synchronized int getActiveMember() {
        return activeMember;
    }

    public synchronized int getDefeatedCount() {
        return defeatedCount;
    }

    private int findNextActiveMember(int start) {
        for (int member = start; member < MEMBER_COUNT; member++) {
            if (!defeated[member]) {
                return member;
            }
        }
        return -1;
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
