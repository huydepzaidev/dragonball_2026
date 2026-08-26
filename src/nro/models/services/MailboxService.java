package nro.models.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Database-backed item mailbox shared by the web admin and the game server. */
public final class MailboxService {

    private static final int PAGE_SIZE = 10;

    private MailboxService() {
    }

    public static void openMailbox(Npc npc, Player player) {
        if (npc == null || player == null || player.getSession() == null) {
            return;
        }
        List<MailEntry> mails = new ArrayList<>();
        String sql = "SELECT id,title,sender_name,rank_position FROM player_mailbox "
                + "WHERE player_id=? AND account_id=? AND status='PENDING' "
                + "ORDER BY id DESC LIMIT ?";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, player.id);
            ps.setInt(2, player.getSession().userId);
            ps.setInt(3, PAGE_SIZE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mails.add(new MailEntry(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("sender_name"),
                            (Integer) rs.getObject("rank_position")));
                }
            }
        } catch (Exception e) {
            Logger.logException(MailboxService.class, e);
            Service.gI().sendThongBao(player, "Hòm thư chưa sẵn sàng. Vui lòng báo Admin.");
            return;
        }

        MailboxState state = new MailboxState(mails);
        NpcFactory.PLAYERID_OBJECT.put(player.id, state);
        if (mails.isEmpty()) {
            npc.createOtherMenu(player, ConstNpc.MENU_MAILBOX_LIST,
                    "Con chưa có thư hoặc phần quà nào chưa nhận.", "Đóng");
            return;
        }

        String[] buttons = new String[mails.size() + 1];
        for (int i = 0; i < mails.size(); i++) {
            MailEntry mail = mails.get(i);
            buttons[i] = mail.rankPosition == null
                    ? shorten(mail.title, 28)
                    : "Top " + mail.rankPosition + "\n" + shorten(mail.title, 22);
        }
        buttons[mails.size()] = "Đóng";
        npc.createOtherMenu(player, ConstNpc.MENU_MAILBOX_LIST,
                "Con có " + mails.size() + " thư chưa nhận. Hãy chọn thư muốn xem:", buttons);
    }

    public static void handleMenu(Npc npc, Player player, int select) {
        if (npc == null || player == null) {
            return;
        }
        int menu = player.idMark.getIndexMenu();
        if (menu == ConstNpc.MENU_MAILBOX_LIST) {
            Object value = NpcFactory.PLAYERID_OBJECT.get(player.id);
            if (!(value instanceof MailboxState state)
                    || select < 0 || select >= state.mails.size()) {
                return;
            }
            state.selectedMailId = state.mails.get(select).id;
            showDetail(npc, player, state.selectedMailId);
        } else if (menu == ConstNpc.MENU_MAILBOX_DETAIL) {
            Object value = NpcFactory.PLAYERID_OBJECT.get(player.id);
            if (!(value instanceof MailboxState state) || state.selectedMailId <= 0) {
                openMailbox(npc, player);
                return;
            }
            if (select == 0) {
                claimMail(player, state.selectedMailId);
                openMailbox(npc, player);
            } else if (select == 1) {
                openMailbox(npc, player);
            }
        }
    }

    private static void showDetail(Npc npc, Player player, long mailId) {
        String sql = "SELECT title,message,sender_name,rank_position,rewards_json FROM player_mailbox "
                + "WHERE id=? AND player_id=? AND account_id=? AND status='PENDING'";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, mailId);
            ps.setLong(2, player.id);
            ps.setInt(3, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Service.gI().sendThongBao(player, "Thư không còn khả dụng.");
                    openMailbox(npc, player);
                    return;
                }
                List<MailReward> rewards = parseRewards(rs.getString("rewards_json"));
                StringBuilder text = new StringBuilder();
                Integer rank = (Integer) rs.getObject("rank_position");
                if (rank != null) {
                    text.append("|1|Quà Top ").append(rank).append('\n');
                }
                text.append("|0|").append(rs.getString("title"));
                String message = rs.getString("message");
                if (message != null && !message.isBlank()) {
                    text.append("\n").append(message);
                }
                text.append("\nNgười gửi: ").append(rs.getString("sender_name"));
                text.append("\n\nPhần thưởng:");
                for (MailReward reward : rewards) {
                    text.append("\n- ").append(rewardSummary(reward));
                }
                npc.createOtherMenu(player, ConstNpc.MENU_MAILBOX_DETAIL,
                        text.toString(), "Nhận quà", "Quay lại", "Đóng");
            }
        } catch (Exception e) {
            Logger.logException(MailboxService.class, e);
            Service.gI().sendThongBao(player, "Không thể đọc thư này.");
        }
    }

    private static void claimMail(Player player, long mailId) {
        synchronized (player) {
            boolean markedProcessing = false;
            boolean rewardsApplied = false;
            try {
                markedProcessing = markProcessing(player, mailId);
                if (!markedProcessing) {
                    Service.gI().sendThongBao(player, "Thư đã được nhận hoặc không còn khả dụng.");
                    return;
                }

                String rewardsJson = loadProcessingRewards(player, mailId);
                List<MailReward> rewards = parseRewards(rewardsJson);
                List<Item> candidateBag = InventoryService.gI().copyItemsBag(player);
                long addGold = 0;
                long addGem = 0;
                long addRuby = 0;

                for (MailReward reward : rewards) {
                    switch (reward.id) {
                        case -1 -> addGold = Math.addExact(addGold, reward.quantity);
                        case -2 -> addGem = Math.addExact(addGem, reward.quantity);
                        case -3 -> addRuby = Math.addExact(addRuby, reward.quantity);
                        default -> {
                            if (reward.id < 0 || reward.id > Short.MAX_VALUE) {
                                throw new IllegalArgumentException("ID vật phẩm không hợp lệ: " + reward.id);
                            }
                            Item item = ItemService.gI().createNewItem((short) reward.id, reward.quantity);
                            if (item == null || item.template == null) {
                                throw new IllegalArgumentException("Không tìm thấy vật phẩm #" + reward.id);
                            }
                            for (MailOption option : reward.options) {
                                Item.ItemOption itemOption = new Item.ItemOption(option.id, option.param);
                                if (itemOption.optionTemplate == null) {
                                    throw new IllegalArgumentException("Không tìm thấy option #" + option.id);
                                }
                                item.itemOptions.add(itemOption);
                            }
                            if (!InventoryService.gI().addItemList(candidateBag, item)) {
                                throw new IllegalStateException("Hành trang không đủ chỗ hoặc vật phẩm đang bị khóa.");
                            }
                        }
                    }
                }

                if (addGold > Inventory.LIMIT_GOLD - player.inventory.gold) {
                    throw new IllegalStateException("Số vàng sau khi nhận sẽ vượt giới hạn.");
                }
                if (addGem > Integer.MAX_VALUE - (long) player.inventory.gem
                        || addRuby > Integer.MAX_VALUE - (long) player.inventory.ruby) {
                    throw new IllegalStateException("Số ngọc sau khi nhận sẽ vượt giới hạn.");
                }

                player.inventory.itemsBag.clear();
                player.inventory.itemsBag.addAll(candidateBag);
                player.inventory.gold += addGold;
                player.inventory.gem += (int) addGem;
                player.inventory.ruby += (int) addRuby;
                rewardsApplied = true;

                PlayerDAO.updatePlayer(player);
                markClaimed(mailId);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                Service.gI().sendThongBao(player, "Nhận quà trong thư thành công!");
            } catch (Exception e) {
                Logger.logException(MailboxService.class, e);
                if (markedProcessing) {
                    if (rewardsApplied) {
                        markProcessingFailure(mailId, "Đã cộng quà nhưng chưa thể chốt trạng thái; cần Admin kiểm tra.");
                    } else {
                        returnToPending(mailId, compact(e.getMessage()));
                    }
                }
                Service.gI().sendThongBao(player, "Chưa thể nhận thư: " + compact(e.getMessage()));
            }
        }
    }

    private static boolean markProcessing(Player player, long mailId) throws Exception {
        String sql = "UPDATE player_mailbox SET status='PROCESSING',processing_at=NOW(),failure_reason=NULL "
                + "WHERE id=? AND player_id=? AND account_id=? AND status='PENDING'";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, mailId);
            ps.setLong(2, player.id);
            ps.setInt(3, player.getSession().userId);
            return ps.executeUpdate() == 1;
        }
    }

    private static String loadProcessingRewards(Player player, long mailId) throws Exception {
        String sql = "SELECT rewards_json FROM player_mailbox "
                + "WHERE id=? AND player_id=? AND account_id=? AND status='PROCESSING'";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, mailId);
            ps.setLong(2, player.id);
            ps.setInt(3, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Không khóa được thư để nhận.");
                }
                return rs.getString("rewards_json");
            }
        }
    }

    private static void markClaimed(long mailId) throws Exception {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE player_mailbox SET status='CLAIMED',claimed_at=NOW(),failure_reason=NULL "
                        + "WHERE id=? AND status='PROCESSING'")) {
            ps.setLong(1, mailId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Không chốt được trạng thái thư.");
            }
        }
    }

    private static void returnToPending(long mailId, String reason) {
        updateFailure(mailId, "PENDING", reason);
    }

    private static void markProcessingFailure(long mailId, String reason) {
        updateFailure(mailId, "PROCESSING", reason);
    }

    private static void updateFailure(long mailId, String status, String reason) {
        String sql = "UPDATE player_mailbox SET status=?,failure_reason=? WHERE id=? AND status='PROCESSING'";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setLong(3, mailId);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.logException(MailboxService.class, e);
        }
    }

    public static boolean sendSystemMailIdempotent(int accountId, long playerId, String title, String message,
            String senderName, String rewardsJson, String idempotencyToken) {
        if (accountId <= 0 || playerId <= 0 || title == null || rewardsJson == null) {
            return false;
        }
        try {
            parseRewards(rewardsJson);
        } catch (Exception e) {
            Logger.logException(MailboxService.class, e);
            return false;
        }

        String searchTitle = (idempotencyToken != null && !idempotencyToken.isBlank())
                ? title + " [" + idempotencyToken + "]"
                : title;

        String checkSql = "SELECT id FROM player_mailbox WHERE account_id=? AND player_id=? AND title=? "
                + "AND status IN ('PENDING','PROCESSING','CLAIMED') LIMIT 1 FOR UPDATE";
        String insertSql = "INSERT INTO player_mailbox (account_id, player_id, title, message, sender_name, rewards_json, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection con = LocalManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setInt(1, accountId);
                checkPs.setLong(2, playerId);
                checkPs.setString(3, searchTitle);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                insertPs.setInt(1, accountId);
                insertPs.setLong(2, playerId);
                insertPs.setString(3, searchTitle);
                insertPs.setString(4, message != null ? message : "");
                insertPs.setString(5, senderName != null ? senderName : "Trọng Tài");
                insertPs.setString(6, rewardsJson);
                insertPs.executeUpdate();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            Logger.logException(MailboxService.class, e);
            return false;
        }
    }

    static List<MailReward> parseRewards(String json) {
        Object parsed = JSONValue.parse(json);
        if (!(parsed instanceof JSONArray array) || array.isEmpty() || array.size() > 50) {
            throw new IllegalArgumentException("Dữ liệu phần thưởng không hợp lệ.");
        }
        List<MailReward> rewards = new ArrayList<>();
        for (Object value : array) {
            if (!(value instanceof JSONObject object)) {
                throw new IllegalArgumentException("Dữ liệu vật phẩm không hợp lệ.");
            }
            int id = exactInt(object.get("id"), "id");
            long quantityLong = number(object.get("quantity"), "quantity").longValue();
            if (id < -3 || quantityLong < 1 || quantityLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("ID hoặc số lượng phần thưởng không hợp lệ.");
            }
            List<MailOption> options = new ArrayList<>();
            Object rawOptions = object.get("options");
            if (rawOptions != null) {
                if (!(rawOptions instanceof JSONArray optionArray) || optionArray.size() > 30) {
                    throw new IllegalArgumentException("Danh sách option không hợp lệ.");
                }
                for (Object rawOption : optionArray) {
                    if (!(rawOption instanceof JSONObject optionObject)) {
                        throw new IllegalArgumentException("Option không hợp lệ.");
                    }
                    int optionId = exactInt(optionObject.get("id"), "option id");
                    int optionParam = exactInt(optionObject.get("param"), "option param");
                    if (optionId < 0) {
                        throw new IllegalArgumentException("ID option không hợp lệ.");
                    }
                    options.add(new MailOption(optionId, optionParam));
                }
            }
            if (id < 0 && !options.isEmpty()) {
                throw new IllegalArgumentException("Tiền tệ không được có option.");
            }
            rewards.add(new MailReward(id, (int) quantityLong, options));
        }
        return rewards;
    }

    private static Number number(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Thiếu " + field + ".");
        }
        return number;
    }

    private static int exactInt(Object value, String field) {
        long number = number(value, field).longValue();
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field + " vượt giới hạn.");
        }
        return (int) number;
    }

    private static String rewardSummary(MailReward reward) {
        return switch (reward.id) {
            case -1 -> reward.quantity + " vàng";
            case -2 -> reward.quantity + " ngọc";
            case -3 -> reward.quantity + " hồng ngọc";
            default -> {
                Item item = reward.id <= Short.MAX_VALUE
                        ? ItemService.gI().createNewItem((short) reward.id) : null;
                String name = item != null && item.template != null
                        ? item.template.name : "Item #" + reward.id;
                yield reward.quantity + " x " + name;
            }
        };
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "Thư quà tặng";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static String compact(String value) {
        if (value == null || value.isBlank()) {
            return "Lỗi không xác định";
        }
        String compact = value.replace('\n', ' ').replace('\r', ' ').trim();
        return compact.length() <= 180 ? compact : compact.substring(0, 180);
    }

    private static final class MailboxState {
        private final List<MailEntry> mails;
        private long selectedMailId;

        private MailboxState(List<MailEntry> mails) {
            this.mails = mails;
        }
    }

    private static final class MailEntry {
        private final long id;
        private final String title;
        private final String sender;
        private final Integer rankPosition;

        private MailEntry(long id, String title, String sender, Integer rankPosition) {
            this.id = id;
            this.title = title;
            this.sender = sender;
            this.rankPosition = rankPosition;
        }
    }

    static final class MailReward {
        final int id;
        final int quantity;
        final List<MailOption> options;

        MailReward(int id, int quantity, List<MailOption> options) {
            this.id = id;
            this.quantity = quantity;
            this.options = options;
        }
    }

    static final class MailOption {
        final int id;
        final int param;

        MailOption(int id, int param) {
            this.id = id;
            this.param = param;
        }
    }
}
