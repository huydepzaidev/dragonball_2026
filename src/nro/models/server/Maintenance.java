package nro.models.server;

import nro.models.services.Service;
import nro.models.utils.Logger;

/**
 *
 * @author By Mr Blue
 *
 */
public class Maintenance implements Runnable {

    private static Maintenance instance;
    private int timeInSeconds;
    public static boolean isRunning = false;

    private Maintenance() {
    }

    public static Maintenance gI() {
        if (instance == null) {
            instance = new Maintenance();
        }
        return instance;
    }

    public synchronized void startCountdown() {
        startSeconds(60);
    }

    public synchronized void startSeconds(int seconds) {
        if (!isRunning) {
            isRunning = true;
            this.timeInSeconds = Math.max(1, seconds);
            new Thread(this, "Maintenance countdown").start();
        }
    }

    public void startImmediately() {
        if (!isRunning) {
            isRunning = true;
            Logger.log(Logger.YELLOW, "BẮT ĐẦU BẢO TRÌ NGAY\n");
            ServerManager.gI().close();
        }
    }

    @Override
    public void run() {
        Logger.log(Logger.YELLOW, "Bắt đầu đếm ngược " + timeInSeconds + "s bảo trì");

        while (timeInSeconds > 0) {
            try {
                sendRemainingTime();
                Thread.sleep(1000);
                timeInSeconds--;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
                return;
            }
        }

        Logger.log(Logger.YELLOW, "BẢO TRÌ BẮT ĐẦU\n");
        ServerManager.gI().close();
    }

    private void sendRemainingTime() {
        String msg = "Hệ thống sẽ bảo trì sau " + timeInSeconds + " giây nữa. Hãy thoát game để tránh mất dữ liệu.";
        Service.gI().sendThongBaoAllPlayer(msg);
        Logger.log(Logger.YELLOW, msg);
    }
}
