package nro.models.matches.dai_hoi_vo_thuat;

import nro.models.server.Maintenance;
import nro.models.utils.Functions;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;

import java.time.LocalDate;
import java.time.LocalTime;

public class SuperRankRewardScheduler implements Runnable {

    private static SuperRankRewardScheduler instance;

    public static SuperRankRewardScheduler gI() {
        if (instance == null) {
            instance = new SuperRankRewardScheduler();
        }
        return instance;
    }

    @Override
    public void run() {
        Logger.log(Logger.PURPLE, "Super Rank Reward Scheduler started.");
        resumeOnStartup();

        while (!Maintenance.isRunning) {
            try {
                LocalDate today = LocalDate.now(TimeUtil.VIETNAM_ZONE);
                LocalTime now = LocalTime.now(TimeUtil.VIETNAM_ZONE);

                if (now.getHour() >= 20) {
                    SuperRankRewardEngine.processDailyCycle(today);
                }
            } catch (Exception e) {
                Logger.logException(SuperRankRewardScheduler.class, e);
            }

            Functions.sleep(5000);
        }
    }

    public void resumeOnStartup() {
        try {
            LocalDate today = LocalDate.now(TimeUtil.VIETNAM_ZONE);
            LocalTime now = LocalTime.now(TimeUtil.VIETNAM_ZONE);

            if (now.getHour() >= 20) {
                SuperRankRewardEngine.processDailyCycle(today);
            } else {
                SuperRankRewardEngine.retryPendingMailboxes();
            }
        } catch (Exception e) {
            Logger.logException(SuperRankRewardScheduler.class, e);
        }
    }
}
