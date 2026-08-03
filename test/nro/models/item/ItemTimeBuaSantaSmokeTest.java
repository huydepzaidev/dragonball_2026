package nro.models.item;

public final class ItemTimeBuaSantaSmokeTest {

    private ItemTimeBuaSantaSmokeTest() {
    }

    public static void main(String[] args) {
        ItemTime itemTime = new ItemTime(null);
        long start = 1_000_000_000L;

        long first = itemTime.addBuaSantaTime(start);
        if (first != ItemTime.TIME_BUA_SANTA
                || itemTime.getRemainingBuaSantaTime(start) != ItemTime.TIME_BUA_SANTA) {
            throw new AssertionError("First charm must grant exactly 30 minutes");
        }

        long fiveMinutesLater = start + 5 * 60_000L;
        long second = itemTime.addBuaSantaTime(fiveMinutesLater);
        if (second != 55 * 60_000L
                || itemTime.getRemainingBuaSantaTime(fiveMinutesLater) != 55 * 60_000L) {
            throw new AssertionError("Second charm must add 30 minutes to remaining time");
        }

        long afterExpiration = start + 2 * 60 * 60_000L;
        long restarted = itemTime.addBuaSantaTime(afterExpiration);
        if (restarted != ItemTime.TIME_BUA_SANTA) {
            throw new AssertionError("Expired charm state must restart at 30 minutes");
        }

        System.out.println("ITEM_TIME_BUA_SANTA_SMOKE_TEST_OK");
    }
}
