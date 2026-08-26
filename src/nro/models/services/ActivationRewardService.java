package nro.models.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        {25, 25, 25, 900, 25},
        {25, 25, 25, 900, 25},
        {25, 25, 25, 900, 25}
    };

    private static final Map<String, Integer> SET_NAME_TO_OPTION = Map.ofEntries(
            Map.entry("kaio", 245),
            Map.entry("kaiovip", 245),
            Map.entry("kaio_vip", 245),
            Map.entry("thanvutrukaio", 245),
            Map.entry("nail", 237),
            Map.entry("nailvip", 237),
            Map.entry("nail_vip", 237),
            Map.entry("nailchienbinh", 237),
            Map.entry("cadicm", 241),
            Map.entry("cadic_m", 241),
            Map.entry("cadicmvip", 241),
            Map.entry("vegetam", 241),
            Map.entry("vegeta_m", 241),
            Map.entry("gohan", 233),
            Map.entry("songoku", 129),
            Map.entry("goku", 129),
            Map.entry("kirin", 128),
            Map.entry("krillin", 128),
            Map.entry("thenxinhang", 127),
            Map.entry("tien", 127),
            Map.entry("kakarot", 133),
            Map.entry("nappa", 135),
            Map.entry("cadic", 134),
            Map.entry("vegeta", 134),
            Map.entry("pikkoro", 132),
            Map.entry("pikkorodaimao", 132),
            Map.entry("daimao", 132),
            Map.entry("octieu", 131),
            Map.entry("oc_tieu", 131),
            Map.entry("lienhoan", 131),
            Map.entry("lien_hoan", 131),
            Map.entry("kamejoko", 129),
            Map.entry("thaiduonghasan", 128),
            Map.entry("tdhs", 128),
            Map.entry("damdragon", 133),
            Map.entry("galick", 134),
            Map.entry("bienkhi", 135),
            Map.entry("tusat", 135),
            Map.entry("piccolo", 130),
            Map.entry("picolo", 130),
            Map.entry("pic", 130)
    );

    private static ActivationRewardService instance;

    private ActivationRewardService() {
    }

    public static ActivationRewardService gI() {
        if (instance == null) {
            instance = new ActivationRewardService();
        }
        return instance;
    }

    public static int getVipOptionByPlanet(int planet) {
        return switch (normalizePlanet(planet)) {
            case 0 -> 245; // Set Thần Vũ Trụ Kaio
            case 1 -> 237; // Set Nail Chiến Binh
            case 2 -> 241; // Set Cađic M
            default -> 245;
        };
    }

    public static int getNativePlanetForOption(int optionId) {
        return switch (optionId) {
            case 127, 128, 129, 245 -> 0; // Trái Đất
            case 130, 131, 132, 237 -> 1; // Namếc
            case 133, 134, 135, 241 -> 2; // Xayda
            default -> 0;
        };
    }

    public static boolean isOptionAllowedForPlanet(int optionId, int planet) {
        int norm = normalizePlanet(planet);
        return contains(DEFAULT_OPTIONS[norm], optionId);
    }

    public static int resolvePlanetForOption(int optionId, int preferredPlanet) {
        int norm = normalizePlanet(preferredPlanet);
        if (isOptionAllowedForPlanet(optionId, norm)) {
            return norm;
        }
        return getNativePlanetForOption(optionId);
    }

    public static int resolveActivationOption(String input) {
        if (input == null || input.isBlank()) {
            return -1;
        }
        String clean = input.trim().toLowerCase(Locale.ROOT);
        try {
            int id = Integer.parseInt(clean);
            if (isAnyActivationOption(id)) {
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        return SET_NAME_TO_OPTION.getOrDefault(clean, -1);
    }

    public static String getSetName(int activationOption) {
        return switch (activationOption) {
            case 245 -> "Set Thần Vũ Trụ Kaio (VIP)";
            case 237 -> "Set Nail Chiến Binh (VIP)";
            case 241 -> "Set Cađic M (VIP)";
            case 233 -> "Set Gohan";
            case 129 -> "Set Sôngôku";
            case 128 -> "Set Kirin";
            case 127 -> "Set Thên Xin Hăng";
            case 133 -> "Set Kakarot";
            case 135 -> "Set Nappa";
            case 134 -> "Set Ca Đíc";
            case 132 -> "Set Pikkoro Daimao";
            case 131 -> "Set Ốc Tiêu";
            case 130 -> "Set Picolo";
            default -> "Set Kích Hoạt #" + activationOption;
        };
    }

    public void openSetBox(Player player, Item box) {
        open(player, box, true);
    }

    public void openSingleCapsule(Player player, Item capsule) {
        open(player, capsule, false);
    }

    public void buffVipSetForAdmin(Player player) {
        if (player == null || !player.isAdmin()) {
            return;
        }
        int planet = normalizePlanet(player.gender);
        int vipOption = getVipOptionByPlanet(planet);
        buffActivationSet(player, planet, vipOption, 12);
    }

    public void handleAdminSKHCommand(Player player, String argument) {
        if (player == null || !player.isAdmin()) {
            return;
        }
        if (argument == null || argument.isBlank()) {
            buffVipSetForAdmin(player);
            return;
        }

        String[] tokens = argument.trim().split("\\s+");
        int activationOption = resolveActivationOption(tokens[0]);
        if (activationOption == -1) {
            Service.gI().sendThongBao(player, "Không tìm thấy Set kích hoạt: " + tokens[0]);
            return;
        }

        int tier = 12;
        if (tokens.length >= 2) {
            try {
                tier = Integer.parseInt(tokens[1]);
                if (tier < 1 || tier > 12) {
                    Service.gI().sendThongBao(player, "Cấp đồ (tier) phải từ 1 đến 12.");
                    return;
                }
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Cấp đồ (tier) phải là số từ 1 đến 12.");
                return;
            }
        }

        int gender = resolvePlanetForOption(activationOption, player.gender);
        if (tokens.length >= 3) {
            try {
                gender = Integer.parseInt(tokens[2]);
                if (gender < 0 || gender > 2) {
                    Service.gI().sendThongBao(player, "Hành tinh (gender) phải từ 0 đến 2.");
                    return;
                }
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Hành tinh (gender) phải là số từ 0 đến 2.");
                return;
            }
        }

        buffActivationSet(player, gender, activationOption, tier);
    }

    public boolean buffActivationSet(Player player, int planet, int activationOption, int tier) {
        if (player == null || !player.isAdmin()) {
            return false;
        }

        if (planet < 0 || planet > 2) {
            Service.gI().sendThongBao(player, "Hành tinh không hợp lệ (0: Trái Đất, 1: Namếc, 2: Xayda).");
            return false;
        }

        if (tier < 1 || tier > 12) {
            Service.gI().sendThongBao(player, "Cấp đồ (tier) phải từ 1 đến 12.");
            return false;
        }

        if (!isAnyActivationOption(activationOption)) {
            Service.gI().sendThongBao(player, "Set kích hoạt không hợp lệ: " + activationOption);
            return false;
        }

        int rewardCount = 5;
        if (InventoryService.gI().getCountEmptyBag(player) < rewardCount) {
            Service.gI().sendThongBao(player,
                    "Cần ít nhất " + rewardCount + " ô trống trong hành trang để nhận Set kích hoạt.");
            return false;
        }

        try {
            ActivationConfig config = loadConfig(planet);
            List<Item> rewards = createSpecificRewards(planet, activationOption, config.bonusOptions, tier);
            if (rewards.size() != rewardCount) {
                throw new IllegalStateException("Không thể tạo đủ 5 món Set kích hoạt.");
            }

            for (Item reward : rewards) {
                if (!InventoryService.gI().addItemBag(player, reward)) {
                    throw new IllegalStateException("Không thể thêm trang bị Set kích hoạt vào hành trang.");
                }
            }

            InventoryService.gI().sendItemBags(player);
            String setName = getSetName(activationOption);
            Service.gI().sendThongBao(player,
                    "Admin đã nhận đủ 5 món " + setName + " (Cấp " + tier + ").");
            return true;
        } catch (Exception e) {
            Logger.logException(ActivationRewardService.class, e);
            Service.gI().sendThongBao(player, "Không thể buff Set kích hoạt lúc này.");
            return false;
        }
    }

    public List<Item> createSpecificRewards(int planet, int activationOption,
            List<BonusOption> bonusOptions, int tier) {
        int normalizedPlanet = normalizePlanet(planet);
        int clampedTier = Math.max(1, Math.min(12, tier));
        List<Item> rewards = new ArrayList<>();
        for (int slot = 0; slot < ITEM_IDS[normalizedPlanet].length; slot++) {
            int[] candidates = ITEM_IDS[normalizedPlanet][slot];
            int tierIndex = Math.min(clampedTier - 1, candidates.length - 1);
            int itemId = candidates[Math.max(0, tierIndex)];
            Item reward = ItemService.gI().createItemSKH(itemId, activationOption);
            if (reward == null || reward.template == null) {
                throw new IllegalStateException("Item SKH không tồn tại: " + itemId);
            }
            removeCrystalStarOptions(reward);
            if (bonusOptions != null) {
                for (BonusOption bonus : bonusOptions) {
                    if (!isCrystalStarOption(bonus.id)) {
                        upsertOption(reward, bonus.id, bonus.param);
                    }
                }
            }
            removeCrystalStarOptions(reward);
            reward.content = reward.getContent();
            reward.info = reward.getInfo();
            rewards.add(reward);
        }
        return rewards;
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
