package nro.models.server;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import nro.models.utils.Logger;

/**
 *
 * @author By Mr Blue
 *
 */

public class AutoMaintenance implements Runnable {

    public static volatile boolean AutoMaintenance = false;
    private static AutoMaintenance instance;
    public static boolean isRunning;
    private LocalDate lastTriggeredDate;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            try {
                GameConfigService config = GameConfigService.gI();
                AutoMaintenance = config.isAutoMaintenanceEnabled();
                if (AutoMaintenance && !Maintenance.isRunning) {
                    LocalDate today = LocalDate.now();
                    LocalTime scheduled = config.getMaintenanceTime();
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime scheduledToday = today.atTime(scheduled);
                    boolean inTriggerWindow = !now.isBefore(scheduledToday)
                            && now.isBefore(scheduledToday.plusMinutes(1));
                    if (inTriggerWindow && !today.equals(lastTriggeredDate)) {
                        lastTriggeredDate = today;
                        isRunning = true;
                        Logger.log(Logger.PURPLE, "Bắt đầu lịch bảo trì từ database.\n");
                        Maintenance.gI().startSeconds(config.getMaintenanceCountdownSeconds());
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Logger.error("Lỗi kiểm tra lịch bảo trì: " + e.getMessage() + "\n");
            }
        }
    }
}
