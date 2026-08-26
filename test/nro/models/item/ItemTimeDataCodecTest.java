package nro.models.item;

import nro.models.consts.ConstItem;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

public final class ItemTimeDataCodecTest {

    private ItemTimeDataCodecTest() {
    }

    public static void main(String[] args) {
        restoresAccount1111WithoutShiftedEffects();
        roundTripsAllShiftedFields();
        acceptsShortLegacyData();
        System.out.println("ItemTimeDataCodecTest: OK");
    }

    private static void restoresAccount1111WithoutShiftedEffects() {
        String raw = "[0,4732496,0,4747213,0,4736919,0,4742044,0,0,0,0,0,0,0,570155,6327,6290,0,0,0,0,1776335,8060,0,0,0,0,0,0,0,0,0]";
        JSONArray data = (JSONArray) JSONValue.parse(raw);
        ItemTime restored = new ItemTime(null);
        long now = 10_000_000_000L;

        ItemTimeDataCodec.restore(restored, data, now);

        assertEquals(4_732_496L,
                restored.getRemainingSuperItemTime(ConstItem.BO_HUYET_2, now), "super blood");
        assertEquals(4_747_213L,
                restored.getRemainingSuperItemTime(ConstItem.BO_KHI_2, now), "super mana");
        assertEquals(4_736_919L,
                restored.getRemainingSuperItemTime(ConstItem.GIAP_XEN_BO_HUNG_2, now), "super armor");
        assertEquals(4_742_044L,
                restored.getRemainingSuperItemTime(ConstItem.CUONG_NO_2, now), "super rage");
        assertTrue(restored.isEatMeal, "normal meal must survive login");
        assertEquals(6327, restored.iconMeal, "normal meal icon");
        assertEquals(570_155L,
                ItemTimeDataCodec.encode(restored, now).get(15), "normal meal remaining");
        assertTrue(restored.isEatMeal2, "meal 2 must survive login");
        assertEquals(8060, restored.iconMeal2, "meal 2 icon");
        assertEquals(1_776_335L, restored.getRemainingMeal2Time(now), "meal 2 remaining");
        assertFalse(restored.isUseTDLT, "impossible 6290-minute TDLT must be removed");
        assertFalse(restored.isUseCMS, "TDLT data must not become a fake CMS timer");
    }

    private static void roundTripsAllShiftedFields() {
        long now = 20_000_000_000L;
        ItemTime source = new ItemTime(null);
        source.restoreBuaSantaTime(55 * 60_000L, now);
        source.restoreMealTime(6327, 95 * 60_000L, now);
        source.isUseTDLT = true;
        source.timeTDLT = 120 * 60_000;
        source.lastTimeUseTDLT = now;
        source.isUseCMS = true;
        source.lastTimeUseCMS = now - 120_000L;
        source.isUseGTPT = true;
        source.lastTimeUseGTPT = now - 60_000L;
        source.isUseDK = true;
        source.lastTimeUseDK = now - 60_000L;
        source.isUseRX = true;
        source.timeRX = 90 * 60_000;
        source.lastTimeUseRX = now;
        source.restoreMeal2Time(8061, 80 * 60_000L, now);
        source.isUseNCD = true;
        source.lastTimeUseNCD = now - 60_000L;
        source.isUseNuocMia1 = true;
        source.lastTimeUseNuocMia1 = now - 60_000L;
        source.isUseNuocMia2 = true;
        source.lastTimeUseNuocMia2 = now - 120_000L;
        source.isUseNuocMia3 = true;
        source.lastTimeUseNuocMia3 = now - 180_000L;
        source.isUseKilis = true;
        source.lastTimeUseKilis = now - 240_000L;
        source.restoreTraiDuaTime(200 * 60_000L, now);

        JSONArray encoded = ItemTimeDataCodec.encode(source, now);
        assertEquals(34, encoded.size(), "versioned field count");
        assertEquals((long) ItemTimeDataCodec.CURRENT_VERSION, encoded.get(33), "version marker");

        ItemTime restored = new ItemTime(null);
        ItemTimeDataCodec.restore(restored, encoded, now);
        JSONArray encodedAgain = ItemTimeDataCodec.encode(restored, now);

        for (int index = 0; index < encoded.size(); index++) {
            assertEquals(encoded.get(index), encodedAgain.get(index), "round trip index " + index);
        }
    }

    private static void acceptsShortLegacyData() {
        JSONArray legacy = new JSONArray();
        for (int i = 0; i < 21; i++) {
            legacy.add(0L);
        }
        ItemTime restored = new ItemTime(null);
        ItemTimeDataCodec.restore(restored, legacy, 30_000_000_000L);
        assertFalse(restored.isEatMeal, "empty legacy meal");
        assertFalse(restored.isEatMeal2, "empty legacy meal 2");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }
}
