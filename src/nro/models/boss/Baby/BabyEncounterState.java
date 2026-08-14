package nro.models.boss.Baby;

import nro.models.consts.ConstMap;

/** Pure state machine for one three-form Baby encounter. */
public final class BabyEncounterState {

    public static final int FORM_COUNT = 3;
    public static final long COMPLETE_COOLDOWN_MS = 10 * 60 * 1000L;
    public static final long INCOMPLETE_TIMEOUT_MS = 15 * 60 * 1000L;

    private static final int[] SPAWN_MAP_IDS = {
        ConstMap.DOI_HOA_CUC, ConstMap.THUNG_LUNG_TRE,
        ConstMap.RUNG_NAM, ConstMap.RUNG_XUONG,
        ConstMap.DAO_KAME, ConstMap.DONG_KARIN,
        ConstMap.DOI_NAM_TIM, ConstMap.THI_TRAN_MOORI,
        ConstMap.THUNG_LUNG_NAMEC, ConstMap.THUNG_LUNG_MAIMA,
        ConstMap.VUC_MAIMA, ConstMap.DAO_GURU,
        ConstMap.DOI_HOANG, ConstMap.LANG_PLANT,
        ConstMap.RUNG_NGUYEN_SINH, ConstMap.RUNG_THONG_XAYDA,
        ConstMap.THANH_PHO_VEGETA, ConstMap.VACH_NUI_DEN,
        ConstMap.RUNG_BAMBOO, ConstMap.RUNG_DUONG_XI,
        ConstMap.NAM_KAME, ConstMap.DAO_BULONG,
        ConstMap.NUI_HOA_VANG, ConstMap.NUI_HOA_TIM,
        ConstMap.NAM_GURU, ConstMap.DONG_NAM_GURU,
        ConstMap.RUNG_CO, ConstMap.RUNG_DA,
        ConstMap.THUNG_LUNG_DEN, ConstMap.BO_VUC_DEN
    };

    public enum Action {
        NONE, START_NEXT_ROUND, RESET_INCOMPLETE_ROUND
    }

    public record DefeatResult(boolean accepted, boolean allDefeated) {

        private static final DefeatResult IGNORED = new DefeatResult(false, false);
    }

    private boolean roundActive;
    private boolean actionDispatched;
    private int defeatedCount;
    private long firstDefeatAt;
    private long nextRoundAt;

    public BabyEncounterState(long createdAt) {
        nextRoundAt = createdAt;
    }

    public static int[] spawnMapIds() {
        return SPAWN_MAP_IDS.clone();
    }

    public synchronized void startRound() {
        roundActive = true;
        actionDispatched = false;
        defeatedCount = 0;
        firstDefeatAt = 0L;
        nextRoundAt = 0L;
    }

    public synchronized DefeatResult defeat(int formIndex, long now) {
        if (!roundActive || formIndex != defeatedCount
                || formIndex < 0 || formIndex >= FORM_COUNT) {
            return DefeatResult.IGNORED;
        }

        defeatedCount++;
        if (firstDefeatAt == 0L) {
            firstDefeatAt = now;
        }
        boolean allDefeated = defeatedCount == FORM_COUNT;
        if (allDefeated) {
            nextRoundAt = now + COMPLETE_COOLDOWN_MS;
        }
        return new DefeatResult(true, allDefeated);
    }

    public synchronized Action poll(long now) {
        if (actionDispatched) {
            return Action.NONE;
        }

        Action action = Action.NONE;
        if (!roundActive && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && defeatedCount == FORM_COUNT
                && nextRoundAt > 0L && now >= nextRoundAt) {
            action = Action.START_NEXT_ROUND;
        } else if (roundActive && defeatedCount > 0 && defeatedCount < FORM_COUNT
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

    public synchronized int getDefeatedCount() {
        return defeatedCount;
    }
}
