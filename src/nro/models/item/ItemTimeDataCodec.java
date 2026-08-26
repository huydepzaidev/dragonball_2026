package nro.models.item;

import nro.models.consts.ConstItem;
import org.json.simple.JSONArray;

/**
 * Canonical serializer for player.data_item_time.
 *
 * Keep the existing 33 data slots stable. New formats append a version marker
 * so save and load code cannot silently drift apart again.
 */
public final class ItemTimeDataCodec {

    public static final int LEGACY_FIELD_COUNT = 33;
    public static final int VERSION_INDEX = 33;
    public static final int CURRENT_VERSION = 2;

    private static final int BO_HUYET = 0;
    private static final int BO_HUYET_2 = 1;
    private static final int BO_KHI = 2;
    private static final int BO_KHI_2 = 3;
    private static final int GIAP_XEN = 4;
    private static final int GIAP_XEN_2 = 5;
    private static final int CUONG_NO = 6;
    private static final int CUONG_NO_2 = 7;
    private static final int AN_DANH = 8;
    private static final int AN_DANH_2 = 9;
    private static final int OPEN_POWER = 10;
    private static final int MAY_DO = 11;
    private static final int CO_BON_LA = 12;
    private static final int KHO_BAU_X2 = 13;
    private static final int BUA_SANTA = 14;
    private static final int MEAL = 15;
    private static final int MEAL_ICON = 16;
    private static final int TDLT_MINUTES = 17;
    private static final int CMS = 18;
    private static final int GTPT = 19;
    private static final int DUOI_KHI = 20;
    private static final int RX_MINUTES = 21;
    private static final int MEAL_2 = 22;
    private static final int MEAL_2_ICON = 23;
    private static final int NCD = 25;
    private static final int NUOC_MIA_1 = 26;
    private static final int NUOC_MIA_2 = 27;
    private static final int NUOC_MIA_3 = 28;
    private static final int KILIS = 29;
    private static final int TRAI_DUA = 32;

    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MAX_TDLT_MILLIS = 30_000L * 1000L;
    private static final long MAX_TDLT_MINUTES = MAX_TDLT_MILLIS / MILLIS_PER_MINUTE;

    private ItemTimeDataCodec() {
    }

    public static JSONArray emptyData() {
        return encode(new ItemTime(null), 0L);
    }

