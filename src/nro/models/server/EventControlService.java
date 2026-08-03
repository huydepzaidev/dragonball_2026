package nro.models.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.ItemMapService;
import nro.models.map.service.MapService;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.models.services.ChatGlobalService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/**
 * Runtime event switch controlled by the website through database commands.
 *
 * Disabling and enabling both perform a clean reset. Event items are removed
 * from online memory, every persisted player inventory and map drops before the
 * target state is exposed. Original player JSON is retained per command in
 * game_event_player_backup for emergency recovery.
 */
public final class EventControlService {

    private static final int SANTA_NPC_ID = 39;

    /**
     * Legacy boxes sold permanently in the event-point shop and every reward
     * they can create. They remain usable after their original event ends.
     */
    private static final Set<Integer> PERSISTENT_LEGACY_REWARD_ITEMS = Set.of(
            1592, 1608, 1757, 1821, 1840,
            1587, 1588, 1589, 1590, 1593, 1595,
            1599, 1600, 1601, 1602, 1807,
            1741, 1742, 1743, 1744, 1745, 1746,
            1765, 1766, 1767, 1768, 1769, 1770, 1771);

    public static final String LUNAR_NEW_YEAR = "lunar_new_year";
    public static final String VALENTINE = "valentine";
    public static final String WOMENS_DAY = "womens_day";
    public static final String HUNG_VUONG = "hung_vuong";
    public static final String SUMMER = "summer";
    public static final String MID_AUTUMN = "mid_autumn";
    public static final String HALLOWEEN = "halloween";
    public static final String TEACHERS_DAY = "teachers_day";
    public static final String CHRISTMAS = "christmas";
    public static final String TOP_UP = "top_up";

    private static final EventControlService INSTANCE = new EventControlService();
    private static final String EMPTY_EVENT_COUNTERS = "[0,0,0,0,0,0]";

    private volatile Map<String, Boolean> states = Collections.emptyMap();
    private volatile Map<Integer, String> itemEvents = Collections.emptyMap();
    private volatile Set<Integer> santaShopItems = Collections.emptySet();
    private volatile Map<Integer, List<NpcRule>> npcEvents = Collections.emptyMap();
    private volatile Map<Integer, String> bossEvents = Collections.emptyMap();
    private volatile boolean available;

    private EventControlService() {
    }

    public static EventControlService gI() {
        return INSTANCE;
    }

    public synchronized void load(Connection con) {
        try {
            Set<Integer> nextSantaShopItems = loadSantaShopItemIds(con);

            Map<String, Boolean> nextStates = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT event_key, enabled FROM game_event_catalog");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nextStates.put(rs.getString("event_key"), rs.getBoolean("enabled"));
                }
            }

