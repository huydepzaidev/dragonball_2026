package nro.models.npc_list;

public final class HomeNpcRubyGuideTest {

    private HomeNpcRubyGuideTest() {
    }

    public static void main(String[] args) {
        String guide = OngGohan.rubyGuideText();
        require(guide.contains(\u0022Số 1 Namek\u0022));
        require(guide.contains(\u0022Tiểu đội trưởng Namek\u0022));
        require(guide.contains(\u0022Bojack\u0022));
        require(guide.contains(\u0022Siêu Bojack\u0022));
        require(guide.contains(\u0022Ăn Trộm\u0022));
        require(guide.contains(\u0022Mặt Trời\u0022));
        require(guide.contains(\u0022Ở Dơ\u0022));
        require(guide.contains(\u002210 capsule mỗi boss\u0022));
        require(guide.contains(\u0022ngẫu nhiên 1-5 capsule mỗi boss\u0022));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
