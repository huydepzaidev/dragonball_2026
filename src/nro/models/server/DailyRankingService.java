package nro.models.server;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;

public final class DailyRankingService {
    public static final String TYPE_BOSS = "BOSS";
    public static final String TYPE_SUMMER_EVENT = "SUMMER_EVENT";
    public static final long BOSS_KILL_POINTS = 50L;
    private static final String UPSERT = "INSERT INTO daily_ranking_score "
            + "(ranking_date, ranking_type, player_id, score) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE score = score + VALUES(score)";

    private DailyRankingService() {
    }

    public static void recordBossDefeat(Boss boss, Player killer) {
        if (boss == null || killer == null || !killer.isPl()) {
            return;
        }
        if (isEligibleBossId((int) boss.id)) {
            addScore(killer, TYPE_BOSS, BOSS_KILL_POINTS);
        }
    }

    public static void recordSummerEventPoints(Player player, long points) {
        addScore(player, TYPE_SUMMER_EVENT, points);
    }

    private static void addScore(Player player, String type, long points) {
        if (player == null || !player.isPl() || points <= 0) {
            return;
        }
        try {
            LocalDate today = LocalDate.now(TimeUtil.VIETNAM_ZONE);
            Date weekStart = Date.valueOf(getWeekStart(today));
            LocalManager.executeUpdate(UPSERT, weekStart, type, player.id, points);
        } catch (Exception e) {
            Logger.logException(DailyRankingService.class, e);
        }
    }

    static LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static boolean isEligibleBossId(int bossId) {
        return switch (bossId) {
            case BossID.SIEU_BO_HUNG,
                    BossID.XEN_CON_1,
                    BossID.XEN_CON_2,
                    BossID.XEN_CON_3,
                    BossID.XEN_CON_4,
                    BossID.XEN_CON_5,
                    BossID.XEN_CON_6,
                    BossID.XEN_CON_7,
                    BossID.BLACK_GOKU,
                    BossID.CUMBER,
                    BossID.GOD_BILL,
                    BossID.GOD_CHAMPA,
                    BossID.PILAP,
                    BossID.MAI_PILAP,
                    BossID.PU_PILAP,
                    BossID.SOI_DO_VO_TINH,
                    BossID.SOI_VANG_VO_TINH,
                    BossID.SOI_XANH_XAM_VO_TINH,
                    BossID.CHILL_1,
                    BossID.CHILL_2,
                    BossID.COOLER,
                    BossID.ZAMASU -> true;
            default -> false;
        };
    }
}
