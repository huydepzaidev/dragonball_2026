package nro.models.services;

import nro.models.consts.ConstMob;
import nro.models.consts.ConstMap;
import nro.models.consts.ConstTask;
import nro.models.mob.Mob;
import nro.models.player.Player;

public class KOLQuestService {

    public static final int STAGE_WOOD_DUMMY = 1;
    public static final int STAGE_BIRD_DEMON = 2;
    public static final int STAGE_FIDE_WAVE = 3;
    public static final int STAGE_CHALLENGE = 4;
    public static final int STAGE_HARD_DAILY_TASK = 5;
    public static final int STAGE_SUPER_BROLY = 6;

    public static final long REQUIRED_WOOD_DUMMY = 1_000;
    public static final long REQUIRED_BIRD_DEMON = 10_000;
    public static final long REQUIRED_FIDE_WAVE = 10;
    public static final long REQUIRED_CHALLENGE = 100;
    public static final long REQUIRED_HARD_DAILY_TASK = 20;
    public static final long REQUIRED_SUPER_BROLY = 20;

    private static KOLQuestService instance;

    public static KOLQuestService gI() {
        if (instance == null) {
            instance = new KOLQuestService();
        }
        return instance;
    }

    public void recordAutoTrainMobKill(Player player, Mob mob) {
        if (player == null || mob == null || !player.isPl()
                || player.itemTime == null || !player.itemTime.isUseTDLT) {
            return;
        }

        if (player.kolQuestStage == STAGE_WOOD_DUMMY && mob.tempId == ConstMob.MOC_NHAN) {
            player.kolWoodDummyAutoTrainKills = incrementUpTo(
                    player.kolWoodDummyAutoTrainKills, REQUIRED_WOOD_DUMMY);
        } else if (player.kolQuestStage == STAGE_BIRD_DEMON
                && mob.tempId == ConstMob.QUY_CHIM
                && mob.zone != null && mob.zone.map != null
                && mob.zone.map.mapId == ConstMap.HANG_QUY_CHIM) {
            player.kolBirdDemonAutoTrainKills = incrementUpTo(
                    player.kolBirdDemonAutoTrainKills, REQUIRED_BIRD_DEMON);
        }
    }

    public boolean isDoingFideWaveQuest(Player player) {
        return player != null && player.isPl() && player.kolQuestStage == STAGE_FIDE_WAVE;
    }

    public void recordFideWaveCompletion(Player player) {
        if (isDoingFideWaveQuest(player)) {
            player.kolFideWaveCompletions = incrementUpTo(
                    player.kolFideWaveCompletions, REQUIRED_FIDE_WAVE);
        }
    }

    public void recordChallengeWin(Player player) {
        if (player != null && player.isPl() && player.kolQuestStage == STAGE_CHALLENGE) {
            player.kolChallengeWins = incrementUpTo(player.kolChallengeWins, REQUIRED_CHALLENGE);
        }
    }

    public void recordHardDailyTaskCompletion(Player player, int taskLevel) {
        if (player != null && player.isPl() && player.kolQuestStage == STAGE_HARD_DAILY_TASK
                && taskLevel == ConstTask.HARD) {
            player.kolHardDailyQuestCompletions = incrementUpTo(
                    player.kolHardDailyQuestCompletions, REQUIRED_HARD_DAILY_TASK);
        }
    }

    public void recordSuperBrolyDefeat(Player player) {
        if (player != null && player.isPl() && player.kolQuestStage == STAGE_SUPER_BROLY) {
            player.kolSuperBrolyDefeats = incrementUpTo(
                    player.kolSuperBrolyDefeats, REQUIRED_SUPER_BROLY);
        }
    }

    public long getProgress(Player player, int stage) {
        return switch (stage) {
            case STAGE_WOOD_DUMMY -> player.kolWoodDummyAutoTrainKills;
            case STAGE_BIRD_DEMON -> player.kolBirdDemonAutoTrainKills;
            case STAGE_FIDE_WAVE -> player.kolFideWaveCompletions;
            case STAGE_CHALLENGE -> player.kolChallengeWins;
            case STAGE_HARD_DAILY_TASK -> player.kolHardDailyQuestCompletions;
            case STAGE_SUPER_BROLY -> player.kolSuperBrolyDefeats;
            default -> 0;
        };
    }

    private long incrementUpTo(long currentValue, long maximum) {
        return currentValue < maximum ? currentValue + 1 : maximum;
    }
}
