package nro.models.item;

import java.util.Arrays;

public final class DragonBallFlagRadarPolicyTest {

    private DragonBallFlagRadarPolicyTest() {
    }

    public static void main(String[] args) {
        require(Arrays.equals(
                new short[]{2008, 2009, 2010, 2011, 2012, 2013, 2014},
                DragonBallFlagRadarPolicy.rewardIds(false)));
        require(Arrays.equals(
                new short[]{2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015},
                DragonBallFlagRadarPolicy.rewardIds(true)));

        require(DragonBallFlagRadarPolicy.isPermanent(false, 0));
        require(!DragonBallFlagRadarPolicy.isPermanent(false, 1));
        require(DragonBallFlagRadarPolicy.isPermanent(true, 9));
        require(!DragonBallFlagRadarPolicy.isPermanent(true, 10));

        for (int roll = 0; roll < 700; roll++) {
            require(DragonBallFlagRadarPolicy.rewardId(false, roll) != 2015);
        }
        boolean vipCanRewardSuper = false;
        for (int roll = 0; roll < 8; roll++) {
            vipCanRewardSuper |= DragonBallFlagRadarPolicy.rewardId(true, roll) == 2015;
        }
        require(vipCanRewardSuper);
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
