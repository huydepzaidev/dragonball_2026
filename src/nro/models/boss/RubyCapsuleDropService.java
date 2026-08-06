package nro.models.boss;

import nro.models.consts.ConstItem;
import nro.models.map.ItemMap;
import nro.models.services.Service;
import nro.models.utils.Util;

public final class RubyCapsuleDropService {

    public static final int RUBY_CAPSULE_ITEM_ID = ConstItem.CAPSULE_HONG_NGOC;
    public static final int QUANTITY_PER_DROP = 1;
    public static final long PUBLIC_DROP_OWNER_ID = -1L;

    private RubyCapsuleDropService() {
    }

    public static void dropPublicRubyCapsules(Boss boss, int dropCount) {
        if (boss == null || boss.zone == null || dropCount <= 0) {
            return;
        }
        for (int i = 0; i < dropCount; i++) {
            int x = boss.location.x + Util.nextInt(-100, 100);
            int y = boss.zone.map.yPhysicInTop(x, boss.location.y - 24);
            ItemMap capsule = new ItemMap(
                    boss.zone,
                    RUBY_CAPSULE_ITEM_ID,
                    QUANTITY_PER_DROP,
                    x,
                    y,
                    PUBLIC_DROP_OWNER_ID);
            Service.gI().dropItemMap(boss.zone, capsule);
        }
    }
}