    public static JSONArray encode(ItemTime itemTime, long now) {
        JSONArray data = new JSONArray();
        add(data, remainingFixed(itemTime.isUseBoHuyet, itemTime.lastTimeBoHuyet,
                ItemTime.TIME_ITEM, now));
        add(data, itemTime.getRemainingSuperItemTime(ConstItem.BO_HUYET_2, now));
        add(data, remainingFixed(itemTime.isUseBoKhi, itemTime.lastTimeBoKhi,
                ItemTime.TIME_ITEM, now));
        add(data, itemTime.getRemainingSuperItemTime(ConstItem.BO_KHI_2, now));
        add(data, remainingFixed(itemTime.isUseGiapXen, itemTime.lastTimeGiapXen,
                ItemTime.TIME_ITEM, now));
        add(data, itemTime.getRemainingSuperItemTime(ConstItem.GIAP_XEN_BO_HUNG_2, now));
        add(data, remainingFixed(itemTime.isUseCuongNo, itemTime.lastTimeCuongNo,
                ItemTime.TIME_ITEM, now));
        add(data, itemTime.getRemainingSuperItemTime(ConstItem.CUONG_NO_2, now));
        add(data, remainingFixed(itemTime.isUseAnDanh, itemTime.lastTimeAnDanh,
                ItemTime.TIME_ITEM, now));
        add(data, itemTime.getRemainingSuperItemTime(ConstItem.AN_DANH_2, now));
        add(data, remainingFixed(itemTime.isOpenPower, itemTime.lastTimeOpenPower,
                ItemTime.TIME_OPEN_POWER, now));
        add(data, itemTime.getRemainingMayDoTime(now));
        add(data, remainingFixed(itemTime.isUseCoBonLa, itemTime.lastTimeUseCoBonLa,
                ItemTime.TIME_CO_BON_LA, now));
        add(data, remainingFixed(itemTime.isUseKhoBauX2, itemTime.lastTimeUseKhoBauX2,
                ItemTime.TIME_MAY_DO2, now));
        add(data, itemTime.getRemainingBuaSantaTime(now));
        add(data, itemTime.getRemainingMealTime(now));
        add(data, itemTime.isEatMeal ? itemTime.iconMeal : 0);
        add(data, remainingTdltMinutes(itemTime, now));
        add(data, remainingFixed(itemTime.isUseCMS, itemTime.lastTimeUseCMS,
                ItemTime.TIME_CMS, now));
        add(data, remainingFixed(itemTime.isUseGTPT, itemTime.lastTimeUseGTPT,
                ItemTime.TIME_ITEM, now));
        add(data, remainingFixed(itemTime.isUseDK, itemTime.lastTimeUseDK,
                ItemTime.TIME_DK, now));
        add(data, remainingRxMinutes(itemTime, now));
        add(data, itemTime.getRemainingMeal2Time(now));
        add(data, itemTime.isEatMeal2 ? itemTime.iconMeal2 : 0);
        add(data, 0);
        add(data, remainingFixed(itemTime.isUseNCD, itemTime.lastTimeUseNCD,
                ItemTime.TIME_NCD, now));
        add(data, remainingFixed(itemTime.isUseNuocMia1, itemTime.lastTimeUseNuocMia1,
                ItemTime.TIME_NUOC_MIA1, now));
        add(data, remainingFixed(itemTime.isUseNuocMia2, itemTime.lastTimeUseNuocMia2,
                ItemTime.TIME_NUOC_MIA2, now));
        add(data, remainingFixed(itemTime.isUseNuocMia3, itemTime.lastTimeUseNuocMia3,
                ItemTime.TIME_NUOC_MIA3, now));
        add(data, remainingFixed(itemTime.isUseKilis, itemTime.lastTimeUseKilis,
                ItemTime.TIME_KILIS, now));
        add(data, 0);
        add(data, 0);
        add(data, itemTime.getRemainingTraiDuaTime(now));
        add(data, CURRENT_VERSION);
        return data;
    }

