package nro.models.mob;

import nro.models.utils.Util;

/**
 * Pure balancing rules for naturally spawned super mobs.
 */
public final class NaturalSuperMobPolicy {

    public static final int MIN_BASE_HP = 3_000;
    public static final int MAX_BASE_HP_EXCLUSIVE = 10_000_000;
    public static final int MIN_SUPER_HP = 30_000;
    public static final int MAX_SUPER_HP = 10_000_000;
    public static final int EXP_MULTIPLIER = 3;

    private NaturalSuperMobPolicy() {
    }

    public static boolean isEligibleBaseHp(int baseHp) {
        return baseHp >= MIN_BASE_HP && baseHp < MAX_BASE_HP_EXCLUSIVE;
    }

    public static int requiredNormalKills(int playerCount) {
        if (playerCount >= 7) {
            return 10;
        }
        if (playerCount >= 4) {
            return 12;
        }
        if (playerCount >= 2) {
            return 14;
        }
        return 15;
    }

    public static int maxConcurrentSupers(int playerCount) {
        if (playerCount >= 7) {
            return 3;
        }
        if (playerCount >= 4) {
            return 2;
        }
        return 1;
    }

    public static int minSuperHp(int baseHp) {
        long hp = Math.max((long) MIN_SUPER_HP, (long) baseHp * 8L);
        return (int) Math.min(hp, (long) MAX_SUPER_HP);
    }

    public static int maxSuperHp(int baseHp) {
        long hp = Math.max((long) minSuperHp(baseHp), (long) baseHp * 12L);
        return (int) Math.min(hp, (long) MAX_SUPER_HP);
    }

    public static int rollSuperHp(int baseHp) {
        int minHp = minSuperHp(baseHp);
        int maxHp = maxSuperHp(baseHp);
        return minHp == maxHp ? minHp : Util.nextInt(minHp, maxHp);
    }

    /**
     * Returns the final per-hit damage ceiling requested for a super mob.
     * Values between 30k and 1m HP are interpolated to avoid a sudden jump.
     */
    public static int damageCapForHp(int superHp) {
        if (superHp <= MIN_SUPER_HP) {
            return 1_000;
        }
        if (superHp < 1_000_000) {
            long scaled = 1_000L
                    + ((long) superHp - MIN_SUPER_HP) * 99_000L / 970_000L;
            return (int) scaled;
        }
        return Math.min(1_000_000, superHp / 10);
    }

    public static long multiplyExperience(long experience) {
        if (experience <= 0) {
            return experience;
        }
        if (experience > Long.MAX_VALUE / EXP_MULTIPLIER) {
            return Long.MAX_VALUE;
        }
        return experience * EXP_MULTIPLIER;
    }

    public static int randomAuraLevel() {
        return Util.nextInt(1, 3);
    }
}
