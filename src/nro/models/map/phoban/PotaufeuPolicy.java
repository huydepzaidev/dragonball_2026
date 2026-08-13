package nro.models.map.phoban;

import nro.models.consts.ConstTask;

public final class PotaufeuPolicy {

    public static final int MAP_ID = 140;
    public static final int REQUIRED_TASK_ID = ConstTask.TASK_22_0;
    public static final int CHALLENGE_DURATION_MS = 5 * 60 * 1000;
    public static final int HOME_CAPSULE_DELAY_MS = 60 * 1000;

    private PotaufeuPolicy() {
    }

    public static boolean canEnter(int taskId) {
        return taskId >= REQUIRED_TASK_ID;
    }

    public static boolean isAutoReturnReady(long availableAt, long now) {
        return availableAt > 0 && now >= availableAt;
    }
}
