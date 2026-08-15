package nro.models.item;

public final class Meal2TimeStackTest {

    private static final int[] FOOD_ICONS = {8060, 8061, 8062};

    private Meal2TimeStackTest() {
    }

    public static void main(String[] args) {
        stacksEveryFoodToExactlyTwoHours();
        rejectsAnotherFoodWhileActive();
        allowsTheSameFoodAfterElapsedTime();
        restoresAndClampsSavedTime();
        allowsAnotherFoodAfterExpiry();
        System.out.println("Meal2TimeStackTest: OK");
    }

    private static void stacksEveryFoodToExactlyTwoHours() {
        long now = 1_000_000_000L;
        for (int icon : FOOD_ICONS) {
            ItemTime itemTime = new ItemTime(null);
            for (int use = 1; use <= 12; use++) {
                assertEquals(ItemTime.TIME_MEAL2_PER_USE,
                        itemTime.addMeal2Time(icon, now), "full serving must be added");
                assertEquals(use * ItemTime.TIME_MEAL2_PER_USE,
                        itemTime.getRemainingMeal2Time(now), "remaining time after serving");
            }
            assertEquals(ItemTime.MAX_TIME_MEAL2,
                    itemTime.getRemainingMeal2Time(now), "maximum duration");
            assertEquals(0, itemTime.addMeal2Time(icon, now), "item must not be consumed at cap");
        }
    }

    private static void rejectsAnotherFoodWhileActive() {
        long now = 2_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        itemTime.addMeal2Time(FOOD_ICONS[0], now);
        assertEquals(-1, itemTime.addMeal2Time(FOOD_ICONS[1], now),
                "another food must be rejected");
        assertEquals(FOOD_ICONS[0], itemTime.iconMeal2, "active food icon must be preserved");
    }

    private static void allowsTheSameFoodAfterElapsedTime() {
        long now = 3_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        for (int use = 0; use < 12; use++) {
            itemTime.addMeal2Time(FOOD_ICONS[1], now);
        }
        long tenMinutesLater = now + ItemTime.TIME_MEAL2_PER_USE;
        assertEquals(ItemTime.TIME_MEAL2_PER_USE,
                itemTime.addMeal2Time(FOOD_ICONS[1], tenMinutesLater),
                "elapsed slot must accept one full serving");
        assertEquals(ItemTime.MAX_TIME_MEAL2,
                itemTime.getRemainingMeal2Time(tenMinutesLater), "duration returns to cap");
    }

    private static void restoresAndClampsSavedTime() {
        long now = 4_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        long saved = 83 * 60 * 1000L;
        itemTime.restoreMeal2Time(FOOD_ICONS[2], saved, now);
        assertTrue(itemTime.isEatMeal2, "restored food must be active");
        assertEquals(FOOD_ICONS[2], itemTime.iconMeal2, "restored icon");
        assertEquals(saved, itemTime.getRemainingMeal2Time(now), "restored duration");

        itemTime.restoreMeal2Time(FOOD_ICONS[2], ItemTime.MAX_TIME_MEAL2 * 2, now);
        assertEquals(ItemTime.MAX_TIME_MEAL2,
                itemTime.getRemainingMeal2Time(now), "oversized save must be clamped");
    }

    private static void allowsAnotherFoodAfterExpiry() {
        long now = 5_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        itemTime.addMeal2Time(FOOD_ICONS[0], now);
        long expired = now + ItemTime.TIME_MEAL2_PER_USE + 1;
        assertEquals(ItemTime.TIME_MEAL2_PER_USE,
                itemTime.addMeal2Time(FOOD_ICONS[2], expired),
                "a new food is allowed after expiry");
        assertEquals(FOOD_ICONS[2], itemTime.iconMeal2, "new food icon");
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
