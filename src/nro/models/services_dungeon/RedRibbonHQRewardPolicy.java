package nro.models.services_dungeon;

import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.Service;

/** Controls rewards that depend on client resources not yet installed safely. */
public final class RedRibbonHQRewardPolicy {

    public static final int CAU_VANG_ITEM_ID = 1824;

    private RedRibbonHQRewardPolicy() {
    }

    public static boolean isCauVangEnabled() {
        return Manager.RED_RIBBON_CAU_VANG_ENABLED;
    }

    public static void dropCauVang(Zone zone, Player owner, int x, int y) {
        if (!isCauVangEnabled()) {
            return;
        }
        ItemMap item = new ItemMap(zone, CAU_VANG_ITEM_ID, 1, x, y, owner.id);
        Service.gI().dropItemMap(zone, item);
    }
}
