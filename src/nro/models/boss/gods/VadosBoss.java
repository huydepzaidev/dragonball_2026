package nro.models.boss.gods;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
public final class VadosBoss extends DivinePairBoss {
    public VadosBoss() throws Exception { super(BossID.ANGEL_VADOS, BossesData.ANGEL_VADOS, false); }
    protected String encounterName() { return "Thần Hủy Diệt Champa và Thiên Sứ Vados"; }
    protected String partnerActivationText() { return "Ngài Champa đã thất bại. Tôi sẽ tiếp tục trận đấu."; }
    protected String standbyText() { return "Tôi sẽ quan sát ngài Champa chiến đấu trước."; }
}
