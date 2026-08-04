package nro.models.boss.gods;

/** Pure, testable state machine for one two-member divine encounter. */
public final class DivinePairEncounterState {

    public static final long COMPLETE_COOLDOWN_MS = 10 * 60 * 1000L;
    public static final long INCOMPLETE_TIMEOUT_MS = 15 * 60 * 1000L;

    public enum Member { LEADER, PARTNER }
    public enum Action { NONE, START_NEXT_ROUND, RESET_INCOMPLETE_ROUND }

    public record DefeatResult(boolean accepted, boolean dropDivine, boolean bothDefeated) {
        private static final DefeatResult IGNORED = new DefeatResult(false, false, false);
    }

    private boolean roundActive;
    private boolean leaderDefeated;
    private boolean partnerDefeated;
    private boolean divineClaimed;
    private boolean actionDispatched;
    private Member divineMember;
    private long firstDefeatAt;
    private long nextRoundAt;

    public DivinePairEncounterState(long createdAt) {
        // Lượt đầu xuất hiện ngay khi server đã nạp xong boss. Thời gian chờ
        // 10 phút chỉ bắt đầu sau khi cả hai thành viên bị tiêu diệt.
        this.nextRoundAt = createdAt;
    }

    public synchronized void startRound(long now, Member selectedDivineMember) {
        roundActive = true;
        leaderDefeated = false;
        partnerDefeated = false;
        divineClaimed = false;
        actionDispatched = false;
        divineMember = selectedDivineMember;
        firstDefeatAt = 0L;
        nextRoundAt = 0L;
    }

    public synchronized DefeatResult defeat(Member member, long now) {
        if (!roundActive || member == null || isDefeated(member)) {
            return DefeatResult.IGNORED;
        }
        if (member == Member.LEADER) leaderDefeated = true;
        else partnerDefeated = true;
        if (firstDefeatAt == 0L) firstDefeatAt = now;

        boolean dropDivine = !divineClaimed && member == divineMember;
        if (dropDivine) divineClaimed = true;
        boolean bothDefeated = leaderDefeated && partnerDefeated;
        if (bothDefeated) nextRoundAt = now + COMPLETE_COOLDOWN_MS;
        return new DefeatResult(true, dropDivine, bothDefeated);
    }

    public synchronized Action poll(long now) {
        if (actionDispatched) return Action.NONE;
        Action action = Action.NONE;
        if (!roundActive && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && leaderDefeated && partnerDefeated
                && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && leaderDefeated != partnerDefeated
                && firstDefeatAt > 0L && now - firstDefeatAt >= INCOMPLETE_TIMEOUT_MS) {
            action = Action.RESET_INCOMPLETE_ROUND;
        }
        if (action != Action.NONE) {
            actionDispatched = true;
            roundActive = false;
        }
        return action;
    }

    private boolean isDefeated(Member member) {
        return member == Member.LEADER ? leaderDefeated : partnerDefeated;
    }

    public synchronized boolean isRoundActive() { return roundActive; }
    public synchronized long getFirstDefeatAt() { return firstDefeatAt; }
    public synchronized long getNextRoundAt() { return nextRoundAt; }

    /** Item IDs 14..20 are dragon balls 1..7 stars; stars 1..3 use 3%, 4%, 5%. */
    public static int dragonBallItemForRoll(int roll) {
        if (roll < 0 || roll >= 100) {
            throw new IllegalArgumentException("roll must be in [0, 99]");
        }
        if (roll < 3) return 14;
        if (roll < 7) return 15;
        if (roll < 12) return 16;
        return 17 + (roll - 12) / 22;
    }
}
