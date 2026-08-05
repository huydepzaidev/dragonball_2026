package nro.models.boss.wolves;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

public final class BlueGrayWolfBoss extends WolfBoss {

    public BlueGrayWolfBoss() throws Exception {
        super(BossID.SOI_XANH_XAM_VO_TINH, BossesData.SOI_XANH_XAM_VO_TINH, 2);
    }

    @Override
    protected String activationText() {
        return "Sức mạnh của hai đồng đội sẽ khiến ta càng đánh càng mạnh!";
    }

    @Override
    protected String standbyText() {
        return "Chỉ khi hai đồng đội thất bại ta mới ra tay.";
    }
}
