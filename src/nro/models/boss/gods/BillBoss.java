package nro.models.boss.gods;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
public final class BillBoss extends DivinePairBoss {
    public BillBoss() throws Exception { super(BossID.GOD_BILL, BossesData.GOD_BILL, true); }
    protected String encounterName() { return "Thần Hủy Diệt Bill và Thiên Sứ Whis"; }
    protected String partnerActivationText() { return "Ngài Bill đã thất bại. Bây giờ đến lượt tôi."; }
    protected String standbyText() { return "Tôi đang quan sát trận đấu của ngài Bill."; }
}
