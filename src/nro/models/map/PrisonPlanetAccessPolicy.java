package nro.models.map;

import nro.models.consts.ConstMap;
import nro.models.consts.ConstTask;

public final class PrisonPlanetAccessPolicy {

    public static final int MAP_ID = ConstMap.HANH_TINH_NGUC_TU;
    public static final int REQUIRED_TASK_ID = ConstTask.TASK_20_0;

    private PrisonPlanetAccessPolicy() {
    }

    public static boolean canEnter(int taskId) {
        return taskId >= REQUIRED_TASK_ID;
    }
}
