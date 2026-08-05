package nro.models.boss.pilap;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class PilapBoss extends PilapSquadBoss {

    public PilapBoss() throws Exception {
        super(BossID.PILAP, BossesData.PILAP, 0);
    }

    @Override
    protected String activationText() {
        return "Ta sẽ mở màn cho Tiểu Đội Pilap!";
    }

    @Override
    protected String standbyText() {
        return "Mai và Pu, hãy xem ta xử lý bọn chúng!";
    }
}
