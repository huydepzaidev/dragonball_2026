package nro.models.item;

public final class MealTimeStackTest {

    private static final int[] FOOD_ICONS = {6324, 6325, 6326, 6327, 6328};

    private MealTimeStackTest() {
    }

    public static void main(String[] args) {
        stacksMixedFoodsToExactlyTwoHours();
        acceptsOneFullServingAfterElapsedTime();
        restoresAndClampsSavedTime();
        rejectsSavedTimeWithoutAnIcon();
        System.out.println("MealTimeStackTest: OK");
    }

    private static void stacksMixedFoodsToExactlyTwoHours() {
        long now = 1_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        for (int use = 1; use <= 12; use++) {
            int icon = FOOD_ICONS[(use - 1) % FOOD_ICONS.length];
            assertEquals(ItemTime.TIME_MEAL_PER_USE,
                    itemTime.addMealTime(icon, now), "full serving must be added");
            assertEquals(use * ItemTime.TIME_MEAL_PER_USE,
                    itemTime.getRemainingMealTime(now), "remaining time after serving");
            assertEquals(icon, itemTime.iconMeal, "latest food icon");
        }
        assertEquals(ItemTime.MAX_TIME_MEAL,
                itemTime.getRemainingMealTime(now), "maximum duration");
        assertEquals(0, itemTime.addMealTime(FOOD_ICONS[0], now),
                "item must not be consumed at cap");
    }

    private static void acceptsOneFullServingAfterElapsedTime() {
        long now = 2_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        for (int use = 0; use < 12; use++) {
            itemTime.addMealTime(FOOD_ICONS[0], now);
        }
        long tenMinutesLater = now + ItemTime.TIME_MEAL_PER_USE;
        assertEquals(ItemTime.TIME_MEAL_PER_USE,
                itemTime.addMealTime(FOOD_ICONS[1], tenMinutesLater),
                "elapsed slot must accept one full serving from another food");
        assertEquals(ItemTime.MAX_TIME_MEAL,
                itemTime.getRemainingMealTime(tenMinutesLater), "duration returns to cap");
        assertEquals(FOOD_ICONS[1], itemTime.iconMeal, "latest mixed food icon");
    }

    private static void restoresAndClampsSavedTime() {
        long now = 3_000_000_000L;
        ItemTime itemTime = new ItemTime(null);
        long saved = 83 * 60_000L;
        itemTime.restoreMealTime(FOOD_ICONS[2], saved, now);
        assertEquals(saved, itemTime.getRemainingMealTime(now), "restored duration");
        assertEquals(FOOD_ICONS[2], itemTime.iconMeal, "restored icon");

        itemTime.restoreMealTime(FOOD_ICONS[3], ItemTime.MAX_TIME_MEAL * 2, now);
        assertEquals(ItemTime.MAX_TIME_MEAL,
                itemTime.getRemainingMealTime(now), "oversized save must be clamped");
    }

    private static void rejectsSavedTimeWithoutAnIcon() {
        ItemTime itemTime = new ItemTime(null);
        itemTime.restoreMealTime(0, ItemTime.TIME_MEAL_PER_USE, 4_000_000_000L);
        assertEquals(0, itemTime.getRemainingMealTime(4_000_000_000L),
                "meal without an icon must stay inactive");
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
