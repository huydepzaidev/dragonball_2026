package nro.models.boss;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.data.LocalManager;
import nro.models.map.ItemMap;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Logger;

/**
 * Plans divine drops once per boss turn and assigns them to forms/members.
 * The 12h and 14h encounters deliberately keep their own reward systems.
 */
public final class DivineTurnDropService {

    private static final DivineTurnDropService INSTANCE = new DivineTurnDropService();
    private static final long CONFIG_CACHE_MS = 5_000L;

    private final Map<Boss, TurnState> activeTurns
            = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<PityKey, Integer> fallbackPity = new HashMap<>();
    private volatile Config config = Config.defaults();
    private volatile long configLoadedAt;
    private volatile boolean databaseErrorReported;

    private DivineTurnDropService() {
    }

    public static DivineTurnDropService gI() {
        return INSTANCE;
    }

    public void onBossDefeated(Boss boss, Player killer) {
        Player owner = resolveRewardOwner(killer);
        EncounterMember member = classify(boss);
        if (owner == null || member == null || boss.zone == null) {
            return;
        }
        Config currentConfig = loadConfigIfNeeded();
        if (!currentConfig.enabled()) {
            return;
        }

        int rewardCount;
        int planned;
        synchronized (activeTurns) {
            TurnState state = activeTurns.get(member.root());
            if (state == null || !state.matches(member)
                    || state.defeated()[member.memberIndex()]) {
                PityKey pityKey = new PityKey(member.encounterKey(),
                        boss.zone.map == null ? -1 : boss.zone.map.mapId,
                        boss.zone.zoneId);
                planned = planDropCount(member.memberCount(), pityKey, currentConfig.rates());
                state = TurnState.create(member, planned);
                activeTurns.put(member.root(), state);
                Logger.successln("[DIVINE TURN PLAN] encounter=" + member.encounterKey()
                        + " map=" + pityKey.mapId() + " zone=" + pityKey.zoneId()
                        + " members=" + member.memberCount() + " drops=" + planned);
            } else {
                planned = state.plannedDrops();
            }
            rewardCount = state.rewards()[member.memberIndex()];
            state.defeated()[member.memberIndex()] = true;
            if (state.isComplete()) {
                activeTurns.remove(member.root());
            }
        }

        for (int i = 0; i < rewardCount; i++) {
            dropDivine(boss, owner, member.encounterKey(), planned, i + 1);
        }
    }

