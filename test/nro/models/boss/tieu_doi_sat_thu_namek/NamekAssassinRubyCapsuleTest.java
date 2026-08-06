package nro.models.boss.tieu_doi_sat_thu_namek;

public final class NamekAssassinRubyCapsuleTest {

    private NamekAssassinRubyCapsuleTest() {
    }

    public static void main(String[] args) {
        require(NamekAssassinRewardService.RUBY_CAPSULE_ITEM_ID == 2005);
        require(NamekAssassinRewardService.RUBY_CAPSULE_DROPS_PER_BOSS == 10);
        require(NamekAssassinRewardService.RUBY_CAPSULE_QUANTITY_PER_DROP == 1);
        require(NamekAssassinRewardService.RUBY_CAPSULE_DROPS_PER_BOSS
                * NamekAssassinRewardService.RUBY_CAPSULE_QUANTITY_PER_DROP == 10);
        require(NamekAssassinRewardService.PUBLIC_DROP_OWNER_ID == -1L);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