    public static void restore(ItemTime itemTime, JSONArray data, long now) {
        long timeBoHuyet = remaining(data, BO_HUYET, ItemTime.TIME_ITEM);
        itemTime.isUseBoHuyet = timeBoHuyet > 0;
        itemTime.lastTimeBoHuyet = lastTime(now, ItemTime.TIME_ITEM, timeBoHuyet);
        itemTime.restoreSuperItemTime(ConstItem.BO_HUYET_2,
                remaining(data, BO_HUYET_2, ItemTime.MAX_TIME_SUPER_ITEM), now);

        long timeBoKhi = remaining(data, BO_KHI, ItemTime.TIME_ITEM);
        itemTime.isUseBoKhi = timeBoKhi > 0;
        itemTime.lastTimeBoKhi = lastTime(now, ItemTime.TIME_ITEM, timeBoKhi);
        itemTime.restoreSuperItemTime(ConstItem.BO_KHI_2,
                remaining(data, BO_KHI_2, ItemTime.MAX_TIME_SUPER_ITEM), now);

        long timeGiapXen = remaining(data, GIAP_XEN, ItemTime.TIME_ITEM);
        itemTime.isUseGiapXen = timeGiapXen > 0;
        itemTime.lastTimeGiapXen = lastTime(now, ItemTime.TIME_ITEM, timeGiapXen);
        itemTime.restoreSuperItemTime(ConstItem.GIAP_XEN_BO_HUNG_2,
                remaining(data, GIAP_XEN_2, ItemTime.MAX_TIME_SUPER_ITEM), now);

        long timeCuongNo = remaining(data, CUONG_NO, ItemTime.TIME_ITEM);
        itemTime.isUseCuongNo = timeCuongNo > 0;
        itemTime.lastTimeCuongNo = lastTime(now, ItemTime.TIME_ITEM, timeCuongNo);
        itemTime.restoreSuperItemTime(ConstItem.CUONG_NO_2,
                remaining(data, CUONG_NO_2, ItemTime.MAX_TIME_SUPER_ITEM), now);

        long timeAnDanh = remaining(data, AN_DANH, ItemTime.TIME_ITEM);
        itemTime.isUseAnDanh = timeAnDanh > 0;
        itemTime.lastTimeAnDanh = lastTime(now, ItemTime.TIME_ITEM, timeAnDanh);
        itemTime.restoreSuperItemTime(ConstItem.AN_DANH_2,
                remaining(data, AN_DANH_2, ItemTime.MAX_TIME_SUPER_ITEM), now);

        long timeOpenPower = remaining(data, OPEN_POWER, ItemTime.TIME_OPEN_POWER);
        itemTime.isOpenPower = timeOpenPower > 0;
        itemTime.lastTimeOpenPower = lastTime(now, ItemTime.TIME_OPEN_POWER, timeOpenPower);
        itemTime.restoreMayDoTime(remaining(data, MAY_DO, ItemTime.MAX_TIME_MAY_DO), now);

        long timeCoBonLa = remaining(data, CO_BON_LA, ItemTime.TIME_CO_BON_LA);
        itemTime.isUseCoBonLa = timeCoBonLa > 0;
        itemTime.lastTimeUseCoBonLa = lastTime(now, ItemTime.TIME_CO_BON_LA, timeCoBonLa);

        long timeKhoBauX2 = remaining(data, KHO_BAU_X2, ItemTime.TIME_MAY_DO2);
        itemTime.isUseKhoBauX2 = timeKhoBauX2 > 0;
        itemTime.lastTimeUseKhoBauX2 = lastTime(now, ItemTime.TIME_MAY_DO2, timeKhoBauX2);
        itemTime.restoreBuaSantaTime(nonNegative(data, BUA_SANTA), now);

        long timeMeal = remaining(data, MEAL, ItemTime.MAX_TIME_MEAL);
        int iconMeal = positiveInt(data, MEAL_ICON);
        itemTime.restoreMealTime(iconMeal, timeMeal, now);

        long tdltMinutes = nonNegative(data, TDLT_MINUTES);
        if (tdltMinutes > 0 && tdltMinutes <= MAX_TDLT_MINUTES) {
            itemTime.isUseTDLT = true;
            itemTime.timeTDLT = (int) (tdltMinutes * MILLIS_PER_MINUTE);
            itemTime.lastTimeUseTDLT = now;
        } else {
            itemTime.isUseTDLT = false;
            itemTime.timeTDLT = 0;
            itemTime.lastTimeUseTDLT = 0;
        }

        long timeCms = remaining(data, CMS, ItemTime.TIME_CMS);
        itemTime.isUseCMS = timeCms > 0;
        itemTime.lastTimeUseCMS = lastTime(now, ItemTime.TIME_CMS, timeCms);
        long timeGtpt = remaining(data, GTPT, ItemTime.TIME_ITEM);
        itemTime.isUseGTPT = timeGtpt > 0;
        itemTime.lastTimeUseGTPT = lastTime(now, ItemTime.TIME_ITEM, timeGtpt);
        long timeDuoiKhi = remaining(data, DUOI_KHI, ItemTime.TIME_DK);
        itemTime.isUseDK = timeDuoiKhi > 0;
        itemTime.lastTimeUseDK = lastTime(now, ItemTime.TIME_DK, timeDuoiKhi);
        restoreRx(itemTime, nonNegative(data, RX_MINUTES), now);

        itemTime.restoreMeal2Time(positiveInt(data, MEAL_2_ICON),
                remaining(data, MEAL_2, ItemTime.MAX_TIME_MEAL2), now);

        long timeNcd = remaining(data, NCD, ItemTime.TIME_NCD);
        itemTime.isUseNCD = timeNcd > 0;
        itemTime.lastTimeUseNCD = lastTime(now, ItemTime.TIME_NCD, timeNcd);

        long timeNuocMia1 = remaining(data, NUOC_MIA_1, ItemTime.TIME_NUOC_MIA1);
        itemTime.isUseNuocMia1 = timeNuocMia1 > 0;
        itemTime.lastTimeUseNuocMia1 = lastTime(now, ItemTime.TIME_NUOC_MIA1, timeNuocMia1);
        long timeNuocMia2 = remaining(data, NUOC_MIA_2, ItemTime.TIME_NUOC_MIA2);
        itemTime.isUseNuocMia2 = timeNuocMia2 > 0;
        itemTime.lastTimeUseNuocMia2 = lastTime(now, ItemTime.TIME_NUOC_MIA2, timeNuocMia2);
        long timeNuocMia3 = remaining(data, NUOC_MIA_3, ItemTime.TIME_NUOC_MIA3);
        itemTime.isUseNuocMia3 = timeNuocMia3 > 0;
        itemTime.lastTimeUseNuocMia3 = lastTime(now, ItemTime.TIME_NUOC_MIA3, timeNuocMia3);

        long timeKilis = remaining(data, KILIS, ItemTime.TIME_KILIS);
        itemTime.isUseKilis = timeKilis > 0;
        itemTime.lastTimeUseKilis = lastTime(now, ItemTime.TIME_KILIS, timeKilis);
        itemTime.timeLengthKilis = itemTime.isUseKilis ? ItemTime.TIME_KILIS : 0;
        itemTime.restoreTraiDuaTime(remaining(data, TRAI_DUA, ItemTime.MAX_TIME_TRAI_DUA), now);
    }

