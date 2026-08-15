package nro.models.combine;

public final class AngelCraftingPolicyTest {

    private AngelCraftingPolicyTest() {
    }

    public static void main(String[] args) {
        assertTrue(AngelCraftingPolicy.isVipRecipe(1084), "earth VIP recipe");
        assertTrue(AngelCraftingPolicy.isVipRecipe(1086), "saiyan VIP recipe");
        assertTrue(!AngelCraftingPolicy.isVipRecipe(1071), "normal recipe must be rejected");

        assertEquals(0, AngelCraftingPolicy.qualityPercent(0, 0), "no sacrifice");
        assertEquals(5, AngelCraftingPolicy.qualityPercent(1, 0), "one destroy item");
        assertEquals(10, AngelCraftingPolicy.qualityPercent(1, 1), "destroy plus divine");
        assertEquals(15, AngelCraftingPolicy.qualityPercent(2, 1), "two destroy plus divine");
        assertEquals(-1, AngelCraftingPolicy.qualityPercent(0, 1), "divine alone");
        assertEquals(-1, AngelCraftingPolicy.qualityPercent(2, 0), "two destroy without divine");

        assertRateRange(0, 91, 95);
        assertRateRange(5, 71, 75);
        assertRateRange(10, 41, 45);
        assertRateRange(15, 16, 20);

        assertEquals(100, AngelCraftingPolicy.statWithQuality(100, 0), "zero quality");
        assertEquals(105, AngelCraftingPolicy.statWithQuality(100, 5), "five percent");
        assertEquals(110, AngelCraftingPolicy.statWithQuality(100, 10), "ten percent");
        assertEquals(115, AngelCraftingPolicy.statWithQuality(100, 15), "fifteen percent");
        assertEquals(19, AngelCraftingPolicy.statWithQuality(18, 5), "small stat rounding");

        System.out.println("AngelCraftingPolicyTest: OK");
    }

    private static void assertRateRange(int quality, int expectedLevel1, int expectedLevel5) {
        assertEquals(expectedLevel1, AngelCraftingPolicy.successRate(quality, 1074),
                quality + "% with level 1 stone");
        assertEquals(expectedLevel5, AngelCraftingPolicy.successRate(quality, 1078),
                quality + "% with level 5 stone");
    }

    private static void assertEquals(int expected, int actual, String message) {
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
