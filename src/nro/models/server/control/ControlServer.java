package nro.models.server.control;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import nro.models.server.control.http.HttpRouter;
import nro.models.server.control.log.ConsoleLogBuffer;
import nro.models.server.control.ws.WebSocketLogServer;
import nro.models.utils.Logger;

public final class ControlServer {

    private static final ControlServer INSTANCE = new ControlServer();
    private HttpServer httpServer;
    private volatile boolean isRunning = false;

    private ControlServer() {}

    public static ControlServer gI() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (isRunning) return;

        ControlConfig.load();
        if (!ControlConfig.ENABLED) {
            Logger.log(Logger.YELLOW, "Control API bị vô hiệu hóa trong Config.properties\n");
            return;
        }

        try {
            ConsoleLogBuffer.gI().hookSystemOut();

            String bindHost = ControlConfig.HOST.equals("0.0.0.0") ? "0.0.0.0" : ControlConfig.HOST;
            InetSocketAddress address = new InetSocketAddress(bindHost, ControlConfig.PORT);

            httpServer = HttpServer.create(address, 0);
            httpServer.createContext("/", new HttpRouter());
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();

            // Start WebSocket server on WS_PORT
            WebSocketLogServer.gI().start(ControlConfig.WS_PORT);

            isRunning = true;
            Logger.success("====================================================\n");
            Logger.success(" Ngoc Rong Vegeta - Control API Started Successfully!\n");
            Logger.success(" HTTP REST API: http://" + ControlConfig.HOST + ":" + ControlConfig.PORT + "\n");
            Logger.success(" WebSocket:     ws://" + ControlConfig.HOST + ":" + ControlConfig.WS_PORT + "\n");
            Logger.success(" Health Check:  http://" + ControlConfig.HOST + ":" + ControlConfig.PORT + "/health\n");
            Logger.success("====================================================\n");
        } catch (IOException e) {
            Logger.log(Logger.RED, "Không thể khởi động Control Server: " + e.getMessage() + "\n");
        }
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        try {
            if (httpServer != null) {
                httpServer.stop(1);
            }
            WebSocketLogServer.gI().stop();
            Logger.log(Logger.YELLOW, "Control Server đã dừng thành công.\n");
        } catch (Exception e) {
            Logger.log(Logger.RED, "Lỗi dừng Control Server: " + e.getMessage() + "\n");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
