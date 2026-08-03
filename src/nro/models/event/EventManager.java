package nro.models.event;

import nro.models.event_list.TopUp;
import nro.models.event_list.TrungThu;
import nro.models.event_list.HungVuong;
import nro.models.event_list.Christmas;
import nro.models.event_list.Halloween;
import nro.models.event_list.LunarNewYear;
import nro.models.event_list.Default;
import nro.models.event_list.InternationalWomensDay;
import nro.models.server.EventControlService;

public class EventManager {

    private static EventManager instance;

    public static boolean LUNNAR_NEW_YEAR = true;

    public static boolean INTERNATIONAL_WOMANS_DAY = true;

    public static boolean CHRISTMAS = true;

    public static boolean HALLOWEEN = true;

    public static boolean HUNG_VUONG = true;

    public static boolean TRUNG_THU = true;

    public static boolean TOP_UP = true;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        new Default().init();
        EventControlService control = EventControlService.gI();
        if (isEnabled(control, EventControlService.LUNAR_NEW_YEAR, LUNNAR_NEW_YEAR)) {
            new LunarNewYear().init();
        }
        if (isEnabled(control, EventControlService.WOMENS_DAY, INTERNATIONAL_WOMANS_DAY)) {
            new InternationalWomensDay().init();
        }
        if (isEnabled(control, EventControlService.HALLOWEEN, HALLOWEEN)) {
            new Halloween().init();
        }
        if (isEnabled(control, EventControlService.CHRISTMAS, CHRISTMAS)) {
            new Christmas().init();
        }
        if (isEnabled(control, EventControlService.HUNG_VUONG, HUNG_VUONG)) {
            new HungVuong().init();
        }
        if (isEnabled(control, EventControlService.MID_AUTUMN, TRUNG_THU)) {
            new TrungThu().init();
        }
        if (isEnabled(control, EventControlService.TOP_UP, TOP_UP)) {
            new TopUp().init();
        }
    }

    private boolean isEnabled(EventControlService control, String eventKey, boolean legacyDefault) {
        return control.isAvailable() ? control.isEnabled(eventKey) : legacyDefault;
    }
}
