package nro.models.map;

import nro.models.consts.ConstMap;
import nro.models.consts.ConstTask;

public final class PrisonPlanetAccessPolicyTest {

    private PrisonPlanetAccessPolicyTest() {
    }

    public static void main(String[] args) {
        ok(PrisonPlanetAccessPolicy.MAP_ID == ConstMap.HANH_TINH_NGUC_TU);
        ok(!PrisonPlanetAccessPolicy.canEnter(ConstTask.TASK_19_3));
        ok(PrisonPlanetAccessPolicy.canEnter(ConstTask.TASK_20_0));
        ok(PrisonPlanetAccessPolicy.canEnter(ConstTask.TASK_20_1));
        System.out.println("PRISON_PLANET_ACCESS_POLICY_OK map=155 task=20");
    }

    private static void ok(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
