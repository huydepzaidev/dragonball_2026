package nro.models.map.phoban;

public final class TreasureMapPolicy {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 110;
    public static final int MAX_EXP_DAME = 500_000;
    public static final int EXP_MULTIPLIER = 5;
    public static final int NORMAL_HP_AT_MAX_LEVEL = 200_000_000;
    public static final int SUPER_HP_AT_MAX_LEVEL = 300_000_000;

    private TreasureMapPolicy() {
    }

    public static boolean isTreasureMap(int mapId) {
        return mapId >= 135 && mapId <= 138;
    }

    public static boolean canEnterTreasureMap(int currentMapId, boolean approvedNpcEntry) {
        return isTreasureMap(currentMapId) || approvedNpcEntry;
    }

    public static int mobMaxHp(int level, boolean superMob) {
        int safeLevel = Math.max(MIN_LEVEL, Math.min(level, MAX_LEVEL));
        int maxLevelHp = superMob ? SUPER_HP_AT_MAX_LEVEL : NORMAL_HP_AT_MAX_LEVEL;
        return (int) ((long) maxLevelHp * safeLevel / MAX_LEVEL);
    }

    public static boolean canReceiveExperience(int attackerDame) {
        return attackerDame >= 0 && attackerDame <= MAX_EXP_DAME;
    }

    public static long cappedRewardDamage(long damage) {
        return Math.max(0L, Math.min(damage, MAX_EXP_DAME));
    }

    public static long baseExperience(long damage, int mobMaxHp) {
        return cappedRewardDamage(damage) + Math.max(0, mobMaxHp) / 2_000L;
    }

    public static long multiplyMapExperience(long experience) {
        if (experience <= 0) {
            return 0;
        }
        if (experience > Integer.MAX_VALUE / EXP_MULTIPLIER) {
            return Integer.MAX_VALUE;
        }
        return experience * EXP_MULTIPLIER;
    }

    public static int toIntExperience(long experience) {
        if (experience <= 0) {
            return 0;
        }
        return (int) Math.min(experience, Integer.MAX_VALUE);
    }
}
