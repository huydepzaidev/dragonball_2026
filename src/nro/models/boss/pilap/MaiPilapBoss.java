package nro.models.boss.pilap;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class MaiPilapBoss extends PilapSquadBoss {

    public MaiPilapBoss() throws Exception {
        super(BossID.MAI_PILAP, BossesData.MAI_PILAP, 1);
    }

    @Override
    protected String activationText() {
        return "Pilap đã thất bại. Bây giờ đến lượt tôi!";
    }

    @Override
    protected String standbyText() {
        return "Tôi đang chờ Pilap kết thúc trận đấu.";
    }
}
