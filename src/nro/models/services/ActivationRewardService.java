package nro.models.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Opens activation-set rewards using the weights configured by the web admin. */
public final class ActivationRewardService {

    public static final int SET_BOX_ID = 1538;
    public static final int SINGLE_CAPSULE_ID = 1559;
    static final int CRYSTAL_STAR_OPTION = 102;
    static final int CRYSTAL_STAR_SLOT_OPTION = 107;

    private static final int[][][] ITEM_IDS = {
        {
            {0, 3, 33, 34, 136, 137, 138, 139, 230, 231, 232, 233},
            {6, 9, 35, 36, 140, 141, 142, 143, 242, 243, 244, 245},
            {21, 24, 37, 38, 144, 145, 146, 147, 254, 256, 257},
            {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269},
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281}
        },
        {
            {1, 4, 41, 42, 152, 153, 154, 155, 235, 236, 237},
            {7, 10, 43, 44, 156, 157, 158, 159, 246, 247, 248, 249},
            {22, 25, 45, 46, 160, 161, 162, 163, 259, 260, 261},
            {28, 31, 47, 48, 164, 165, 166, 167, 270, 271, 272, 273},
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281}
        },
        {
            {2, 5, 49, 50, 168, 169, 170, 171, 238, 239, 240, 241},
            {8, 11, 51, 52, 172, 173, 174, 174, 250, 251, 252, 253},
            {23, 26, 53, 54, 176, 177, 178, 179, 262, 263, 264, 265},
            {29, 32, 55, 56, 180, 181, 182, 183, 274, 275, 276, 277},
            {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281}
        }
    };

    private static final int[][] DEFAULT_OPTIONS = {
        {127, 128, 129, 233, 245},
        {130, 131, 132, 233, 237},
        {133, 135, 134, 233, 241}
    };

    private static final int[][] DEFAULT_WEIGHTS = {
        {20, 120, 20, 120, 20},
        {120, 20, 20, 120, 20},
        {20, 20, 120, 120, 20}
    };

    private static ActivationRewardService instance;

    private ActivationRewardService() {
    }

    public static ActivationRewardService gI() {
        if (instance == null) {
            instance = new ActivationRewardService();
        }
        return instance;
    }

    public void openSetBox(Player player, Item box) {
        open(player, box, true);
    }

    public void openSingleCapsule(Player player, Item capsule) {
        open(player, capsule, false);
    }

    private void open(Player player, Item container, boolean fullSet) {
        if (player == null || container == null || container.template == null || container.quantity < 1) {
            return;
        }
        int expectedId = fullSet ? SET_BOX_ID : SINGLE_CAPSULE_ID;
        if (container.template.id != expectedId) {
            return;
        }

        int rewardCount = fullSet ? 5 : 1;
        if (InventoryService.gI().getCountEmptyBag(player) < rewardCount) {
            Service.gI().sendThongBao(player,
                    "Cần ít nhất " + rewardCount + " ô trống trong hành trang để mở vật phẩm này.");
            return;
        }

        try {
            int planet = normalizePlanet(player.gender);
            ActivationConfig config = loadConfig(planet);
            int activationOption = randomWeightedOption(
                    container.template.id, config.optionIds, config.weights);
            List<Item> rewards = createRewards(planet, activationOption, config.bonusOptions, fullSet);
            if (rewards.size() != rewardCount) {
                throw new IllegalStateException("Không thể tạo đủ trang bị Set kích hoạt.");
            }
            for (Item reward : rewards) {
                if (!InventoryService.gI().addItemBag(player, reward)) {
                    throw new IllegalStateException("Không thể thêm trang bị Set kích hoạt vào hành trang.");
                }
            }
            InventoryService.gI().subQuantityItemsBag(player, container, 1);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, fullSet
                    ? "Bạn đã nhận đủ 5 món Set kích hoạt thường, không có sao pha lê."
                    : "Bạn đã nhận 1 món Set kích hoạt thường, không có sao pha lê.");
        } catch (Exception e) {
            Logger.logException(ActivationRewardService.class, e);
            Service.gI().sendThongBao(player, "Không thể mở vật phẩm Set kích hoạt lúc này.");
        }
    }

    private List<Item> createRewards(int planet, int activationOption,
            List<BonusOption> bonusOptions, boolean fullSet) {
        List<Item> rewards = new ArrayList<>();
        int firstSlot = fullSet ? 0 : Util.nextInt(ITEM_IDS[planet].length);
        int lastSlot = fullSet ? ITEM_IDS[planet].length - 1 : firstSlot;
        for (int slot = firstSlot; slot <= lastSlot; slot++) {
            int[] candidates = ITEM_IDS[planet][slot];
            int itemId = candidates[Util.nextInt(candidates.length)];
            Item reward = ItemService.gI().createItemSKH(itemId, activationOption);
            if (reward == null || reward.template == null) {
                throw new IllegalStateException("Item SKH không tồn tại: " + itemId);
            }
            removeCrystalStarOptions(reward);
            for (BonusOption bonus : bonusOptions) {
                if (!isCrystalStarOption(bonus.id)) {
                    upsertOption(reward, bonus.id, bonus.param);
                }
            }
            removeCrystalStarOptions(reward);
            reward.content = reward.getContent();
            reward.info = reward.getInfo();
            rewards.add(reward);
        }
        return rewards;
    }

    private ActivationConfig loadConfig(int planet) {
        ActivationConfig fallback = defaultConfig(planet);
        String sql = "SELECT activation_options_json,activation_weights_json,bonus_options_json "
                + "FROM activation_reward_config WHERE planet=? AND enabled=1 LIMIT 1";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planet);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return fallback;
                }
                ActivationConfig configured = parseConfig(planet,
                        rs.getString("activation_options_json"),
                        rs.getString("activation_weights_json"),
                        rs.getString("bonus_options_json"));
                return configured.optionIds.length == 0 ? fallback : configured;
            }
        } catch (Exception e) {
            Logger.logException(ActivationRewardService.class, e);
            return fallback;
        }
    }

    private ActivationConfig parseConfig(int planet, String optionsJson, String weightsJson,
            String bonusJson) {
        Object parsedOptions = JSONValue.parse(optionsJson);
        Object parsedWeights = JSONValue.parse(weightsJson);
        if (!(parsedOptions instanceof JSONArray optionArray)
                || !(parsedWeights instanceof JSONObject weightObject)) {
            return defaultConfig(planet);
        }

        List<Integer> ids = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Object rawId : optionArray) {
            if (!(rawId instanceof Number number)) {
                continue;
            }
            int id = number.intValue();
            if (!contains(DEFAULT_OPTIONS[planet], id) || ids.contains(id)) {
                continue;
            }
            Object rawWeight = weightObject.get(String.valueOf(id));
            if (!(rawWeight instanceof Number weightNumber)) {
                continue;
            }
            int weight = weightNumber.intValue();
            if (weight < 1 || weight > 1_000_000) {
                continue;
            }
            ids.add(id);
            weights.add(weight);
        }
        if (ids.isEmpty()) {
            return defaultConfig(planet);
        }

        List<BonusOption> bonuses = new ArrayList<>();
        Object parsedBonus = JSONValue.parse(bonusJson);
        if (parsedBonus instanceof JSONArray bonusArray) {
            for (Object rawBonus : bonusArray) {
                if (!(rawBonus instanceof JSONObject bonusObject)
                        || !(bonusObject.get("id") instanceof Number idNumber)
                        || !(bonusObject.get("param") instanceof Number paramNumber)) {
                    continue;
                }
                int id = idNumber.intValue();
                if (id < 0 || isCrystalStarOption(id) || isAnyActivationOption(id)) {
                    continue;
                }
                bonuses.add(new BonusOption(id, paramNumber.intValue()));
                if (bonuses.size() >= 20) {
                    break;
                }
            }
        }
        return new ActivationConfig(toIntArray(ids), toIntArray(weights), bonuses);
    }

    static ActivationConfig defaultConfig(int planet) {
        int normalized = normalizePlanet(planet);
        return new ActivationConfig(
                Arrays.copyOf(DEFAULT_OPTIONS[normalized], DEFAULT_OPTIONS[normalized].length),
                Arrays.copyOf(DEFAULT_WEIGHTS[normalized], DEFAULT_WEIGHTS[normalized].length),
                List.of());
    }

    static int pickWeightedOption(int[] optionIds, int[] weights, int roll) {
        if (optionIds == null || weights == null || optionIds.length == 0
                || optionIds.length != weights.length) {
            throw new IllegalArgumentException("Cấu hình trọng số không hợp lệ.");
        }
        int total = 0;
        for (int weight : weights) {
            if (weight < 1 || total > Integer.MAX_VALUE - weight) {
                throw new IllegalArgumentException("Trọng số không hợp lệ.");
            }
            total += weight;
        }
        if (roll < 0 || roll >= total) {
            throw new IllegalArgumentException("Giá trị random ngoài phạm vi.");
        }
        int cursor = roll;
        for (int i = 0; i < optionIds.length; i++) {
            if (cursor < weights[i]) {
                return optionIds[i];
            }
            cursor -= weights[i];
        }
        throw new IllegalStateException("Không chọn được Set kích hoạt.");
    }

    static int pickWeightedOptionForContainer(
            int containerId, int[] optionIds, int[] weights, int roll) {
        if (containerId != SET_BOX_ID && containerId != SINGLE_CAPSULE_ID) {
            throw new IllegalArgumentException("Vật phẩm không dùng bảng tỉ lệ Set kích hoạt.");
        }
        return pickWeightedOption(optionIds, weights, roll);
    }

    static boolean isCrystalStarOption(int optionId) {
        return optionId == CRYSTAL_STAR_OPTION || optionId == CRYSTAL_STAR_SLOT_OPTION;
    }

    private static int randomWeightedOption(int containerId, int[] optionIds, int[] weights) {
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        return pickWeightedOptionForContainer(
                containerId, optionIds, weights, Util.nextInt(total));
    }

    private static void removeCrystalStarOptions(Item item) {
        item.itemOptions.removeIf(option -> option != null && option.optionTemplate != null
                && isCrystalStarOption(option.optionTemplate.id));
    }

    private static void upsertOption(Item item, int optionId, int param) {
        if (ItemService.gI().getItemOptionTemplate(optionId) == null) {
            return;
        }
        for (Item.ItemOption option : item.itemOptions) {
            if (option != null && option.optionTemplate != null
                    && option.optionTemplate.id == optionId) {
                option.param = param;
                return;
            }
        }
        item.itemOptions.add(new Item.ItemOption(optionId, param));
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnyActivationOption(int optionId) {
        for (int[] options : DEFAULT_OPTIONS) {
            if (contains(options, optionId)) {
                return true;
            }
        }
        return false;
    }

    private static int normalizePlanet(int planet) {
        return planet < 0 || planet > 2 ? 2 : planet;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    static final class ActivationConfig {
        final int[] optionIds;
        final int[] weights;
        final List<BonusOption> bonusOptions;

        ActivationConfig(int[] optionIds, int[] weights, List<BonusOption> bonusOptions) {
            this.optionIds = optionIds;
            this.weights = weights;
            this.bonusOptions = bonusOptions;
        }
    }

    static final class BonusOption {
        final int id;
        final int param;

        BonusOption(int id, int param) {
            this.id = id;
            this.param = param;
        }
    }
}
