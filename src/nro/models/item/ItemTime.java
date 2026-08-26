package nro.models.item;

import nro.models.consts.ConstItem;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.utils.Util;
import nro.models.services.ItemTimeService;

public class ItemTime {

    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;
    public static final byte CON_DUONG_RAN_DOC = 2;
    public static final byte KHI_GAS_HUY_DIET = 3;
    public static final byte TIME_KEO_BUA_BAO = 4;
    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 5;

    public static final int TIME_ITEM = 600000;
    public static final long TIME_SUPER_ITEM_PER_USE = 10 * 60 * 1000L;
    public static final long MAX_TIME_SUPER_ITEM = 120 * 60 * 1000L;
    public static final int TIME_OPEN_POWER = 8640000;
    public static final long TIME_MAY_DO_PER_USE = 30 * 60 * 1000L;
    public static final long MAX_TIME_MAY_DO = 120 * 60 * 1000L;
    public static final long TIME_MAY_DO = MAX_TIME_MAY_DO;
    public static final int TIME_MAY_DO2 = 15 * 60 * 1000;
    public static final long TIME_CO_BON_LA = 30 * 60 * 1000L;
    public static final int TIME_KILIS = 3600000;
    public static final int TIME_NUOC_MIA1 = 600_000;
    public static final int TIME_NUOC_MIA2 = 600_000;
    public static final int TIME_NUOC_MIA3 = 600_000;

    public static final int TIME_BUA_SANTA = 1800000;
    public static final long TIME_TRAI_DUA_PER_USE = 30 * 60 * 1000L;
    public static final long MAX_TIME_TRAI_DUA = 500 * 60 * 1000L;
    public static final int TIME_EAT_MEAL = 600000;
    public static final long TIME_MEAL_PER_USE = TIME_EAT_MEAL;
    public static final long MAX_TIME_MEAL = 120 * 60 * 1000L;
    public static final long TIME_MEAL2_PER_USE = TIME_EAT_MEAL;
    public static final long MAX_TIME_MEAL2 = 120 * 60 * 1000L;
    public static final int TIME_CMS = 3600000;
    public static final int TIME_DK = 1800000;
    public static final int TIME_NCD = 1800000;

    private Player player;

    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public boolean isUseAnDanh2;

    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;

    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;
    public long lastTimeAnDanh2;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;
    public boolean isUseKhoBauX2;
    public long lastTimeUseKhoBauX2;
    public boolean isUseBuaSanta;
    public long lastTimeBuaSanta;
    public boolean isUseTraiDua;
    public long lastTimeUseTraiDua;
    public long timeLengthTraiDua;

    public long lastTimeUseCoBonLa;
    public boolean isUseCoBonLa;

    public boolean isUseKilis;
    public long lastTimeUseKilis;

    public boolean isUseNuocMia1;
    public long lastTimeUseNuocMia1;
    public boolean isUseNuocMia2;
    public long lastTimeUseNuocMia2;
    public boolean isUseNuocMia3;
    public long lastTimeUseNuocMia3;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isUseRX;
    public long lastTimeUseRX;
    public int timeRX;

    public boolean isUseCMS;
    public long lastTimeUseCMS;

    public boolean isUseNCD;
    public long lastTimeUseNCD;

    public boolean isUseGTPT;
    public long lastTimeUseGTPT;

