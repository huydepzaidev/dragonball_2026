package nro.models.item;

import nro.models.consts.ConstItem;

public final class SuperItemTimeStackTest {

    private SuperItemTimeStackTest() {
    }

    public static void main(String[] args) {
        ItemTime itemTime = new ItemTime(null);
        long now = 1_000_000_000L;
        int[] itemIds = {
            ConstItem.CUONG_NO_2,
            ConstItem.BO_KHI_2,
            ConstItem.BO_HUYET_2,
            ConstItem.GIAP_XEN_BO_HUNG_2,
            ConstItem.AN_DANH_2
        };

        for (int itemId : itemIds) {
            for (int use = 0; use < 12; use++) {
                if (itemTime.addSuperItemTime(itemId, now) != ItemTime.TIME_SUPER_ITEM_PER_USE) {
                    throw new AssertionError();
                }
            }
            if (itemTime.getRemainingSuperItemTime(itemId, now) != ItemTime.MAX_TIME_SUPER_ITEM) {
                throw new AssertionError();
            }
            if (itemTime.addSuperItemTime(itemId, now) != 0) {
                throw new AssertionError();
            }
        }

        long elapsed = 7 * 60_000L;
        if (itemTime.getRemainingSuperItemTime(ConstItem.CUONG_NO_2, now + elapsed)
                != ItemTime.MAX_TIME_SUPER_ITEM - elapsed) {
            throw new AssertionError();
        }
        if (itemTime.addSuperItemTime(ConstItem.CUONG_NO_2, now + elapsed) != 0) {
            throw new AssertionError();
        }

        ItemTime restored = new ItemTime(null);
        long savedRemaining = 83 * 60_000L;
        restored.restoreSuperItemTime(ConstItem.BO_HUYET_2, savedRemaining, now);
        if (restored.getRemainingSuperItemTime(ConstItem.BO_HUYET_2, now) != savedRemaining
                || restored.getRemainingSuperItemTime(ConstItem.BO_KHI_2, now) != 0) {
            throw new AssertionError();
        }
    }
}
