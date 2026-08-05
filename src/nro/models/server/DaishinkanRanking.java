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

    static void hideTaskScore(TOP top) {
        hide(top, " - Ẩn NV", "Xếp hạng theo tiến độ nhiệm vụ");
    }

    static void hideSummerEventScore(TOP top) {
        hide(top, " - Ẩn điểm", "Xếp hạng sự kiện hè");
    }

    static void hideBossScore(TOP top) {
        hide(top, " - Ẩn Boss", "Xếp hạng theo số Boss đã tiêu diệt");
    }

    private static void hide(TOP top, String info1, String info2) {
        top.setHiddenScore(true);
        top.setParamCompare(0L);
        top.setInfo1(info1);
        top.setInfo2(info2);
    }
}