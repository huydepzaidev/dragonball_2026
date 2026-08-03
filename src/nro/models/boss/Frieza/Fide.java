package nro.models.boss.Frieza;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import nro.models.item.Item;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.KOLQuestService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.utils.Util;

public class Fide extends Boss {

    private long st;
    private long kolWaveKillerId = -1;

    public Fide() throws Exception {
        super(BossID.FIDE, BossesData.FIDE_DAI_CA_1, BossesData.FIDE_DAI_CA_2, BossesData.FIDE_DAI_CA_3);
    }

    @Override
    public void reward(Player plKill) {
        updateKOLWaveProgress(plKill);
        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point");
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, 190, Util.nextInt(20000, 30001),
          this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        if (Util.isTrue(80, 100)) {
            int[] items = Util.isTrue(50, 100) ? new int[]{18, 19, 20} : new int[]{18,19,20};
            int randomItem = items[new Random().nextInt(items.length)];
            Service.gI().dropItemMap(this.zone, new ItemMap(this.zone, randomItem, 1,
          this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id));
        }
    }

    @Override
    public void joinMap() {
        if (this.currentLevel == 0) {
            this.kolWaveKillerId = -1;
        }
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
        st = System.currentTimeMillis();
    }

    private void updateKOLWaveProgress(Player plKill) {
        if (plKill == null) {
            this.kolWaveKillerId = -1;
            return;
        }

        switch (this.currentLevel) {
            case 0 ->
                this.kolWaveKillerId = KOLQuestService.gI().isDoingFideWaveQuest(plKill)
                        ? plKill.id : -1;
            case 1 -> {
                if (this.kolWaveKillerId != plKill.id
                        || !KOLQuestService.gI().isDoingFideWaveQuest(plKill)) {
                    this.kolWaveKillerId = -1;
                }
            }
            case 2 -> {
                if (this.kolWaveKillerId == plKill.id) {
                    KOLQuestService.gI().recordFideWaveCompletion(plKill);
                }
                this.kolWaveKillerId = -1;
            }
            default ->
                this.kolWaveKillerId = -1;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

}
