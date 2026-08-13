package nro.models.services.shenron;

public final class SummonDragonWishPolicyTest {
    private SummonDragonWishPolicyTest() {
    }

    public static void main(String[] args) {
        noPowerWish(SummonDragon.SHENRON_1_STAR_WISHES_1);
        noPowerWish(SummonDragon.SHENRON_1_STAR_WISHES_2);
        noPowerWish(SummonDragon.SHENRON_1_STAR_WISHES_3);
        noPowerWish(SummonDragon.SHENRON_2_STARS_BlackGokuHES);
        noPowerWish(SummonDragon.SHENRON_3_STARS_BlackGokuHES);
        contains(SummonDragon.SHENRON_1_STAR_WISHES_2[1], "+10K");
        contains(SummonDragon.SHENRON_1_STAR_WISHES_2[1], "Hồng Ngọc");
        System.out.println("SHENRON_WISH_POLICY_OK");
    }

    private static void noPowerWish(String[] wishes) {
        for (String wish : wishes) {
            String normalized = wish.toLowerCase();
            if (normalized.contains("sức mạnh") || normalized.contains("tiềm năng")) {
                throw new AssertionError("Power wish is still enabled: " + wish);
            }
        }
    }

    private static void contains(String value, String expected) {
        if (!value.contains(expected)) {
            throw new AssertionError("Missing '" + expected + "' in '" + value + "'");
        }
    }
}
