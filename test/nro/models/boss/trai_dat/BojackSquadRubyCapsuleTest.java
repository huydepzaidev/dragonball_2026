package nro.models.boss.trai_dat;

public final class BojackSquadRubyCapsuleTest {

    private BojackSquadRubyCapsuleTest() {
    }

    public static void main(String[] args) {
        require(BojackSquadRewardService.RUBY_CAPSULE_ITEM_ID == 2005);
        require(BojackSquadRewardService.RUBY_CAPSULE_DROPS_PER_BOSS == 10);
        require(BojackSquadRewardService.RUBY_CAPSULE_QUANTITY_PER_DROP == 1);
        require(BojackSquadRewardService.PUBLIC_DROP_OWNER_ID == -1L);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
