package nro.models.item;

import java.util.Arrays;

public final class DragonBallFlagOptionPolicyTest {

    private DragonBallFlagOptionPolicyTest() {
    }

    public static void main(String[] args) {
        assertOptions(2008, pair(50, 19), pair(77, 19), pair(103, 19),
                pair(14, 10), pair(5, 10), pair(30, 0), pair(231, 0));
        assertOptions(2009, pair(50, 18), pair(77, 18), pair(103, 18),
                pair(108, 10), pair(94, 10), pair(30, 0), pair(231, 0));
        assertOptions(2010, pair(50, 17), pair(77, 17), pair(103, 17),
                pair(108, 5), pair(94, 5), pair(30, 0), pair(231, 0));
        assertOptions(2011, pair(50, 16), pair(77, 16), pair(103, 16),
                pair(14, 5), pair(5, 5), pair(30, 0), pair(231, 0));
        assertOptions(2012, pair(50, 15), pair(77, 15), pair(103, 15),
                pair(14, 5), pair(108, 5), pair(30, 0), pair(231, 0));
        assertOptions(2013, pair(50, 13), pair(77, 13), pair(103, 13),
                pair(108, 5), pair(30, 0), pair(231, 0));
        assertOptions(2014, pair(50, 12), pair(77, 12), pair(103, 12),
                pair(14, 5), pair(30, 0), pair(231, 0));
        assertOptions(2015, pair(50, 22), pair(77, 22), pair(103, 22),
                pair(14, 10), pair(108, 10), pair(5, 15), pair(30, 0), pair(231, 0));
        assertOptions(2007);
        assertOptions(2016);
    }

    private static int[] pair(int optionId, int param) {
        return new int[]{optionId, param};
    }

    private static void assertOptions(int itemId, int[]... expected) {
        int[][] actual = DragonBallFlagOptionPolicy.optionsFor((short) itemId);
        if (!Arrays.deepEquals(expected, actual)) {
            throw new AssertionError();
        }
    }
}