    public boolean isUseDK;
    public long lastTimeUseDK;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isEatMeal2;
    public long lastTimeEatMeal2;
    public int iconMeal2;
    public long lastTimeKhauTrang;
    public boolean isUseKhauTrang;
    public long timeLengthKilis;
    public long totalCoBonLaTime;
    public long timeLengthCoBonLa;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        if (isEatMeal) {
            if (getRemainingMealTime() <= 0) {
                isEatMeal = false;
                iconMeal = 0;
                lastTimeEatMeal = 0;
                Service.gI().point(player);
            }
        }
        if (isEatMeal2) {
            if (getRemainingMeal2Time() <= 0) {
                isEatMeal2 = false;
                iconMeal2 = 0;
                lastTimeEatMeal2 = 0;
                Service.gI().point(player);
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                Service.gI().point(player);
            }
        }

        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
            }
        }

        if (isUseBoHuyet2) {
            if (getRemainingSuperItemTime(ConstItem.BO_HUYET_2) <= 0) {
                isUseBoHuyet2 = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi2) {
            if (getRemainingSuperItemTime(ConstItem.BO_KHI_2) <= 0) {
                isUseBoKhi2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen2) {
            if (getRemainingSuperItemTime(ConstItem.GIAP_XEN_BO_HUNG_2) <= 0) {
                isUseGiapXen2 = false;
            }
        }
        if (isUseCuongNo2) {
            if (getRemainingSuperItemTime(ConstItem.CUONG_NO_2) <= 0) {
                isUseCuongNo2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh2) {
            if (getRemainingSuperItemTime(ConstItem.AN_DANH_2) <= 0) {
                isUseAnDanh2 = false;
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS)) {
                isUseCMS = false;
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM)) {
                isUseGTPT = false;
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK)) {
                isUseDK = false;
            }
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
            }
        }
        if (isUseMayDo) {
            if (getRemainingMayDoTime() <= 0) {
                isUseMayDo = false;
            }
        }
        if (isUseCoBonLa) {
            if (Util.canDoWithTime(lastTimeUseCoBonLa, TIME_CO_BON_LA)) {
                isUseCoBonLa = false;
            }
        }
        if (isUseKilis) {
            if (Util.canDoWithTime(lastTimeUseKilis, TIME_KILIS)) {
                isUseKilis = false;
            }
        }
        if (isUseNuocMia1) {
            if (Util.canDoWithTime(lastTimeUseNuocMia1, TIME_NUOC_MIA1)) {
                isUseNuocMia1 = false;
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia2) {
            if (Util.canDoWithTime(lastTimeUseNuocMia2, TIME_NUOC_MIA2)) {
                isUseNuocMia2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseNuocMia3) {
            if (Util.canDoWithTime(lastTimeUseNuocMia3, TIME_NUOC_MIA3)) {
                isUseNuocMia3 = false;
                Service.gI().point(player);
            }
        }
        if (isUseBuaSanta) {
            if (getRemainingBuaSantaTime() <= 0) {
                isUseBuaSanta = false;
            }
        }
        if (isUseTraiDua && getRemainingTraiDuaTime() <= 0) {
            isUseTraiDua = false;
            timeLengthTraiDua = 0;
        }
        if (isUseKhoBauX2) {
            if (Util.canDoWithTime(lastTimeUseKhoBauX2, TIME_MAY_DO2)) {
                isUseKhoBauX2 = false;
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
            }
        }
        if (isUseRX) {
            if (Util.canDoWithTime(lastTimeUseRX, timeRX)) {
                isUseRX = false;
            }
        }
    }

    public void dispose() {
        this.player = null;
    }

    public static boolean isSuperItem(int itemId) {
        return itemId == ConstItem.CUONG_NO_2
                || itemId == ConstItem.BO_KHI_2
                || itemId == ConstItem.BO_HUYET_2
                || itemId == ConstItem.GIAP_XEN_BO_HUNG_2
                || itemId == ConstItem.AN_DANH_2;
    }

    public boolean hasActiveNormalCounterpart(int superItemId) {
        return switch (superItemId) {
            case ConstItem.CUONG_NO_2 -> isUseCuongNo;
            case ConstItem.BO_KHI_2 -> isUseBoKhi;
            case ConstItem.BO_HUYET_2 -> isUseBoHuyet;
            case ConstItem.GIAP_XEN_BO_HUNG_2 -> isUseGiapXen;
            case ConstItem.AN_DANH_2 -> isUseAnDanh;
            default -> false;
        };
    }

    public long getRemainingSuperItemTime(int itemId) {
        return getRemainingSuperItemTime(itemId, System.currentTimeMillis());
    }

    public long getRemainingSuperItemTime(int itemId, long now) {
        return switch (itemId) {
            case ConstItem.CUONG_NO_2 -> remainingSuperTime(isUseCuongNo2, lastTimeCuongNo2, now);
            case ConstItem.BO_KHI_2 -> remainingSuperTime(isUseBoKhi2, lastTimeBoKhi2, now);
            case ConstItem.BO_HUYET_2 -> remainingSuperTime(isUseBoHuyet2, lastTimeBoHuyet2, now);
            case ConstItem.GIAP_XEN_BO_HUNG_2 -> remainingSuperTime(isUseGiapXen2, lastTimeGiapXen2, now);
            case ConstItem.AN_DANH_2 -> remainingSuperTime(isUseAnDanh2, lastTimeAnDanh2, now);
            default -> 0;
        };
    }

    private long remainingSuperTime(boolean active, long lastTime, long now) {
        if (!active) {
            return 0;
        }
        return Math.max(0, MAX_TIME_SUPER_ITEM - (now - lastTime));
    }

    public long addSuperItemTime(int itemId, long now) {
        if (!isSuperItem(itemId)) {
            return 0;
        }
        long remaining = getRemainingSuperItemTime(itemId, now);
        if (remaining > MAX_TIME_SUPER_ITEM - TIME_SUPER_ITEM_PER_USE) {
            return 0;
        }
        long newRemaining = remaining + TIME_SUPER_ITEM_PER_USE;
        setSuperItemRemainingTime(itemId, newRemaining, now);
        return TIME_SUPER_ITEM_PER_USE;
    }

    public void restoreSuperItemTime(int itemId, long remaining, long now) {
        if (!isSuperItem(itemId)) {
            return;
        }
        setSuperItemRemainingTime(itemId, Math.max(0, Math.min(MAX_TIME_SUPER_ITEM, remaining)), now);
    }

    private void setSuperItemRemainingTime(int itemId, long remaining, long now) {
        boolean active = remaining > 0;
        long lastTime = active ? now - (MAX_TIME_SUPER_ITEM - remaining) : 0;
        switch (itemId) {
            case ConstItem.CUONG_NO_2 -> {
                isUseCuongNo2 = active;
                lastTimeCuongNo2 = lastTime;
            }
            case ConstItem.BO_KHI_2 -> {
                isUseBoKhi2 = active;
                lastTimeBoKhi2 = lastTime;
            }
            case ConstItem.BO_HUYET_2 -> {
                isUseBoHuyet2 = active;
                lastTimeBoHuyet2 = lastTime;
            }
            case ConstItem.GIAP_XEN_BO_HUNG_2 -> {
                isUseGiapXen2 = active;
                lastTimeGiapXen2 = lastTime;
            }
            case ConstItem.AN_DANH_2 -> {
                isUseAnDanh2 = active;
                lastTimeAnDanh2 = lastTime;
            }
            default -> {
            }
        }
    }

    public long getRemainingMealTime() {
        return getRemainingMealTime(System.currentTimeMillis());
    }

    public long getRemainingMealTime(long now) {
        if (!isEatMeal) {
            return 0;
        }
        return Math.max(0, MAX_TIME_MEAL - (now - lastTimeEatMeal));
    }

    /**
     * Adds one 10-minute serving from the Bill divine-equipment food group.
     * All five foods share the same +10% damage effect, so their time can mix.
     *
     * @return time added, or zero when another full serving would exceed 120 minutes
     */
    public long addMealTime(int iconId, long now) {
        long remaining = getRemainingMealTime(now);
        if (remaining > MAX_TIME_MEAL - TIME_MEAL_PER_USE) {
            return 0;
        }
        setMealRemainingTime(iconId, remaining + TIME_MEAL_PER_USE, now);
        return TIME_MEAL_PER_USE;
    }

    public void restoreMealTime(int iconId, long remaining, long now) {
        setMealRemainingTime(iconId,
                Math.max(0, Math.min(MAX_TIME_MEAL, remaining)), now);
    }

    private void setMealRemainingTime(int iconId, long remaining, long now) {
        isEatMeal = remaining > 0 && iconId > 0;
        iconMeal = isEatMeal ? iconId : 0;
        lastTimeEatMeal = isEatMeal
                ? now - (MAX_TIME_MEAL - remaining)
                : 0;
    }

    public long getRemainingMeal2Time() {
        return getRemainingMeal2Time(System.currentTimeMillis());
    }

    public long getRemainingMeal2Time(long now) {
        if (!isEatMeal2) {
            return 0;
        }
        return Math.max(0, MAX_TIME_MEAL2 - (now - lastTimeEatMeal2));
    }

    /**
     * Adds one 10-minute serving to the active collaboration food.
     *
     * @return time added; zero at the 120-minute cap; -1 when another food is active
     */
    public long addMeal2Time(int iconId, long now) {
        long remaining = getRemainingMeal2Time(now);
        if (remaining > 0 && iconMeal2 != iconId) {
            return -1;
        }
        if (remaining > MAX_TIME_MEAL2 - TIME_MEAL2_PER_USE) {
            return 0;
        }
        setMeal2RemainingTime(iconId, remaining + TIME_MEAL2_PER_USE, now);
        return TIME_MEAL2_PER_USE;
    }

    public void restoreMeal2Time(int iconId, long remaining, long now) {
        setMeal2RemainingTime(iconId,
                Math.max(0, Math.min(MAX_TIME_MEAL2, remaining)), now);
    }

    private void setMeal2RemainingTime(int iconId, long remaining, long now) {
        isEatMeal2 = remaining > 0;
        iconMeal2 = isEatMeal2 ? iconId : 0;
        lastTimeEatMeal2 = isEatMeal2
                ? now - (MAX_TIME_MEAL2 - remaining)
                : 0;
    }

    public long getRemainingMayDoTime() {
        return getRemainingMayDoTime(System.currentTimeMillis());
    }

    public long getRemainingMayDoTime(long now) {
        if (!isUseMayDo) {
            return 0;
        }
        return Math.max(0, MAX_TIME_MAY_DO - (now - lastTimeUseMayDo));
    }

    /**
     * Adds up to 30 minutes to the detector, capped at 120 minutes.
     *
     * @return the time actually added, or zero when already at the cap
     */
    public long addMayDoTime(long now) {
        long remaining = getRemainingMayDoTime(now);
        long newRemaining = Math.min(MAX_TIME_MAY_DO, remaining + TIME_MAY_DO_PER_USE);
        long added = newRemaining - remaining;
        if (added > 0) {
            setMayDoRemainingTime(newRemaining, now);
        }
        return added;
    }

    public void restoreMayDoTime(long remaining, long now) {
        setMayDoRemainingTime(Math.max(0, Math.min(MAX_TIME_MAY_DO, remaining)), now);
    }

    private void setMayDoRemainingTime(long remaining, long now) {
        isUseMayDo = remaining > 0;
        lastTimeUseMayDo = isUseMayDo ? now - (MAX_TIME_MAY_DO - remaining) : 0;
    }

    public long getRemainingBuaSantaTime() {
        return getRemainingBuaSantaTime(System.currentTimeMillis());
    }

    public long getRemainingBuaSantaTime(long now) {
        if (!isUseBuaSanta) {
            return 0;
        }
        return Math.max(0, TIME_BUA_SANTA - (now - lastTimeBuaSanta));
    }

    /**
     * Mỗi bùa cộng đúng 30 phút vào thời gian còn lại. Nếu trạng thái cũ đã
     * hết hạn thì bắt đầu lại từ thời điểm hiện tại.
     */
    public long addBuaSantaTime(long now) {
        long remaining = getRemainingBuaSantaTime(now);
        long newRemaining = remaining + TIME_BUA_SANTA;
        isUseBuaSanta = true;
        lastTimeBuaSanta = now - (TIME_BUA_SANTA - newRemaining);
        return newRemaining;
    }

    public void restoreBuaSantaTime(long remaining, long now) {
        long safeRemaining = Math.max(0, remaining);
        isUseBuaSanta = safeRemaining > 0;
        lastTimeBuaSanta = isUseBuaSanta
                ? now - (TIME_BUA_SANTA - safeRemaining)
                : 0;
    }

    public long getRemainingTraiDuaTime() {
        return getRemainingTraiDuaTime(System.currentTimeMillis());
    }

    public long getRemainingTraiDuaTime(long now) {
        if (!isUseTraiDua) {
            return 0;
        }
        return Math.max(0, lastTimeUseTraiDua + timeLengthTraiDua - now);
    }

    /**
     * Adds 30 minutes to the current coconut buff and returns the time actually added.
     */
    public long addTraiDuaTime(long now) {
        long remaining = getRemainingTraiDuaTime(now);
        long newRemaining = Math.min(MAX_TIME_TRAI_DUA, remaining + TIME_TRAI_DUA_PER_USE);
        long added = newRemaining - remaining;
        if (added > 0) {
            isUseTraiDua = true;
            lastTimeUseTraiDua = now;
            timeLengthTraiDua = newRemaining;
        }
        return added;
    }

    public void restoreTraiDuaTime(long remaining, long now) {
        long safeRemaining = Math.max(0, Math.min(MAX_TIME_TRAI_DUA, remaining));
        isUseTraiDua = safeRemaining > 0;
        lastTimeUseTraiDua = isUseTraiDua ? now : 0;
        timeLengthTraiDua = safeRemaining;
    }
}
