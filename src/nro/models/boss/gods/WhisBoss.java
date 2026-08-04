package nro.models.boss.gods;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
public final class WhisBoss extends DivinePairBoss {
    public WhisBoss() throws Exception { super(BossID.ANGEL_WHIS, BossesData.ANGEL_WHIS, false); }
    protected String encounterName() { return "Thần Hủy Diệt Bill và Thiên Sứ Whis"; }
    protected String partnerActivationText() { return "Ngài Bill đã thất bại. Bây giờ đến lượt tôi."; }
    protected String standbyText() { return "Tôi sẽ đợi ngài Bill kết thúc trận đấu."; }
}
