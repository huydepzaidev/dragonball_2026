package nro.models.npc_list;

public final class QuyLaoKameKOLMenuTest {

    private QuyLaoKameKOLMenuTest() {
    }

    public static void main(String[] args) {
        require(QuyLaoKame.kolBaseMenuLabel(false).equals(\u0022Nhiệm vụ\nKOL\u0022));
        require(QuyLaoKame.kolBaseMenuLabel(true).equals(\u0022Nhận quà\u0022));
        require(!QuyLaoKame.isKOLQuestComplete(9, 10));
        require(QuyLaoKame.isKOLQuestComplete(10, 10));
        require(QuyLaoKame.isKOLQuestComplete(11, 10));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
