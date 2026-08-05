package nro.models.boss.pilap;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class PuPilapBoss extends PilapSquadBoss {

    public PuPilapBoss() throws Exception {
        super(BossID.PU_PILAP, BossesData.PU_PILAP, 2);
    }

    @Override
    protected String activationText() {
        return "Đồng đội của ta đã thất bại. Ta sẽ chiến đấu!";
    }

    @Override
    protected String standbyText() {
        return "Ta sẽ đứng đây chờ đến lượt mình.";
    }
}
