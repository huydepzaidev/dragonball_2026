package nro.models.server.control.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import nro.models.server.control.auth.AdminUser;
import nro.models.server.control.auth.AuthManager;
import nro.models.server.control.handlers.*;
import nro.models.utils.Logger;

public final class HttpRouter implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        String path = requestUri.getPath();
        String method = exchange.getRequestMethod();

        // Handle CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        Map<String, String> queryParams = parseQueryParams(requestUri.getRawQuery());

        try {
            // 1. Public Endpoints
            if (path.equals("/health") || path.equals("/")) {
                HealthHandler.handle(exchange);
                return;
            }

            if (path.equals("/api/auth/login")) {
                AuthHandler.handleLogin(exchange);
                return;
            }

            if (path.equals("/api/auth/refresh")) {
                AuthHandler.handleRefresh(exchange);
                return;
            }

            // 2. Authenticated Endpoints Check
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            } else if (queryParams.containsKey("token")) {
                token = queryParams.get("token");
            }

            AdminUser user = AuthManager.gI().validateToken(token, "ACCESS");
            if (user == null) {
                JsonResponse.unauthorized(exchange, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
                return;
            }

            // 3. Authenticated Routes
            if (path.equals("/api/auth/me")) {
                AuthHandler.handleMe(exchange, user);
                return;
            }

            if (path.equals("/api/auth/logout")) {
                AuthHandler.handleLogout(exchange, user, token);
                return;
            }

            if (path.equals("/api/dashboard")) {
                DashboardHandler.handle(exchange, user);
                return;
            }

            // Players
            if (path.equals("/api/players/online")) {
                PlayerHandler.handleOnlineList(exchange, user);
                return;
            }

            if (path.equals("/api/players/search")) {
                PlayerHandler.handleSearch(exchange, user, queryParams.get("keyword"));
                return;
            }

            if (path.startsWith("/api/players/")) {
                String sub = path.substring("/api/players/".length());
                if (sub.contains("/action")) {
                    String idStr = sub.substring(0, sub.indexOf("/action"));
                    long playerId = Long.parseLong(idStr);
                    PlayerHandler.handlePlayerAction(exchange, user, playerId);
                    return;
                } else if (!sub.contains("/")) {
                    long playerId = Long.parseLong(sub);
                    PlayerHandler.handlePlayerDetail(exchange, user, playerId);
                    return;
                }
            }

            // Bosses
            if (path.equals("/api/bosses")) {
                BossHandler.handleList(exchange, user);
                return;
            }

            if (path.equals("/api/bosses/action")) {
                BossHandler.handleAction(exchange, user);
                return;
            }

            // Server Controls
            if (path.equals("/api/server/maintenance")) {
                ServerControlHandler.handleMaintenance(exchange, user);
                return;
            }

            if (path.equals("/api/server/admin-only")) {
                ServerControlHandler.handleAdminOnly(exchange, user);
                return;
            }

            if (path.equals("/api/server/rates")) {
                ServerControlHandler.handleRates(exchange, user);
                return;
            }

            if (path.equals("/api/server/broadcast")) {
                ServerControlHandler.handleBroadcast(exchange, user);
                return;
            }

            if (path.equals("/api/server/command")) {
                ServerControlHandler.handleCommand(exchange, user);
                return;
            }

            // WebAdmin Modules
            if (path.equals("/api/admin-native")) {
                NativeAdminHandler.handle(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/giftcodes")) {
                WebAdminHandler.handleGiftcodes(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/mailboxes")) {
                WebAdminHandler.handleMailboxes(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/accounts")) {
                WebAdminHandler.handleAccounts(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/transactions")) {
                WebAdminHandler.handleTransactions(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/posts")) {
                WebAdminHandler.handlePosts(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/settings")) {
                WebAdminHandler.handleSettings(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/events")) {
                WebAdminHandler.handleEvents(exchange, user);
                return;
            }

            if (path.equals("/api/webadmin/logs")) {
                WebAdminHandler.handleLogs(exchange, user);
                return;
            }

            // Catalogs
            if (path.equals("/api/catalog/items")) {
                CatalogHandler.handleItems(exchange, user, queryParams.get("search"));
                return;
            }

            if (path.equals("/api/catalog/options")) {
                CatalogHandler.handleOptions(exchange, user);
                return;
            }

            // Logs
            if (path.equals("/api/logs")) {
                int limit = Integer.parseInt(queryParams.getOrDefault("limit", "100"));
                String level = queryParams.getOrDefault("level", "ALL");
                LogHandler.handle(exchange, user, limit, level);
                return;
            }

            JsonResponse.notFound(exchange, "Không tìm thấy endpoint: " + path);
        } catch (NumberFormatException e) {
            JsonResponse.badRequest(exchange, "Định dạng ID số không hợp lệ");
        } catch (Exception e) {
            Logger.logException(HttpRouter.class, e, "Lỗi xử lý request HTTP Control API");
            JsonResponse.serverError(exchange, "Lỗi máy chủ nội bộ: " + e.getMessage());
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                params.put(pair[0], java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                params.put(pair[0], "");
            }
        }
        return params;
    }
}
