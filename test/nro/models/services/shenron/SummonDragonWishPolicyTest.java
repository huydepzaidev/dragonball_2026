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
        noGemOrRubyWish(SummonDragon.SHENRON_1_STAR_WISHES_2);
        noGemOrRubyWish(SummonDragon.SHENRON_2_STARS_BlackGokuHES);
        noGemOrRubyWish(SummonDragon.SHENRON_3_STARS_BlackGokuHES);
        noGoldWish(SummonDragon.SHENRON_1_STAR_WISHES_1);
        noGoldWish(SummonDragon.SHENRON_1_STAR_WISHES_2);
        noGoldWish(SummonDragon.SHENRON_1_STAR_WISHES_3);
        noGoldWish(SummonDragon.SHENRON_2_STARS_BlackGokuHES);
        noGoldWish(SummonDragon.SHENRON_3_STARS_BlackGokuHES);
        require(SummonDragon.SHENRON_2_STARS_BlackGokuHES.length == 0,
                "Two-star dragon must not expose a wish");
        require(SummonDragon.SHENRON_3_STARS_BlackGokuHES.length == 0,
                "Three-star dragon must not expose a wish");
        require(SummonDragon.isSummonEnabled((byte) 1),
                "One-star dragon must remain enabled");
        require(!SummonDragon.isSummonEnabled((byte) 2),
                "Two-star dragon must be disabled");
        require(!SummonDragon.isSummonEnabled((byte) 3),
                "Three-star dragon must be disabled");
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

    private static void noGemOrRubyWish(String[] wishes) {
        for (String wish : wishes) {
            String normalized = wish.toLowerCase();
            if (normalized.contains("hồng ngọc") || normalized.contains("\nngọc")) {
                throw new AssertionError("Gem/ruby wish is still enabled: " + wish);
            }
        }
    }

    private static void noGoldWish(String[] wishes) {
        for (String wish : wishes) {
            if (wish.toLowerCase().contains("vàng")) {
                throw new AssertionError("Gold wish is still enabled: " + wish);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
