package nro.models.boss.trai_dat;

import nro.models.boss.Boss;
import nro.models.boss.RubyCapsuleDropService;

final class BojackSquadRewardService {

    static final int RUBY_CAPSULE_ITEM_ID = RubyCapsuleDropService.RUBY_CAPSULE_ITEM_ID;
    static final int RUBY_CAPSULE_DROPS_PER_BOSS = 10;
    static final int RUBY_CAPSULE_QUANTITY_PER_DROP = RubyCapsuleDropService.QUANTITY_PER_DROP;
    static final long PUBLIC_DROP_OWNER_ID = RubyCapsuleDropService.PUBLIC_DROP_OWNER_ID;

    private BojackSquadRewardService() {
    }

    static void dropPublicRubyCapsules(Boss boss) {
        RubyCapsuleDropService.dropPublicRubyCapsules(boss, RUBY_CAPSULE_DROPS_PER_BOSS);
    }
}
