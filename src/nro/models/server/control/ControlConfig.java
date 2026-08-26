package nro.models.server.control;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import nro.models.utils.Logger;

public final class ControlConfig {

    public static volatile boolean ENABLED = true;
    public static volatile String HOST = "0.0.0.0";
    public static volatile int PORT = 8088;
    public static volatile int WS_PORT = 8089;
    public static volatile String JWT_SECRET = "MadbroS_2026_DragonBall_HuyDev_SecretKey_Secure_!@#";
    public static volatile long ACCESS_TOKEN_EXPIRATION_MS = 30 * 60 * 1000L; // 30 mins
    public static volatile long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
    public static volatile int MAX_FAILED_LOGINS = 5;
    public static volatile long LOCKOUT_DURATION_MS = 5 * 60 * 1000L; // 5 mins

    public static void load() {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream("Config.properties")) {
            prop.load(fis);
            if (prop.containsKey("server.control.enabled")) {
                ENABLED = Boolean.parseBoolean(prop.getProperty("server.control.enabled").trim());
            }
            if (prop.containsKey("server.control.host")) {
                HOST = prop.getProperty("server.control.host").trim();
            }
            if (prop.containsKey("server.control.port")) {
                PORT = Integer.parseInt(prop.getProperty("server.control.port").trim());
            }
            if (prop.containsKey("server.control.ws_port")) {
                WS_PORT = Integer.parseInt(prop.getProperty("server.control.ws_port").trim());
            } else {
                WS_PORT = PORT + 1;
            }
            if (prop.containsKey("server.control.secret")) {
                String secret = prop.getProperty("server.control.secret").trim();
                if (!secret.isEmpty()) {
                    JWT_SECRET = secret;
                }
            }
            Logger.success("Control API Config loaded: " + HOST + ":" + PORT + " (WS: " + WS_PORT + ", Enabled: " + ENABLED + ")\n");
        } catch (IOException | NumberFormatException e) {
            Logger.log(Logger.YELLOW, "Control Config: Sử dụng cấu hình mặc định (Port: " + PORT + ")\n");
        }
    }
}
