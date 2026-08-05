package nro.models.boss.zamasu;

/** Pure state machine for the recurring single-Zamasu encounter. */
public final class ZamasuEncounterState {

    public static final long COMPLETE_COOLDOWN_MS = 10 * 60 * 1000L;
    public static final long ALIVE_TIMEOUT_MS = 15 * 60 * 1000L;

    public enum Action {
        NONE, START_NEXT_ROUND, RESET_ALIVE_ROUND
    }

    public record DefeatResult(boolean accepted, boolean dropDivine) {
        private static final DefeatResult IGNORED = new DefeatResult(false, false);
    }

    private boolean roundActive;
    private boolean defeated;
    private boolean actionDispatched;
    private boolean nextDefeatDropsDivine;
    private int completedDefeats;
    private long roundStartedAt;
    private long nextRoundAt;

    public ZamasuEncounterState(long createdAt, boolean firstDefeatDropsDivine) {
        this.nextRoundAt = createdAt;
        this.nextDefeatDropsDivine = firstDefeatDropsDivine;
    }

    public synchronized void startRound(long now) {
        roundActive = true;
        defeated = false;
        actionDispatched = false;
        roundStartedAt = now;
        nextRoundAt = 0L;
    }

    public synchronized DefeatResult defeat(long now) {
        if (!roundActive || defeated) {
            return DefeatResult.IGNORED;
        }
        defeated = true;
        roundActive = false;
        completedDefeats++;
        boolean dropDivine = nextDefeatDropsDivine;
        nextDefeatDropsDivine = !nextDefeatDropsDivine;
        nextRoundAt = now + COMPLETE_COOLDOWN_MS;
        return new DefeatResult(true, dropDivine);
    }

    public synchronized Action poll(long now) {
        if (actionDispatched) {
            return Action.NONE;
        }
        Action action = Action.NONE;
        if (!roundActive && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && !defeated
                && now - roundStartedAt >= ALIVE_TIMEOUT_MS) {
            action = Action.RESET_ALIVE_ROUND;
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

    public synchronized int getCompletedDefeats() {
        return completedDefeats;
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