    private static void restoreRx(ItemTime itemTime, long minutes, long now) {
        long maxMinutes = Integer.MAX_VALUE / MILLIS_PER_MINUTE;
        long safeMinutes = Math.min(minutes, maxMinutes);
        itemTime.isUseRX = safeMinutes > 0;
        itemTime.timeRX = itemTime.isUseRX ? (int) (safeMinutes * MILLIS_PER_MINUTE) : 0;
        itemTime.lastTimeUseRX = itemTime.isUseRX ? now : 0;
    }

    private static long remainingTdltMinutes(ItemTime itemTime, long now) {
        if (!itemTime.isUseTDLT) {
            return 0;
        }
        long remaining = itemTime.timeTDLT - (now - itemTime.lastTimeUseTDLT);
        if (remaining <= 0 || remaining > MAX_TDLT_MILLIS) {
            return 0;
        }
        return remaining / MILLIS_PER_MINUTE;
    }

    private static long remainingRxMinutes(ItemTime itemTime, long now) {
        if (!itemTime.isUseRX) {
            return 0;
        }
        return Math.max(0, itemTime.timeRX - (now - itemTime.lastTimeUseRX)) / MILLIS_PER_MINUTE;
    }

    private static long remainingFixed(boolean active, long lastTime, long duration, long now) {
        if (!active) {
            return 0;
        }
        return Math.max(0, Math.min(duration, duration - (now - lastTime)));
    }

    private static long remaining(JSONArray data, int index, long maximum) {
        return Math.min(nonNegative(data, index), maximum);
    }

    private static long nonNegative(JSONArray data, int index) {
        if (data == null || index < 0 || index >= data.size()) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(String.valueOf(data.get(index))));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static int positiveInt(JSONArray data, int index) {
        return (int) Math.min(Integer.MAX_VALUE, nonNegative(data, index));
    }

    private static long lastTime(long now, long duration, long remaining) {
        return remaining > 0 ? now - (duration - remaining) : 0;
    }

    private static void add(JSONArray data, long value) {
        data.add(Math.max(0, value));
    }
}
