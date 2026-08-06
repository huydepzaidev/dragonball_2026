package nro.models.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.services.ItemService;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Nội dung giới thiệu Top tại Đại Thiên Sứ. Phần quà luôn được đọc từ cấu hình
 * web/DB tại thời điểm người chơi mở bảng để tránh hiển thị sai cấu hình.
 */
public final class TopRankingInfoService {

    public static final String TOP_POWER = "top_power";
    public static final String TOP_TASK = "top_task";
    public static final String TOP_SUMMER = "summer";
    public static final String TOP_BOSS = "top_boss";

    private static final TopRankingInfoService INSTANCE = new TopRankingInfoService();
    private static final String NOT_CONFIGURED = "Chưa cấu hình trên web";

    private TopRankingInfoService() {
    }

    public static TopRankingInfoService gI() {
        return INSTANCE;
    }

    public String buildInfo(String rankingKey) {
        StringBuilder text = new StringBuilder(buildRuleText(rankingKey));
        text.append("\n\nPHẦN QUÀ HIỆN TẠI");
        try {
            appendRewardSection(text, loadRewardConfigs(rankingKey));
        } catch (Exception ex) {
            Logger.logException(TopRankingInfoService.class, ex);
            text.append("\nTop 1: Không đọc được cấu hình quà lúc này")
                    .append("\nTop 2: Không đọc được cấu hình quà lúc này")
                    .append("\nTop 3: Không đọc được cấu hình quà lúc này")
                    .append("\nTop 4–10: Không đọc được cấu hình quà lúc này");
        }
        text.append("\n\nPhần thưởng được gửi vào Hòm thư khi Admin chốt Top.");
        return text.toString();
    }

    private String buildRuleText(String rankingKey) {
        return switch (rankingKey) {
            case TOP_POWER -> "TOP SỨC MẠNH"
                    + "\nXếp hạng theo sức mạnh hiện tại."
                    + "\nTop này chốt và trao thưởng một lần, không tự reset.";
            case TOP_TASK -> "TOP NHIỆM VỤ"
                    + "\nXếp hạng theo nhiệm vụ chính; nếu bằng nhau sẽ xét bước và tiến độ."
                    + "\nTrong bảng xếp hạng game hiển thị: Nhiệm vụ số X."
                    + "\nTop này chốt và trao thưởng một lần, không tự reset.";
            case TOP_SUMMER -> "TOP SỰ KIỆN HÈ"
                    + "\nXếp hạng theo điểm Sự kiện hè của người chơi."
                    + "\nĐiểm Top tính theo tuần, từ Thứ Hai đến Chủ Nhật và sang tuần mới tính lại.";
            case TOP_BOSS -> "TOP SĂN BOSS"
                    + "\nMỗi Boss hợp lệ bị hạ: +50 điểm."
                    + "\nĐiểm Top tính theo tuần, từ Thứ Hai đến Chủ Nhật và sang tuần mới tính lại."
                    + "\nBoss được tính: Siêu Bọ Hung; Xen con 1–7; Black Goku; Cumber; Beerus; Champa;"
                    + " Pilap, Mai, Pu; Sói đỏ, Sói vàng, Sói xanh xám; Chill 1–2; Cooler; Zamasu.";
            default -> "THÔNG TIN BẢNG XẾP HẠNG";
        };
    }

    private Map<Integer, String> loadRewardConfigs(String rankingKey) throws Exception {
        Map<Integer, String> rewardsByRank = new HashMap<>();
        String sql = "SELECT rank_position, rewards_json FROM top_reward_config "
                + "WHERE ranking_key = ? AND rank_position BETWEEN 1 AND 10 ORDER BY rank_position";
        try (Connection connection = LocalManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rankingKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rewardsByRank.put(result.getInt("rank_position"), result.getString("rewards_json"));
                }
            }
        }
        return rewardsByRank;
    }

    static void appendRewardSection(StringBuilder text, Map<Integer, String> rewardsByRank) {
        text.append("\nTop 1: ").append(summarizeRewards(rewardsByRank.get(1)));
        text.append("\nTop 2: ").append(summarizeRewards(rewardsByRank.get(2)));
        text.append("\nTop 3: ").append(summarizeRewards(rewardsByRank.get(3)));
        text.append("\nTop 4–10: ").append(summarizeRanksFourToTen(rewardsByRank));
    }

    private static String summarizeRanksFourToTen(Map<Integer, String> rewardsByRank) {
        String first = rewardsByRank.get(4);
        if (first == null || first.isBlank()) {
            return NOT_CONFIGURED;
        }
        Object firstParsed = JSONValue.parse(first);
        for (int rank = 5; rank <= 10; rank++) {
            String current = rewardsByRank.get(rank);
            if (current == null || current.isBlank()) {
                return "Chưa cấu hình đầy đủ trên web";
            }
            if (!Objects.equals(firstParsed, JSONValue.parse(current))) {
                return "Quà đang khác nhau theo từng hạng, xem chi tiết trên web";
            }
        }
        return summarizeRewards(first);
    }

    static String summarizeRewards(String rewardsJson) {
        if (rewardsJson == null || rewardsJson.isBlank()) {
            return NOT_CONFIGURED;
        }
        Object parsed = JSONValue.parse(rewardsJson);
        if (!(parsed instanceof JSONArray rewards) || rewards.isEmpty()) {
            return NOT_CONFIGURED;
        }
        StringBuilder summary = new StringBuilder();
        int shown = 0;
        for (Object value : rewards) {
            if (!(value instanceof JSONObject reward)) {
                continue;
            }
            Number idValue = reward.get("id") instanceof Number number ? number : null;
            Number quantityValue = reward.get("quantity") instanceof Number number ? number : null;
            if (idValue == null || quantityValue == null || quantityValue.longValue() <= 0) {
                continue;
            }
            if (shown > 0) {
                summary.append(", ");
            }
            appendReward(summary, idValue.intValue(), quantityValue.longValue());
            shown++;
        }
        return shown == 0 ? NOT_CONFIGURED : summary.toString();
    }

    private static void appendReward(StringBuilder summary, int itemId, long quantity) {
        String formattedQuantity = Util.formatNumber(quantity);
        switch (itemId) {
            case -1 -> summary.append(formattedQuantity).append(" vàng");
            case -2 -> summary.append(formattedQuantity).append(" ngọc");
            case -3 -> summary.append(formattedQuantity).append(" hồng ngọc");
            default -> summary.append(formattedQuantity).append(" x ").append(rewardName(itemId));
        }
    }

    private static String rewardName(int itemId) {
        try {
            Item item = itemId >= 0 && itemId <= Short.MAX_VALUE
                    ? ItemService.gI().createNewItem((short) itemId) : null;
            if (item != null && item.template != null && item.template.name != null) {
                return item.template.name;
            }
        } catch (Exception ignored) {
        }
        return "Item #" + itemId;
    }
}
