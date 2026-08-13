package nro.models.boss.MajinBuu_12h;

import java.time.LocalDate;
import nro.models.boss.Boss;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

public final class Mabu12hDivineDropService {

    private static final Mabu12hDivineDropPolicy POLICY = new Mabu12hDivineDropPolicy();

    private Mabu12hDivineDropService() {
    }

    public static void dropForTurn(Boss boss, Player killer) {
        if (boss == null || killer == null || boss.zone == null || !TimeUtil.isMabuOpen()) {
            return;
        }
        long turnId = LocalDate.now(TimeUtil.VIETNAM_ZONE).toEpochDay();
        int selectedBossIndex = Util.nextInt(Mabu12hDivineDropPolicy.bossCount());
        if (!POLICY.reserveDrop(turnId, boss.zone.zoneId, (int) boss.id, selectedBossIndex)) {
            return;
        }

        int x = boss.location.x;
        int y = boss.zone.map.yPhysicInTop(x, boss.location.y - 24);
        ItemMap item = ItemService.gI().randDoTLBoss(boss.zone, 1, x, y, killer.id);
        Service.gI().dropItemMap(boss.zone, item);
    }
}
