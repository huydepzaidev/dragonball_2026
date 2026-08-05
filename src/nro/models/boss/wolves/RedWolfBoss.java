package nro.models.boss.wolves;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class RedWolfBoss extends WolfBoss {

    public RedWolfBoss() throws Exception {
        super(BossID.SOI_DO_VO_TINH, BossesData.SOI_DO_VO_TINH, 0);
    }

    @Override
    protected String activationText() {
        return "Đến lượt Sói Đỏ chiến đấu!";
    }

    @Override
    protected String standbyText() {
        return "Ta sẽ chờ đồng đội kết thúc trận đấu.";
    }
}
