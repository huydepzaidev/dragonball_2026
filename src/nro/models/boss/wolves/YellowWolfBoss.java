package nro.models.boss.wolves;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class YellowWolfBoss extends WolfBoss {

    public YellowWolfBoss() throws Exception {
        super(BossID.SOI_VANG_VO_TINH, BossesData.SOI_VANG_VO_TINH, 1);
    }

    @Override
    protected String activationText() {
        return "Đến lượt Sói Vàng chiến đấu!";
    }

    @Override
    protected String standbyText() {
        return "Ta đang quan sát trận chiến này.";
    }
}
