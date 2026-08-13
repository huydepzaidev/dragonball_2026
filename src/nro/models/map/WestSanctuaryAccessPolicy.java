package nro.models.map;

import nro.models.consts.ConstTask;

public final class WestSanctuaryAccessPolicy {

    public static final int MAP_ID = 156;
    public static final int REQUIRED_TASK_ID = ConstTask.TASK_22_0;

    private WestSanctuaryAccessPolicy() {
    }

    public static boolean canEnter(int taskId) {
        return taskId >= REQUIRED_TASK_ID;
    }
}