    private Config loadConfigIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - configLoadedAt < CONFIG_CACHE_MS) {
            return config;
        }
        synchronized (this) {
            if (now - configLoadedAt < CONFIG_CACHE_MS) {
                return config;
            }
            try (Connection con = LocalManager.getConnection();
                    PreparedStatement ps = con.prepareStatement(
                            "SELECT enabled, one_zero_bp, one_one_bp, one_two_bp, "
                            + "two_zero_bp, two_one_bp, two_two_bp, multi_zero_bp, "
                            + "multi_one_bp, multi_two_bp, multi_three_bp, pity_blank_turns "
                            + "FROM game_divine_turn_config WHERE id=1");
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DivineTurnDropPolicy.Rates rates = new DivineTurnDropPolicy.Rates(
                            rs.getInt("one_zero_bp"), rs.getInt("one_one_bp"),
                            rs.getInt("one_two_bp"), rs.getInt("two_zero_bp"),
                            rs.getInt("two_one_bp"), rs.getInt("two_two_bp"),
                            rs.getInt("multi_zero_bp"), rs.getInt("multi_one_bp"),
                            rs.getInt("multi_two_bp"), rs.getInt("multi_three_bp"),
                            rs.getInt("pity_blank_turns"));
                    config = new Config(rs.getBoolean("enabled"),
                            rates.isValid() ? rates : DivineTurnDropPolicy.DEFAULT_RATES);
                }
                databaseErrorReported = false;
            } catch (Exception error) {
                reportDatabaseErrorOnce("load config", error);
            }
            configLoadedAt = now;
            return config;
        }
    }

    private int planDropCount(int memberCount, PityKey key,
            DivineTurnDropPolicy.Rates rates) {
        int roll = ThreadLocalRandom.current().nextInt(10_000);
        try (Connection con = LocalManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement insert = con.prepareStatement(
                        "INSERT IGNORE INTO game_divine_turn_pity "
                        + "(encounter_key,map_id,zone_id,blank_turns) VALUES (?,?,?,0)")) {
                    bindKey(insert, key);
                    insert.executeUpdate();
                }
                int blankTurns = 0;
                try (PreparedStatement select = con.prepareStatement(
                        "SELECT blank_turns FROM game_divine_turn_pity "
                        + "WHERE encounter_key=? AND map_id=? AND zone_id=? FOR UPDATE")) {
                    bindKey(select, key);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            blankTurns = rs.getInt(1);
                        }
                    }
                }
                int count = DivineTurnDropPolicy.dropCount(memberCount, roll, blankTurns, rates);
                try (PreparedStatement update = con.prepareStatement(
                        "UPDATE game_divine_turn_pity SET blank_turns=?, "
                        + "updated_at=CURRENT_TIMESTAMP WHERE encounter_key=? "
                        + "AND map_id=? AND zone_id=?")) {
                    update.setInt(1, count == 0 ? Math.min(100, blankTurns + 1) : 0);
                    update.setString(2, key.encounterKey());
                    update.setInt(3, key.mapId());
                    update.setInt(4, key.zoneId());
                    update.executeUpdate();
                }
                con.commit();
                databaseErrorReported = false;
                return count;
            } catch (Exception error) {
                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
                throw error;
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception error) {
            reportDatabaseErrorOnce("update pity", error);
            synchronized (fallbackPity) {
                int blankTurns = fallbackPity.getOrDefault(key, 0);
                int count = DivineTurnDropPolicy.dropCount(memberCount, roll, blankTurns, rates);
                fallbackPity.put(key, count == 0 ? Math.min(100, blankTurns + 1) : 0);
                return count;
            }
        }
    }

    private static void bindKey(PreparedStatement ps, PityKey key) throws Exception {
        ps.setString(1, key.encounterKey());
        ps.setInt(2, key.mapId());
        ps.setInt(3, key.zoneId());
    }

    private void reportDatabaseErrorOnce(String phase, Exception error) {
        if (!databaseErrorReported) {
            databaseErrorReported = true;
            Logger.error("Divine turn drop cannot " + phase
                    + "; using safe in-memory defaults: " + error.getMessage() + "\n");
        }
    }

    private static void dropDivine(Boss boss, Player owner, String encounter,
            int planned, int sequence) {
        try {
            int x = boss.location == null ? 0 : boss.location.x;
            if (sequence > 1) {
                x += ThreadLocalRandom.current().nextInt(-25, 26);
            }
            int rawY = boss.location == null ? 0 : boss.location.y - 24;
            int y = boss.zone.map == null ? rawY : boss.zone.map.yPhysicInTop(x, rawY);
            ItemMap item = ItemService.gI().randDoTLBoss(boss.zone, 1, x, y, owner.id);
            if (item == null || item.itemTemplate == null) {
                return;
            }
            Service.gI().dropItemMap(boss.zone, item);
            Logger.successln("[DIVINE TURN DROP] encounter=" + encounter
                    + " boss=" + boss.id + " item=" + item.itemTemplate.id
                    + " owner=" + owner.id + " planned=" + planned
                    + " sequence=" + sequence);
        } catch (Exception error) {
            Logger.error("Divine turn drop failed for boss " + boss.id
                    + ": " + error.getMessage() + "\n");
        }
    }

    private static Player resolveRewardOwner(Player attacker) {
        if (attacker == null || attacker.isBot) {
            return null;
        }
        if (attacker instanceof Pet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        if (attacker instanceof NewPet pet) {
            return pet.master != null && !pet.master.isBot ? pet.master : null;
        }
        return attacker.isPl() ? attacker : null;
    }

    private static EncounterMember classify(Boss boss) {
        if (boss == null || isExcludedTimedEncounter(boss)) {
            return null;
        }
        Boss root = boss.parentBoss == null ? boss : boss.parentBoss;
        int id = (int) boss.id;
        if (!isDivineEligibleBossId(id)) {
            return null;
        }
        return switch (id) {
            case BossID.KUKU -> member("KUKU", boss, 1, 0);
            case BossID.MAP_DAU_DINH -> member("MAP_DAU_DINH", boss, 1, 0);
            case BossID.RAMBO -> member("RAMBO", boss, 1, 0);
            case BossID.SO_4 -> member("NAPPA_SQUAD", root, 5, 0);
            case BossID.SO_3 -> member("NAPPA_SQUAD", root, 5, 1);
            case BossID.SO_2 -> member("NAPPA_SQUAD", root, 5, 2);
            case BossID.SO_1 -> member("NAPPA_SQUAD", root, 5, 3);
            case BossID.TIEU_DOI_TRUONG -> member("NAPPA_SQUAD", root, 5, 4);
            case BossID.SO_4_NM -> member("NAMEK_ASSASSIN_SQUAD", root, 5, 0);
            case BossID.SO_3_NM -> member("NAMEK_ASSASSIN_SQUAD", root, 5, 1);
            case BossID.SO_2_NM -> member("NAMEK_ASSASSIN_SQUAD", root, 5, 2);
            case BossID.SO_1_NM -> member("NAMEK_ASSASSIN_SQUAD", root, 5, 3);
            case BossID.TIEU_DOI_TRUONG_NM -> member("NAMEK_ASSASSIN_SQUAD", root, 5, 4);
            case BossID.FIDE -> member("FIDE", boss, 3, boss.currentLevel);
            case BossID.COOLER -> member("COOLER", boss, 2, boss.currentLevel);
            case BossID.ANDROID_19 -> member("ANDROID_19_20", root, 2, 1);
            case BossID.DR_KORE -> member("ANDROID_19_20", root, 2, 0);
            case BossID.PIC -> member("PIC_POC_KING_KONG", root, 3, 1);
            case BossID.POC -> member("PIC_POC_KING_KONG", root, 3, 2);
            case BossID.KING_KONG -> member("PIC_POC_KING_KONG", root, 3, 0);
            case BossID.XEN_BO_HUNG -> member("XEN_GINDER", boss, 3, boss.currentLevel);
            case BossID.SIEU_BO_HUNG -> member("XEN_ARENA", boss, 9, boss.currentLevel);
            case BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3,
                    BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6,
                    BossID.XEN_CON_7 -> member("XEN_ARENA", root, 9,
                            2 + BossID.XEN_CON_1 - id);
            case BossID.BLACK_GOKU -> member("BLACK_GOKU", boss, 2, boss.currentLevel);
            case BossID.CUMBER -> member("CUMBER", boss, 2, boss.currentLevel);
            case BossID.BABY -> member("BABY", boss, 3, boss.currentLevel);
            case BossID.GOD_BILL -> member("BILL_WHIS", root, 2, 0);
            case BossID.ANGEL_WHIS -> member("BILL_WHIS", root, 2, 1);
            case BossID.GOD_CHAMPA -> member("CHAMPA_VADOS", root, 2, 0);
            case BossID.ANGEL_VADOS -> member("CHAMPA_VADOS", root, 2, 1);
            case BossID.PILAP -> member("PILAP_SQUAD", root, 3, 0);
            case BossID.MAI_PILAP -> member("PILAP_SQUAD", root, 3, 1);
            case BossID.PU_PILAP -> member("PILAP_SQUAD", root, 3, 2);
            case BossID.SOI_DO_VO_TINH -> member("THREE_WOLVES", root, 3, 0);
            case BossID.SOI_VANG_VO_TINH -> member("THREE_WOLVES", root, 3, 1);
            case BossID.SOI_XANH_XAM_VO_TINH -> member("THREE_WOLVES", root, 3, 2);
            case BossID.ZAMASU -> member("ZAMASU", boss, 1, 0);
            default -> null;
        };
    }

    private static boolean isExcludedTimedEncounter(Boss boss) {
        String className = boss.getClass().getName();
        return className.startsWith("nro.models.boss.MajinBuu_12h.")
                || className.startsWith("nro.models.boss.MajinBuu_14h.");
    }

    static boolean isDivineEligibleBossId(int bossId) {
        return switch (bossId) {
            case BossID.COOLER,
                    BossID.XEN_BO_HUNG, BossID.SIEU_BO_HUNG,
                    BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3,
                    BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6,
                    BossID.XEN_CON_7,
                    BossID.BLACK_GOKU, BossID.CUMBER, BossID.BABY,
                    BossID.GOD_BILL, BossID.ANGEL_WHIS,
                    BossID.GOD_CHAMPA, BossID.ANGEL_VADOS,
                    BossID.PILAP, BossID.MAI_PILAP, BossID.PU_PILAP,
                    BossID.SOI_DO_VO_TINH, BossID.SOI_VANG_VO_TINH,
                    BossID.SOI_XANH_XAM_VO_TINH, BossID.ZAMASU -> true;
            default -> false;
        };
    }

    private static EncounterMember member(String key, Boss root, int count, int index) {
        if (root == null || index < 0 || index >= count) {
            return null;
        }
        return new EncounterMember(key, root, count, index);
    }

    private record Config(boolean enabled, DivineTurnDropPolicy.Rates rates) {
        private static Config defaults() {
            return new Config(true, DivineTurnDropPolicy.DEFAULT_RATES);
        }
    }

    private record EncounterMember(String encounterKey, Boss root,
            int memberCount, int memberIndex) {
    }

    private record PityKey(String encounterKey, int mapId, int zoneId) {
    }

    private record TurnState(String encounterKey, int memberCount, long turnSequence,
            int plannedDrops, int[] rewards, boolean[] defeated) {

        private static TurnState create(EncounterMember member, int plannedDrops) {
            int[] rewards = new int[member.memberCount()];
            if (member.memberCount() == 1) {
                rewards[0] = plannedDrops;
            } else {
                int[] indexes = new int[member.memberCount()];
                for (int i = 0; i < indexes.length; i++) {
                    indexes[i] = i;
                }
                for (int i = indexes.length - 1; i > 0; i--) {
                    int selected = ThreadLocalRandom.current().nextInt(i + 1);
                    int value = indexes[i];
                    indexes[i] = indexes[selected];
                    indexes[selected] = value;
                }
                for (int i = 0; i < Math.min(plannedDrops, indexes.length); i++) {
                    rewards[indexes[i]]++;
                }
            }
            return new TurnState(member.encounterKey(), member.memberCount(),
                    member.root().getDivineTurnSequence(),
                    plannedDrops, rewards, new boolean[member.memberCount()]);
        }

        private boolean matches(EncounterMember member) {
            return encounterKey.equals(member.encounterKey())
                    && memberCount == member.memberCount()
                    && turnSequence == member.root().getDivineTurnSequence();
        }

        private boolean isComplete() {
            for (boolean value : defeated) {
                if (!value) {
                    return false;
                }
            }
            return true;
        }
    }
}
