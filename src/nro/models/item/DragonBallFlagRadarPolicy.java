package nro.models.item;

/**
 * Reward pools and permanent rates for the two Dragon Ball flag radars.
 */
public final class DragonBallFlagRadarPolicy {

    public static final short NORMAL_RADAR_ITEM_ID = 1822;
    public static final short VIP_RADAR_ITEM_ID = 1823;
    public static final int NORMAL_PERMANENT_RATE_PERCENT = 1;
    public static final int VIP_PERMANENT_RATE_PERCENT = 10;
    public static final int LIMITED_DURATION_DAYS = 15;

    private static final short[] NORMAL_REWARDS = {
        2008, 2009, 2010, 2011, 2012, 2013, 2014
    };
    private static final short[] VIP_REWARDS = {
        2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015
    };

    private DragonBallFlagRadarPolicy() {
    }

    public static short radarItemId(boolean vip) {
        return vip ? VIP_RADAR_ITEM_ID : NORMAL_RADAR_ITEM_ID;
    }

    public static short rewardId(boolean vip, int roll) {
        short[] rewards = vip ? VIP_REWARDS : NORMAL_REWARDS;
        return rewards[Math.floorMod(roll, rewards.length)];
    }

    public static boolean isPermanent(boolean vip, int percentRoll) {
        int rate = vip ? VIP_PERMANENT_RATE_PERCENT : NORMAL_PERMANENT_RATE_PERCENT;
        return Math.floorMod(percentRoll, 100) < rate;
    }

    public static short[] rewardIds(boolean vip) {
        return (vip ? VIP_REWARDS : NORMAL_REWARDS).clone();
    }
}
