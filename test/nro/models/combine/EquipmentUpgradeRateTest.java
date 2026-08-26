package nro.models.combine;

public final class EquipmentUpgradeRateTest {

    private EquipmentUpgradeRateTest() {
    }

    public static void main(String[] args) {
        double[] expectedRates = {40, 35, 30, 25, 20, 15, 10, 5};

        check(CombineService.MAX_LEVEL_ITEM == expectedRates.length,
                "The rate table must cover every equipment level");
        for (int level = 0; level < expectedRates.length; level++) {
            double actualRate = CombineSystem.getTileNangCapDo(level);
            check(Double.compare(actualRate, expectedRates[level]) == 0,
                    "Unexpected rate for +" + level + " -> +" + (level + 1)
                            + ": " + actualRate + "%");
        }

        check(Double.compare(CombineSystem.getTileNangCapDo(-1), 0) == 0,
                "Negative levels must not have an upgrade rate");
        check(Double.compare(
                CombineSystem.getTileNangCapDo(CombineService.MAX_LEVEL_ITEM), 0) == 0,
                "Max-level equipment must not have an upgrade rate");

        System.out.println("EQUIPMENT_UPGRADE_RATE_TEST_OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
