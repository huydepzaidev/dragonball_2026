package nro.models.managers;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player_system.GiftCode;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.services.InventoryService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GiftCodeManager {

    public String name;
    public final ArrayList<GiftCode> listGiftCode = new ArrayList<>();

    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) {
            instance = new GiftCodeManager();
        }
        return instance;
    }

    public synchronized GiftCode checkUseGiftCode(Player player, String code) {
        for (GiftCode giftCode : listGiftCode) {
            if (giftCode.code.equalsIgnoreCase(code)) {
                if (giftCode.countLeft <= 0) {
                    Service.gI().sendThongBaoOK(player, "Giftcode đã hết");
                    return null;
                } else if (giftCode.isUsedGiftCode(player)) {
                    Service.gI().sendThongBaoOK(player, "Tham lam!");
                    return null;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < giftCode.detail.size()) {
                    Service.gI().sendThongBaoOK(player, "Cần tối thiểu " + giftCode.detail.size() + " ô hành trang trống");
                    return null;
                }
                giftCode.countLeft -= 1;
                player.giftCode.add(code);
                updateGiftCode(giftCode);
                return giftCode;
            }
        }
        return null;
    }

    /**
     * Nạp lại toàn bộ giftcode khi người chơi mở ô nhập mã. Mỗi đối tượng
     * Player chỉ được thực hiện tối đa hai lần, tương ứng một phiên đăng nhập.
     */
    public synchronized boolean reloadAllGiftCodes(Player player) {
        if (player == null || player.giftCode == null || !player.giftCode.tryAcquireReload()) {
            return false;
        }

        String sql = "SELECT id, code, count_left, detail, datecreate, expired FROM giftcode";
        ArrayList<GiftCode> loadedGiftCodes = new ArrayList<>();
        try (Connection connection = LocalManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    GiftCode loaded = new GiftCode();
                    loaded.id = result.getInt("id");
                    loaded.code = result.getString("code");
                    loaded.countLeft = result.getInt("count_left");
                    if (loaded.countLeft == -1) {
                        loaded.countLeft = 999999999;
                    }
                    loaded.datecreate = result.getTimestamp("datecreate");
                    loaded.dateexpired = result.getTimestamp("expired");

                    JSONArray rewards = (JSONArray) JSONValue.parse(result.getString("detail"));
                    if (rewards != null) {
                        for (Object rewardObject : rewards) {
                            JSONObject reward = (JSONObject) rewardObject;
                            int itemId = Integer.parseInt(reward.get("id").toString());
                            int quantity = Integer.parseInt(reward.get("quantity").toString());
                            ArrayList<Item.ItemOption> itemOptions = new ArrayList<>();
                            JSONArray options = (JSONArray) reward.get("options");
                            if (options != null) {
                                for (Object optionObject : options) {
                                    JSONObject option = (JSONObject) optionObject;
                                    int optionId = Integer.parseInt(option.get("id").toString());
                                    int param = Integer.parseInt(option.get("param").toString());
                                    itemOptions.add(new Item.ItemOption(optionId, param));
                                }
                            }
                            loaded.detail.put(itemId, quantity);
                            loaded.option.put(itemId, itemOptions);
                        }
                    }
                    loadedGiftCodes.add(loaded);
                }
            }

            listGiftCode.clear();
            listGiftCode.addAll(loadedGiftCodes);
            return true;
        } catch (Exception exception) {
            // Chỉ thay danh sách sau khi đọc thành công để giữ dữ liệu đang chạy khi DB lỗi.
            exception.printStackTrace();
            return false;
        }
    }

    public void updateGiftCode(GiftCode giftcode) {
        try {
            LocalManager.executeUpdate("update giftcode set count_left = ? where id = ?", giftcode.countLeft, giftcode.id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (GiftCode giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng còn lại: ").append(giftCode.countLeft).append("\b")
                    .append("Ngày tạo: ")
                    .append(giftCode.datecreate).append(", Ngày hết hạn: ").append(giftCode.dateexpired)
                    .append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }

}
