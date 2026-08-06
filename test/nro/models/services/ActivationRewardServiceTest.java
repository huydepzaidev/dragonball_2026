package nro.models.services;

/** Pure regression checks for web-configured activation-set weights. */
public final class ActivationRewardServiceTest {

    private ActivationRewardServiceTest() {
    }

    public static void main(String[] args) {
        assertDefault(0, new int[]{127, 128, 129, 233, 245}, new int[]{20, 120, 20, 120, 20});
        assertDefault(1, new int[]{130, 131, 132, 233, 237}, new int[]{120, 20, 20, 120, 20});
        assertDefault(2, new int[]{133, 135, 134, 233, 241}, new int[]{20, 20, 120, 120, 20});

        ActivationRewardService.ActivationConfig earth = ActivationRewardService.defaultConfig(0);
        require(ActivationRewardService.pickWeightedOption(earth.optionIds, earth.weights, 0) == 127,
                "Roll đầu phải ra option đầu.");
        require(ActivationRewardService.pickWeightedOption(earth.optionIds, earth.weights, 19) == 127,
                "Biên trên option đầu sai.");
        require(ActivationRewardService.pickWeightedOption(earth.optionIds, earth.weights, 20) == 128,
                "Biên dưới option thứ hai sai.");
        require(ActivationRewardService.pickWeightedOption(earth.optionIds, earth.weights, 299) == 245,
                "Roll cuối phải ra option cuối.");
        for (int roll = 0; roll < 300; roll++) {
            int boxOption = ActivationRewardService.pickWeightedOptionForContainer(
                    ActivationRewardService.SET_BOX_ID,
                    earth.optionIds, earth.weights, roll);
            int capsuleOption = ActivationRewardService.pickWeightedOptionForContainer(
                    ActivationRewardService.SINGLE_CAPSULE_ID,
                    earth.optionIds, earth.weights, roll);
            require(boxOption == capsuleOption,
                    "Hộp Set và Capsule lệch tỉ lệ tại roll " + roll + ".");
        }
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

        System.out.println("ACTIVATION_REWARD_CONFIG_OK containers=1538,1559 shared_weight_rolls=300"
                + " crystal_star_options_blocked=102,107 set_mappings=13");
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
