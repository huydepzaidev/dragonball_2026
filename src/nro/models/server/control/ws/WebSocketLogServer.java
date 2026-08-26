package nro.models.server.control.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nro.models.server.control.ControlConfig;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.AuthManager;
import nro.models.server.control.log.ConsoleLogBuffer;
import nro.models.utils.Logger;
import org.json.simple.JSONObject;

public final class WebSocketLogServer implements Runnable {

    private static final WebSocketLogServer INSTANCE = new WebSocketLogServer();
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final Set<ClientConnection> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    private WebSocketLogServer() {}

    public static WebSocketLogServer gI() {
        return INSTANCE;
    }

    public synchronized void start(int port) {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(ControlConfig.HOST.equals("0.0.0.0") ? "0.0.0.0" : ControlConfig.HOST, port));
            running = true;

            new Thread(this, "Control-WebSocket-Server").start();

            // Hook to ConsoleLogBuffer to stream logs live
            ConsoleLogBuffer.gI().addListener(entry -> {
                JSONObject msg = new JSONObject();
                msg.put("type", "LOG_ENTRY");
                msg.put("data", entry.toJson());
                broadcast(msg.toJSONString());
            });

            Logger.success("Control WebSocket Server running on port " + port + "\n");
        } catch (IOException e) {
            Logger.log(Logger.RED, "Không thể khởi động WebSocket server trên port " + port + ": " + e.getMessage() + "\n");
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientConnection client : clients) {
                client.close();
            }
            clients.clear();
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                clientPool.submit(new ClientConnection(socket));
            } catch (IOException e) {
                if (!running) break;
            }
        }
    }

    public void broadcast(String message) {
        if (!running || clients.isEmpty()) return;
        for (ClientConnection client : clients) {
            client.sendText(message);
        }
    }

    private class ClientConnection implements Runnable {
        private final Socket socket;
        private OutputStream out;
        private InputStream in;
        private boolean authenticated = false;
        private AdminUser user;

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line = reader.readLine();
                if (line == null || !line.startsWith("GET ")) {
                    socket.close();
                    return;
                }

                // Extract query params for token: e.g. GET /ws/logs?token=xxx HTTP/1.1
                String pathAndQuery = line.split(" ")[1];
                String token = extractToken(pathAndQuery);

                String key = null;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                        key = line.substring(line.indexOf(":") + 1).trim();
                    }
                }

                if (key == null) {
                    socket.close();
                    return;
                }

                // Verify token if present in URL
                if (token != null) {
                    this.user = AuthManager.gI().validateToken(token, "ACCESS");
                    if (this.user != null) {
                        this.authenticated = true;
                    }
                }

                // Handshake response
                String acceptKey = generateAcceptKey(key);
                String response = "HTTP/1.1 101 Switching Protocols\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();

                clients.add(this);

                // Send welcome message
                JSONObject welcome = new JSONObject();
                welcome.put("type", "CONNECTED");
                welcome.put("auth", this.authenticated);
                welcome.put("server_time", System.currentTimeMillis());
                sendText(welcome.toJSONString());

                // Read frames loop
                while (running && !socket.isClosed()) {
                    int b1 = in.read();
                    if (b1 == -1) break;
                    int opcode = b1 & 0x0F;
                    if (opcode == 8) { // CLOSE
                        break;
                    }

                    int b2 = in.read();
                    if (b2 == -1) break;
                    boolean masked = (b2 & 0x80) != 0;
                    long length = b2 & 0x7F;

                    if (length == 126) {
                        length = ((in.read() << 8) | in.read());
                    } else if (length == 127) {
                        length = 0;
                        for (int i = 0; i < 8; i++) {
                            length = (length << 8) | in.read();
                        }
                    }

                    byte[] masks = new byte[4];
                    if (masked) {
                        in.read(masks, 0, 4);
                    }

                    byte[] payload = new byte[(int) length];
                    int readBytes = 0;
                    while (readBytes < length) {
                        int r = in.read(payload, readBytes, (int) (length - readBytes));
                        if (r == -1) break;
                        readBytes += r;
                    }

                    if (masked) {
                        for (int i = 0; i < payload.length; i++) {
                            payload[i] = (byte) (payload[i] ^ masks[i % 4]);
                        }
                    }

                    if (opcode == 1) { // TEXT FRAME
                        String msg = new String(payload, StandardCharsets.UTF_8);
                        handleIncomingMessage(msg);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                close();
            }
        }

        private void handleIncomingMessage(String msg) {
            try {
                if (msg.startsWith("AUTH:")) {
                    String token = msg.substring(5).trim();
                    this.user = AuthManager.gI().validateToken(token, "ACCESS");
                    if (this.user != null) {
                        this.authenticated = true;
                        JSONObject authOk = new JSONObject();
                        authOk.put("type", "AUTH_OK");
                        authOk.put("user", user.getUsername());
                        authOk.put("role", user.getRole().name());
                        sendText(authOk.toJSONString());
                    }
                }
            } catch (Exception ignored) {}
        }

        public synchronized void sendText(String text) {
            if (socket.isClosed()) return;
            try {
                byte[] raw = text.getBytes(StandardCharsets.UTF_8);
                int len = raw.length;

                out.write(0x81); // Final text frame
                if (len <= 125) {
                    out.write(len);
                } else if (len <= 65535) {
                    out.write(126);
                    out.write((len >> 8) & 0xFF);
                    out.write(len & 0xFF);
                } else {
                    out.write(127);
                    for (int i = 7; i >= 0; i--) {
                        out.write((int) ((len >> (8 * i)) & 0xFF));
                    }
                }
                out.write(raw);
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        public void close() {
            clients.remove(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }

        private String extractToken(String pathAndQuery) {
            if (pathAndQuery == null || !pathAndQuery.contains("?")) return null;
            String query = pathAndQuery.substring(pathAndQuery.indexOf("?") + 1);
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equalsIgnoreCase("token")) {
                    return pair[1];
                }
            }
            return null;
        }

        private String generateAcceptKey(String key) throws Exception {
            String combined = key + WS_GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1 = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sha1);
        }
    }
}
