package nro.models.item;

public final class MayDoTimeStackTest {

    private MayDoTimeStackTest() {
    }

    public static void main(String[] args) {
        ItemTime itemTime = new ItemTime(null);
        long now = 1_000_000_000L;

        for (int use = 1; use <= 4; use++) {
            long added = itemTime.addMayDoTime(now);
            if (added != ItemTime.TIME_MAY_DO_PER_USE) {
                throw new AssertionError();
            }
            if (itemTime.getRemainingMayDoTime(now)
                    != use * ItemTime.TIME_MAY_DO_PER_USE) {
                throw new AssertionError();
            }
        }
        if (itemTime.addMayDoTime(now) != 0) {
            throw new AssertionError();
        }

        long elapsed = 10 * 60_000L;
        if (itemTime.addMayDoTime(now + elapsed) != elapsed
                || itemTime.getRemainingMayDoTime(now + elapsed) != ItemTime.MAX_TIME_MAY_DO) {
            throw new AssertionError();
        }

        ItemTime restored = new ItemTime(null);
        long savedRemaining = 75 * 60_000L;
        restored.restoreMayDoTime(savedRemaining, now);
        if (!restored.isUseMayDo
                || restored.getRemainingMayDoTime(now) != savedRemaining) {
            throw new AssertionError();
        }
    }
}
