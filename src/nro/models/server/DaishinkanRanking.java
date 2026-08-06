package nro.models.server;

import nro.models.matches.TOP;
import nro.models.utils.Util;

/** Display policy for the live Daishinkan rankings. */
final class DaishinkanRanking {

    static final int LIMIT = 10;

    private DaishinkanRanking() {
    }

    static void showPower(TOP top, long power) {
        String text = Util.formatCompactVietnamese(power) + " sức mạnh";
        top.setInfo1(text);
        top.setInfo2(text);
    }

    static void showTaskScore(TOP top, int taskId) {
        show(top, "Nhiệm vụ số " + taskId, taskId);
    }

    static void showSummerEventScore(TOP top, long score) {
        showPoints(top, score);
    }

    static void showBossScore(TOP top, long score) {
        showPoints(top, score);
    }

    private static void showPoints(TOP top, long score) {
        show(top, score + " điểm", score);
    }

    private static void show(TOP top, String text, long compareValue) {
        top.setHiddenScore(false);
        top.setParamCompare(compareValue);
        top.setInfo1(text);
        top.setInfo2(text);
    }
}
