package nro.models.boss.gods;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
public final class ChampaBoss extends DivinePairBoss {
    public ChampaBoss() throws Exception { super(BossID.GOD_CHAMPA, BossesData.GOD_CHAMPA, true); }
    protected String encounterName() { return "Thần Hủy Diệt Champa và Thiên Sứ Vados"; }
    protected String partnerActivationText() { return "Ngài Champa đã thất bại. Tôi sẽ tiếp tục trận đấu."; }
    protected String standbyText() { return "Xin ngài Champa đừng kéo dài trận đấu."; }
}
