package nro.models.boss;

import java.util.Arrays;

public final class BalancedBossHpTest {

    private BalancedBossHpTest() {
    }

    public static void main(String[] args) {
        hp(BossesData.KUKU, 50_000_000);
        hp(BossesData.MAP_DAU_DINH, 100_000_000);
        hp(BossesData.RAMBO, 150_000_000);
        progression(new BossData[]{BossesData.SO_4, BossesData.SO_3, BossesData.SO_2,
            BossesData.SO_1, BossesData.TIEU_DOI_TRUONG},
                150_000_000, 225_000_000, 300_000_000, 400_000_000, 500_000_000);
        progression(new BossData[]{BossesData.SO_4_NM, BossesData.SO_3_NM,
            BossesData.SO_2_NM, BossesData.SO_1_NM, BossesData.TIEU_DOI_TRUONG_NM},
                150_000_000, 225_000_000, 300_000_000, 400_000_000, 500_000_000);
        progression(new BossData[]{BossesData.FIDE_DAI_CA_1, BossesData.FIDE_DAI_CA_2,
            BossesData.FIDE_DAI_CA_3}, 200_000_000, 300_000_000, 400_000_000);
        progression(new BossData[]{BossesData.ANDROID_19, BossesData.DR_KORE},
                150_000_000, 250_000_000);
        progression(new BossData[]{BossesData.PIC, BossesData.POC, BossesData.KING_KONG},
                200_000_000, 300_000_000, 400_000_000);
        progression(new BossData[]{BossesData.XEN_BO_HUNG_1, BossesData.XEN_BO_HUNG_2,
            BossesData.XEN_BO_HUNG_3}, 250_000_000, 375_000_000, 500_000_000);
        progression(new BossData[]{BossesData.SIEU_BO_HUNG_1, BossesData.SIEU_BO_HUNG_2},
                300_000_000, 1_000_000_000);
        progression(new BossData[]{BossesData.XEN_CON_1, BossesData.XEN_CON_2,
            BossesData.XEN_CON_3, BossesData.XEN_CON_4, BossesData.XEN_CON_5,
            BossesData.XEN_CON_6, BossesData.XEN_CON_7},
                300_000_000, 300_000_000, 300_000_000, 300_000_000,
                300_000_000, 300_000_000, 300_000_000);
        progression(new BossData[]{BossesData.COOLER, BossesData.COOLER_2},
                1_000_000_000, 2_000_000_000);
        System.out.println("BALANCED_BOSS_HP_OK groups=10 max=2000000000");
    }

    private static void progression(BossData[] data, int... expected) {
        if (data.length != expected.length) {
            throw new AssertionError("invalid test data");
        }
        for (int i = 0; i < data.length; i++) {
            hp(data[i], expected[i]);
        }
    }

    private static void hp(BossData data, int expected) {
        if (!Arrays.equals(data.getHp(), new int[]{expected})) {
            throw new AssertionError(data.getName() + " expected hp=" + expected
                    + " actual=" + Arrays.toString(data.getHp()));
        }
    }
}
