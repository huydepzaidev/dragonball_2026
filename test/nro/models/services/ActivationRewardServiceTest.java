package nro.models.services;

/** Pure regression checks for web-configured activation-set weights. */
public final class ActivationRewardServiceTest {

    private ActivationRewardServiceTest() {
    }

    public static void main(String[] args) {
        assertDefault(0, new int[]{127, 128, 129, 233, 245}, new int[]{25, 25, 25, 900, 25});
        assertDefault(1, new int[]{130, 131, 132, 233, 237}, new int[]{25, 25, 25, 900, 25});
        assertDefault(2, new int[]{133, 135, 134, 233, 241}, new int[]{25, 25, 25, 900, 25});

        assertSharedDistribution(0);
        assertSharedDistribution(1);
        assertSharedDistribution(2);

        ActivationRewardService.ActivationConfig earth = ActivationRewardService.defaultConfig(0);
        try {
            ActivationRewardService.pickWeightedOptionForContainer(
                    1, earth.optionIds, earth.weights, 0);
            throw new AssertionError("Vật phẩm ngoài 1538/1559 phải bị từ chối.");
        } catch (IllegalArgumentException expected) {
        }
        require(ActivationRewardService.isCrystalStarOption(102), "Option 102 phải bị chặn.");
        require(ActivationRewardService.isCrystalStarOption(107), "Option 107 phải bị chặn.");
        require(!ActivationRewardService.isCrystalStarOption(127), "Option SKH không được coi là sao pha lê.");
        assertSubOptions(127, new int[]{139});
        assertSubOptions(128, new int[]{140});
        assertSubOptions(129, new int[]{141});
        assertSubOptions(130, new int[]{142});
        assertSubOptions(131, new int[]{143});
        assertSubOptions(132, new int[]{144});
        assertSubOptions(133, new int[]{136});
        assertSubOptions(134, new int[]{137});
        assertSubOptions(135, new int[]{138});
        assertSubOptions(233, new int[]{234});
        assertSubOptions(237, new int[]{238, 239, 240});
        assertSubOptions(241, new int[]{242, 243, 244});
        assertSubOptions(245, new int[]{246, 247, 248});

        System.out.println("ACTIVATION_REWARD_CONFIG_OK containers=1538,1559 shared_weight_rolls=3000"
                + " gohan_rate=90 other_rates=2.5 crystal_star_options_blocked=102,107 set_mappings=13");
    }

    private static void assertSharedDistribution(int planet) {
        ActivationRewardService.ActivationConfig config = ActivationRewardService.defaultConfig(planet);
        int total = 0;
        for (int weight : config.weights) {
            total += weight;
        }
        require(total == 1_000, "Tổng trọng số phải bằng 1.000 tại hành tinh " + planet);
        int[] counts = new int[config.optionIds.length];
        for (int roll = 0; roll < total; roll++) {
            int expectedOption = ActivationRewardService.pickWeightedOption(
                    config.optionIds, config.weights, roll);
            int boxOption = ActivationRewardService.pickWeightedOptionForContainer(
                    ActivationRewardService.SET_BOX_ID,
                    config.optionIds, config.weights, roll);
            int capsuleOption = ActivationRewardService.pickWeightedOptionForContainer(
                    ActivationRewardService.SINGLE_CAPSULE_ID,
                    config.optionIds, config.weights, roll);
            require(boxOption == expectedOption,
                    "Hộp Set lệch tỉ lệ tại hành tinh " + planet + ", roll " + roll);
            require(capsuleOption == expectedOption,
                    "Capsule lệch tỉ lệ tại hành tinh " + planet + ", roll " + roll);
            counts[indexOf(config.optionIds, expectedOption)]++;
        }
        for (int i = 0; i < config.optionIds.length; i++) {
            int expectedCount = config.optionIds[i] == 233 ? 900 : 25;
            require(counts[i] == expectedCount,
                    "Sai số mốc option #" + config.optionIds[i] + " tại hành tinh " + planet);
        }
    }

    private static int indexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        throw new AssertionError("Không tìm thấy option #" + target);
    }

    private static void assertDefault(int planet, int[] expectedOptions, int[] expectedWeights) {
        ActivationRewardService.ActivationConfig config = ActivationRewardService.defaultConfig(planet);
        require(java.util.Arrays.equals(config.optionIds, expectedOptions), "Sai pool mặc định hành tinh " + planet);
        require(java.util.Arrays.equals(config.weights, expectedWeights), "Sai trọng số mặc định hành tinh " + planet);
    }

    private static void assertSubOptions(int setOption, int[] expected) {
        int[] actual = ItemService.gI().getOptionIdsBySKH(setOption);
        require(java.util.Arrays.equals(actual, expected), "Sai option phụ của Set #" + setOption);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
