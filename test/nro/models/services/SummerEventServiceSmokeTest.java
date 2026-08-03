package nro.models.services;

import nro.models.consts.ConstItem;
import nro.models.item.ItemTime;

public final class SummerEventServiceSmokeTest {

    private SummerEventServiceSmokeTest() {
    }

    public static void main(String[] args) {
        if (SummerEventService.getBeachShortsId(0) != ConstItem.QUAN_DI_BIEN
                || SummerEventService.getBeachShortsId(1) != ConstItem.QUAN_DI_BIEN_692
                || SummerEventService.getBeachShortsId(2) != ConstItem.QUAN_DI_BIEN_693) {
            throw new AssertionError("Beach shorts do not match player gender");
        }

        if (SummerEventService.getMaterialDropRate(false) != 100
                || SummerEventService.getMaterialDropRate(true) != 170) {
            throw new AssertionError("Summer buffs must increase the material drop rate by exactly 70%");
        }

        if (!SummerEventService.containsWaterTile(3, new int[][]{{0, 31}})
                || SummerEventService.containsWaterTile(3, new int[][]{{0, 1, 2}})) {
            throw new AssertionError("Unexpected water-map detection");
        }

        if (!SummerEventService.isSummerMaterial(ConstItem.VO_OC)
                || !SummerEventService.isSummerMaterial(ConstItem.SAO_BIEN)
                || SummerEventService.isSummerMaterial(ConstItem.TRAI_DUA)
                || SummerEventService.SUMMER_MATERIAL_OPTION_ID != 251) {
            throw new AssertionError("Summer materials must use option 251");
        }

        ItemTime itemTime = new ItemTime(null);
        long now = 1_000_000L;
        for (int i = 0; i < 20; i++) {
            itemTime.addTraiDuaTime(now);
        }
        if (itemTime.getRemainingTraiDuaTime(now) != ItemTime.MAX_TIME_TRAI_DUA) {
            throw new AssertionError("Coconut duration must be capped at 500 minutes");
        }
        if (itemTime.addTraiDuaTime(now) != 0) {
            throw new AssertionError("A coconut must not be consumed when the duration is already capped");
        }

        ItemTime luckyGrassTime = new ItemTime(null);
        luckyGrassTime.isUseCoBonLa = true;
        luckyGrassTime.lastTimeUseCoBonLa = System.currentTimeMillis();
        luckyGrassTime.update();
        if (!luckyGrassTime.isUseCoBonLa) {
            throw new AssertionError("Four-leaf clover must remain active for 30 minutes");
        }

        long elapsed = 10 * 60 * 1000L;
        if (itemTime.getRemainingTraiDuaTime(now + elapsed) != ItemTime.MAX_TIME_TRAI_DUA - elapsed) {
            throw new AssertionError("Coconut duration does not count down correctly");
        }

        System.out.println("SUMMER_EVENT_SMOKE_TEST_OK");
    }
}