            Map<Integer, String> nextItems = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT event_key, item_id FROM game_event_item");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    if (!nextSantaShopItems.contains(itemId)) {
                        nextItems.put(itemId, rs.getString("event_key"));
                    }
                }
            }

            Map<Integer, List<NpcRule>> nextNpcs = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT event_key, npc_id, map_id, x, y, managed_runtime "
                    + "FROM game_event_npc");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer mapId = (Integer) rs.getObject("map_id");
                    Integer x = (Integer) rs.getObject("x");
                    Integer y = (Integer) rs.getObject("y");
                    NpcRule rule = new NpcRule(
                            rs.getString("event_key"), rs.getInt("npc_id"),
                            mapId, x, y, rs.getBoolean("managed_runtime"));
                    nextNpcs.computeIfAbsent(rule.npcId, ignored -> new ArrayList<>()).add(rule);
                }
            }

            Map<Integer, String> nextBosses = new HashMap<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT event_key, boss_id FROM game_event_boss");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nextBosses.put(rs.getInt("boss_id"), rs.getString("event_key"));
                }
            }

            states = Collections.unmodifiableMap(nextStates);
            itemEvents = Collections.unmodifiableMap(nextItems);
            santaShopItems = Collections.unmodifiableSet(nextSantaShopItems);
            Map<Integer, List<NpcRule>> npcSnapshot = new HashMap<>();
            nextNpcs.forEach((id, rules) -> npcSnapshot.put(id, List.copyOf(rules)));
            npcEvents = Collections.unmodifiableMap(npcSnapshot);
            bossEvents = Collections.unmodifiableMap(nextBosses);
            available = !nextStates.isEmpty();
        } catch (SQLException e) {
            available = false;
            Logger.error("Không thể nạp catalog sự kiện web: " + compactError(e) + "\n");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isEnabled(String eventKey) {
        Boolean enabled = states.get(eventKey);
        return enabled == null || enabled;
    }

    public String getDisabledEventForItem(int itemId) {
        String eventKey = itemEvents.get(itemId);
        return eventKey != null && !isEnabled(eventKey) ? eventKey : null;
    }

    public boolean canAcquireItem(int itemId) {
        return isPermanentNonEventItem(itemId)
                || getDisabledEventForItem(itemId) == null;
    }

    private boolean isPermanentNonEventItem(int itemId) {
        return PERSISTENT_LEGACY_REWARD_ITEMS.contains(itemId)
                || santaShopItems.contains(itemId);
    }

    /**
     * Every item sold by Santa is permanent shop stock. The event catalog must
     * never block or purge it, even when an item id overlaps an event id range.
     */
    private Set<Integer> loadSantaShopItemIds(Connection con) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT si.temp_id "
                + "FROM item_shop si "
                + "INNER JOIN tab_shop st ON st.id=si.tab_id "
                + "INNER JOIN shop s ON s.id=st.shop_id "
                + "WHERE s.npc_id=?")) {
            ps.setInt(1, SANTA_NPC_ID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    public boolean canOpenNpc(int npcId, int mapId) {
        List<NpcRule> rules = npcEvents.get(npcId);
        if (rules == null) {
            return true;
        }
        for (NpcRule rule : rules) {
            if (rule.managedRuntime && (rule.mapId == null || rule.mapId == mapId)
                    && !isEnabled(rule.eventKey)) {
                return false;
            }
        }
        return true;
    }

    public boolean canCreateBoss(int bossId) {
        String eventKey = bossEvents.get(bossId);
        return eventKey == null || isEnabled(eventKey);
    }

    public void applyStartupState() {
        if (!available) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : states.entrySet()) {
            if (entry.getValue()) {
                try {
                    restoreEventNpcs(entry.getKey());
                    restoreEventBosses(entry.getKey());
                } catch (Exception e) {
                    Logger.error("Không thể đồng bộ event đang bật " + entry.getKey()
                            + ": " + compactError(e) + "\n");
                }
            } else {
                removeEventNpcs(entry.getKey());
                removeEventBosses(entry.getKey());
                if (HUNG_VUONG.equals(entry.getKey())) {
                    clearPersistedWatermelons();
                }
            }
        }
    }

    private void clearPersistedWatermelons() {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE player SET data_duahau_egg='[]' "
                        + "WHERE data_duahau_egg IS NULL OR TRIM(data_duahau_egg) <> '[]'")) {
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error("Không thể xóa dữ liệu Dưa hấu khi sự kiện Hùng Vương đang tắt: "
                    + compactError(e) + "\n");
        }
    }

    public void processCommands() {
        if (!available) {
            return;
        }
        List<EventCommand> commands = new ArrayList<>();
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, event_key, target_enabled FROM game_event_command "
                        + "WHERE status='PENDING' ORDER BY id LIMIT 3");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                commands.add(new EventCommand(rs.getLong("id"),
                        rs.getString("event_key"), rs.getBoolean("target_enabled")));
            }
        } catch (Exception e) {
            Logger.error("Không thể đọc hàng đợi sự kiện: " + compactError(e) + "\n");
            return;
        }
        for (EventCommand command : commands) {
            processCommand(command);
        }
    }

    private void processCommand(EventCommand command) {
        if (!claimCommand(command.id)) {
            return;
        }
        boolean oldState = isEnabled(command.eventKey);
        String result;
        boolean success = false;
        try {
            if (!states.containsKey(command.eventKey)) {
                throw new IllegalArgumentException("Sự kiện không tồn tại: " + command.eventKey);
            }

            // Close every acquisition path immediately while cleanup is running.
            setSnapshotState(command.eventKey, false);
            removeEventBosses(command.eventKey);
            removeEventNpcs(command.eventKey);

            Set<Integer> itemIds = loadPurgeItemIds(command.eventKey);
            int groundCount = purgeGroundItems(itemIds);
            int onlineCount = purgeOnlinePlayers(command.eventKey, itemIds);
            PurgeResult database = purgeDatabasePlayers(command, itemIds);

            updateCatalogState(command.eventKey, command.targetEnabled,
                    "Đã reset " + database.changedPlayers + " nhân vật, "
                    + onlineCount + " online và " + groundCount + " item dưới đất.");
            setSnapshotState(command.eventKey, command.targetEnabled);

            if (command.targetEnabled) {
                restoreEventNpcs(command.eventKey);
                restoreEventBosses(command.eventKey);
            }

            result = (command.targetEnabled ? "Đã bật" : "Đã tắt")
                    + " sự kiện; xóa " + database.removedSlots + " ô item của "
                    + database.changedPlayers + " nhân vật và " + groundCount + " item dưới đất.";
            success = true;
            announce(command.eventKey, command.targetEnabled);
        } catch (Exception e) {
            setSnapshotState(command.eventKey, oldState);
            result = compactError(e);
            Logger.error("Lỗi áp dụng sự kiện " + command.eventKey + ": " + result + "\n");
        }
        finishCommand(command.id, success, result);
    }

    private Set<Integer> loadPurgeItemIds(String eventKey) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT item_id FROM game_event_item WHERE event_key=? AND purge_on_reset=1")) {
            ps.setString(1, eventKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemId = rs.getInt(1);
                    if (!isPermanentNonEventItem(itemId)) {
                        ids.add(itemId);
                    }
                }
            }
        }
        return ids;
    }

    private int purgeGroundItems(Set<Integer> itemIds) {
        if (itemIds.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (nro.models.map.Map map : Manager.MAPS) {
            if (map == null || map.zones == null) {
                continue;
            }
            for (Zone zone : map.zones) {
                for (ItemMap itemMap : new ArrayList<>(zone.items)) {
                    if (itemMap != null && itemMap.itemTemplate != null
                            && itemIds.contains((int) itemMap.itemTemplate.id)) {
                        ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private int purgeOnlinePlayers(String eventKey, Set<Integer> itemIds) {
        int changedPlayers = 0;
        for (Player player : new ArrayList<>(Client.gI().getPlayers())) {
            if (player == null || player.inventory == null) {
                continue;
            }
            int removed = 0;
            removed += purgeItemList(player.inventory.itemsBody, itemIds);
            removed += purgeItemList(player.inventory.itemsBag, itemIds);
            removed += purgeItemList(player.inventory.itemsBox, itemIds);
            removed += purgeItemList(player.inventory.itemsBoxCrackBall, itemIds);
            removed += purgeItemList(player.inventory.itemsDaBan, itemIds);
            if (player.pet != null && player.pet.inventory != null) {
                removed += purgeItemList(player.pet.inventory.itemsBody, itemIds);
            }
            boolean removedWatermelon = HUNG_VUONG.equals(eventKey) && player.DuaHauEgg != null;
            if (removedWatermelon) {
                player.DuaHauEgg.destroyEgg();
            }
            resetItemEventCounters(player, eventKey);
            if (removed > 0 || removedWatermelon) {
                changedPlayers++;
                InventoryService.gI().sendItemBody(player);
                InventoryService.gI().sendItemBags(player);
                InventoryService.gI().sendItemBox(player);
                Service.gI().Send_Caitrang(player);
                if (player.pet != null) {
                    Service.gI().Send_Caitrang(player.pet);
                    Service.gI().showInfoPet(player);
                }
                Service.gI().sendThongBao(player,
                        "Dữ liệu vật phẩm sự kiện vừa được quản trị viên reset.");
            }
            PlayerDAO.updatePlayer(player);
        }
        return changedPlayers;
    }

    private int purgeItemList(List<Item> items, Set<Integer> itemIds) {
        if (items == null || itemIds.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            if (item != null && item.isNotNullItem()
                    && itemIds.contains((int) item.template.id)) {
                items.set(i, ItemService.gI().createItemNull());
                removed++;
            }
        }
        return removed;
    }

    private void resetItemEventCounters(Player player, String eventKey) {
        switch (eventKey) {
            case "childrens_day" -> player.point_sukien = 0;
            case "sugarcane" -> player.point_sukien1 = 0;
            case "fruit_ice_cream" -> player.point_sukien2 = 0;
            case SUMMER -> player.point_summer_cards = 0;
            default -> {
            }
        }
        if (player.itemEvent == null) {
            return;
        }
        player.itemEvent.lastTVGSTime = 0;
        player.itemEvent.lastItemChuongDong = 0;
        player.itemEvent.lastItemBanhQuy = 0;
        player.itemEvent.lastItemCaTuyet = 0;
        player.itemEvent.lastItemKeoDuong = 0;
        player.itemEvent.lastItemKeoNguoiTuyet = 0;
        player.itemEvent.lastItemManhVo = 0;
        player.itemEvent.lastHHTime = 0;
        player.itemEvent.lastBNTime = 0;
        player.itemEvent.remainingTVGSCount = 0;
        player.itemEvent.remainingChuongDongCount = 0;
        player.itemEvent.remainingBanhQuyCount = 0;
        player.itemEvent.remainingCaTuyetCount = 0;
        player.itemEvent.remainingKeoDuongCount = 0;
        player.itemEvent.remainingKeoNguoiTuyetCount = 0;
        player.itemEvent.remainingManhVo = 0;
        player.itemEvent.remainingHHCount = 0;
        player.itemEvent.remainingBNCount = 0;
    }

    private PurgeResult purgeDatabasePlayers(EventCommand command, Set<Integer> itemIds)
            throws SQLException {
        List<PlayerChange> changes = new ArrayList<>();
        String select = "SELECT id, items_body, items_bag, items_box, "
                + "items_box_lucky_round, items_daban, pet, data_duahau_egg FROM player";
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(select);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilterResult body = filterInventoryJson(rs.getString("items_body"), itemIds);
                FilterResult bag = filterInventoryJson(rs.getString("items_bag"), itemIds);
                FilterResult box = filterInventoryJson(rs.getString("items_box"), itemIds);
                FilterResult lucky = filterInventoryJson(rs.getString("items_box_lucky_round"), itemIds);
                FilterResult sold = filterInventoryJson(rs.getString("items_daban"), itemIds);
                FilterResult pet = filterPetJson(rs.getString("pet"), itemIds);
                int removed = body.removed + bag.removed + box.removed
                        + lucky.removed + sold.removed + pet.removed;
                String watermelonData = rs.getString("data_duahau_egg");
                boolean clearWatermelon = HUNG_VUONG.equals(command.eventKey)
                        && watermelonData != null
                        && !watermelonData.isBlank()
                        && !"[]".equals(watermelonData.trim())
                        && !"null".equalsIgnoreCase(watermelonData.trim());
                if (removed > 0 || clearWatermelon) {
                    changes.add(new PlayerChange(
                            rs.getLong("id"),
                            rs.getString("items_body"), rs.getString("items_bag"),
                            rs.getString("items_box"), rs.getString("items_box_lucky_round"),
                            rs.getString("items_daban"), rs.getString("pet"),
                            body.value, bag.value, box.value, lucky.value, sold.value, pet.value,
                            removed));
                }
            }
        }

        int removedSlots = 0;
        try (Connection con = LocalManager.getConnection()) {
            con.setAutoCommit(false);
            String backupSql = "INSERT INTO game_event_player_backup "
                    + "(command_id, player_id, event_key, items_body, items_bag, items_box, "
                    + "items_box_lucky_round, items_daban, pet) VALUES (?,?,?,?,?,?,?,?,?)";
            String updateSql = "UPDATE player SET items_body=?, items_bag=?, items_box=?, "
                    + "items_box_lucky_round=?, items_daban=?, pet=?, data_item_event=? WHERE id=?";
            try (PreparedStatement backup = con.prepareStatement(backupSql);
                    PreparedStatement update = con.prepareStatement(updateSql);
                    PreparedStatement reset = con.prepareStatement(
                            "UPDATE player SET data_item_event=?")) {
                for (PlayerChange change : changes) {
                    backup.setLong(1, command.id);
                    backup.setLong(2, change.playerId);
                    backup.setString(3, command.eventKey);
                    backup.setString(4, change.oldBody);
                    backup.setString(5, change.oldBag);
                    backup.setString(6, change.oldBox);
                    backup.setString(7, change.oldLucky);
                    backup.setString(8, change.oldSold);
                    backup.setString(9, change.oldPet);
                    backup.addBatch();

                    update.setString(1, change.newBody);
                    update.setString(2, change.newBag);
                    update.setString(3, change.newBox);
                    update.setString(4, change.newLucky);
                    update.setString(5, change.newSold);
                    update.setString(6, change.newPet);
                    update.setString(7, EMPTY_EVENT_COUNTERS);
                    update.setLong(8, change.playerId);
                    update.addBatch();
                    removedSlots += change.removed;
                }
                if (!changes.isEmpty()) {
                    backup.executeBatch();
                    update.executeBatch();
                }
                reset.setString(1, EMPTY_EVENT_COUNTERS);
                reset.executeUpdate();
                if (HUNG_VUONG.equals(command.eventKey)) {
                    try (PreparedStatement clearWatermelons = con.prepareStatement(
                            "UPDATE player SET data_duahau_egg='[]'")) {
                        clearWatermelons.executeUpdate();
                    }
                }
                String pointColumn = eventPointColumn(command.eventKey);
                if (pointColumn != null) {
                    try (PreparedStatement resetPoints = con.prepareStatement(
                            "UPDATE player SET " + pointColumn + "=0")) {
                        resetPoints.executeUpdate();
                    }
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
        return new PurgeResult(changes.size(), removedSlots);
    }

    private static String eventPointColumn(String eventKey) {
        return switch (eventKey) {
            case "childrens_day" -> "point_sukien";
            case "sugarcane" -> "point_sukien1";
            case "fruit_ice_cream" -> "point_sukien2";
            case SUMMER -> "point_summer_cards";
            default -> null;
        };
    }

    static FilterResult filterInventoryJson(String raw, Set<Integer> itemIds) {
        if (raw == null || raw.isBlank() || itemIds.isEmpty()) {
            return new FilterResult(raw, 0);
        }
        Object parsed = JSONValue.parse(raw);
        if (!(parsed instanceof JSONArray outer)) {
            return new FilterResult(raw, 0);
        }
        int removed = 0;
        for (int i = 0; i < outer.size(); i++) {
            Object slotRaw = outer.get(i);
            Object slotParsed = slotRaw instanceof String
                    ? JSONValue.parse((String) slotRaw) : slotRaw;
            if (!(slotParsed instanceof JSONArray slot) || slot.isEmpty()) {
                continue;
            }
            int itemId = toInt(slot.get(0), -1);
            if (itemIds.contains(itemId)) {
                JSONArray empty = new JSONArray();
                empty.add(-1L);
                empty.add(0L);
                empty.add("[]");
                empty.add(slot.size() > 3 ? slot.get(3) : 0L);
                outer.set(i, empty.toJSONString());
                removed++;
            }
        }
        return new FilterResult(removed == 0 ? raw : outer.toJSONString(), removed);
    }

    static FilterResult filterPetJson(String raw, Set<Integer> itemIds) {
        if (raw == null || raw.isBlank() || itemIds.isEmpty()) {
            return new FilterResult(raw, 0);
        }
        Object parsed = JSONValue.parse(raw);
        if (!(parsed instanceof JSONArray pet) || pet.size() < 3) {
            return new FilterResult(raw, 0);
        }
        Object body = pet.get(2);
        if (!(body instanceof String)) {
            return new FilterResult(raw, 0);
        }
        FilterResult filtered = filterInventoryJson((String) body, itemIds);
        if (filtered.removed == 0) {
            return new FilterResult(raw, 0);
        }
        pet.set(2, filtered.value);
        return new FilterResult(pet.toJSONString(), filtered.removed);
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void removeEventNpcs(String eventKey) {
        for (List<NpcRule> rules : npcEvents.values()) {
            for (NpcRule rule : rules) {
                if (!rule.managedRuntime || !rule.eventKey.equals(eventKey)
                        || rule.mapId == null) {
                    continue;
                }
                nro.models.map.Map map = MapService.gI().getMapById(rule.mapId);
                if (map == null || map.npcs == null) {
                    continue;
                }
                List<Npc> removed = new ArrayList<>();
                for (Npc npc : new ArrayList<>(map.npcs)) {
                    if (npc != null && npc.tempId == rule.npcId
                            && (rule.x == null || npc.cx == rule.x)
                            && (rule.y == null || npc.cy == rule.y)) {
                        removed.add(npc);
                    }
                }
                map.npcs.removeAll(removed);
                Manager.NPCS.removeAll(removed);
            }
        }
    }

    private void restoreEventNpcs(String eventKey) {
        for (List<NpcRule> rules : npcEvents.values()) {
            for (NpcRule rule : rules) {
                if (!rule.managedRuntime || !rule.eventKey.equals(eventKey)
                        || rule.mapId == null || rule.x == null || rule.y == null) {
                    continue;
                }
                nro.models.map.Map map = MapService.gI().getMapById(rule.mapId);
                if (map == null || map.npcs == null) {
                    continue;
                }
                boolean exists = map.npcs.stream().anyMatch(npc -> npc != null
                        && npc.tempId == rule.npcId && npc.cx == rule.x && npc.cy == rule.y);
                if (!exists) {
                    map.npcs.add(NpcFactory.createNPC(rule.mapId, 1, rule.x, rule.y, rule.npcId));
                }
            }
        }
    }

    private BossManager eventBossManager(String eventKey) {
        return switch (eventKey) {
            case LUNAR_NEW_YEAR -> LunarNewYearEventManager.gI();
            case MID_AUTUMN -> TrungThuEventManager.gI();
            case HALLOWEEN -> HalloweenEventManager.gI();
            case CHRISTMAS -> ChristmasEventManager.gI();
            case HUNG_VUONG -> HungVuongEventManager.gI();
            default -> null;
        };
    }

    private void removeEventBosses(String eventKey) {
        BossManager manager = eventBossManager(eventKey);
        if (manager == null) {
            return;
        }
        for (Boss boss : new ArrayList<>(manager.getBosses())) {
            try {
                if (boss != null && boss.zone != null) {
                    ChangeMapService.gI().exitMap(boss);
                }
            } catch (Exception ignored) {
            }
            manager.removeBoss(boss);
        }
    }

    private void restoreEventBosses(String eventKey) throws SQLException {
        removeEventBosses(eventKey);
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT boss_id, quantity FROM game_event_boss WHERE event_key=? ORDER BY boss_id")) {
            ps.setString(1, eventKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int quantity = Math.min(100, rs.getInt("quantity"));
                    if (quantity > 0) {
                        BossManager.gI().createBoss(rs.getInt("boss_id"), quantity);
                    }
                }
            }
        }
    }

    private void updateCatalogState(String eventKey, boolean enabled, String result)
            throws SQLException {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_event_catalog SET enabled=?, reset_version=reset_version+1, "
                        + "last_action=?, last_result=?, last_changed_at=CURRENT_TIMESTAMP "
                        + "WHERE event_key=?")) {
            ps.setBoolean(1, enabled);
            ps.setString(2, enabled ? "ENABLE" : "DISABLE");
            ps.setString(3, result);
            ps.setString(4, eventKey);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không cập nhật được trạng thái sự kiện " + eventKey);
            }
        }
    }

    private synchronized void setSnapshotState(String eventKey, boolean enabled) {
        Map<String, Boolean> mutable = new HashMap<>(states);
        mutable.put(eventKey, enabled);
        states = Collections.unmodifiableMap(mutable);
    }

    private boolean claimCommand(long id) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_event_command SET status='PROCESSING', started_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND status='PENDING'")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void finishCommand(long id, boolean success, String message) {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE game_event_command SET status=?, result_message=?, "
                        + "finished_at=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setString(1, success ? "DONE" : "FAILED");
            ps.setString(2, trim(message, 500));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error("Không cập nhật được kết quả lệnh event #" + id + "\n");
        }
    }

    private void announce(String eventKey, boolean enabled) {
        String message = (enabled ? "Đã bật" : "Đã tắt")
                + " sự kiện " + eventKey + ". Dữ liệu vật phẩm sự kiện đã được reset.";
        ChatGlobalService.gI().chatAdmin(message);
        Service.gI().sendThongBaoAllPlayer("Admin: " + message);
    }

    private static String compactError(Throwable error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        if (message == null || message.isBlank()) {
            message = error == null ? "Unknown error" : error.getClass().getSimpleName();
        }
        return trim(message.replace('\r', ' ').replace('\n', ' '), 500);
    }

    private static String trim(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    static record FilterResult(String value, int removed) {
    }

    private record NpcRule(String eventKey, int npcId, Integer mapId,
            Integer x, Integer y, boolean managedRuntime) {
    }

    private record EventCommand(long id, String eventKey, boolean targetEnabled) {
    }

    private record PurgeResult(int changedPlayers, int removedSlots) {
    }

    private record PlayerChange(long playerId,
            String oldBody, String oldBag, String oldBox, String oldLucky,
            String oldSold, String oldPet,
            String newBody, String newBag, String newBox, String newLucky,
            String newSold, String newPet, int removed) {
    }
}
