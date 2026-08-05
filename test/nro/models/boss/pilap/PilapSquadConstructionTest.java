package nro.models.boss.pilap;

import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.BossManager;

public final class PilapSquadConstructionTest {

    private PilapSquadConstructionTest() {
    }

    public static void main(String[] args) {
        BossManager manager = BossManager.gI();
        int before = manager.getBosses().size();
        Boss leader = manager.createBoss(BossID.PILAP);
        if (!(leader instanceof PilapBoss)) {
            throw new AssertionError("BossManager did not create Pilap leader");
        }
        if (manager.getBosses().size() != before + 3) {
            throw new AssertionError("Pilap encounter must create exactly 3 bosses");
        }
        if (leader.bossAppearTogether == null
                || leader.bossAppearTogether.length != 1
                || leader.bossAppearTogether[0] == null
                || leader.bossAppearTogether[0].length != 2
                || leader.bossAppearTogether[0][0].id != BossID.MAI_PILAP
                || leader.bossAppearTogether[0][1].id != BossID.PU_PILAP) {
            throw new AssertionError("Pilap/Mai/Pu relationship is invalid");
        }
        System.out.println("PILAP_SQUAD_CONSTRUCTION_OK managerMembers=3 leader=Pilap partners=Mai/Pu");
    }
}
