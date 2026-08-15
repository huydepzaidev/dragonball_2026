package nro.models.services_dungeon;

import nro.models.boss.BossID;
import nro.models.consts.ConstNpc;

public final class TrainingServiceNpcLookupTest {

    private TrainingServiceNpcLookupTest() {
    }

    public static void main(String[] args) {
        TrainingService service = TrainingService.gI();

        if (service.getNonInteractiveNPC(null, BossID.WHIS) != null) {
            throw new AssertionError("A null zone must return no non-interactive NPC");
        }
        if (service.getNpc(BossID.WHIS) != ConstNpc.WHIS) {
            throw new AssertionError("Whis boss must map back to the Whis NPC");
        }

        System.out.println("TrainingServiceNpcLookupTest: OK");
    }
}
