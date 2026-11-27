// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Locale;

/**
 * Central API router enforcing loopback origin/session security and consistent HTTP error mapping.
 */
public class ApiRouter implements HttpHandler {

    private final String sessionToken;
    private final DashboardHandler dashboardHandler;
    private final ProductHandler productHandler;
    private final CategoryHandler categoryHandler;
    private final PricingHandler pricingHandler;
    private final ReportsHandler reportsHandler;
    private final DataAndSettingsHandler dataAndSettingsHandler;

    public ApiRouter(String sessionToken,
                     DashboardHandler dashboardHandler,
                     ProductHandler productHandler,
                     CategoryHandler categoryHandler,
                     PricingHandler pricingHandler,
                     ReportsHandler reportsHandler,
                     DataAndSettingsHandler dataAndSettingsHandler) {
        this.sessionToken = sessionToken;
        this.dashboardHandler = dashboardHandler;
        this.productHandler = productHandler;
        this.categoryHandler = categoryHandler;
        this.pricingHandler = pricingHandler;
        this.reportsHandler = reportsHandler;
        this.dataAndSettingsHandler = dataAndSettingsHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isAllowedLoopbackRequest(exchange)) {
                HttpContextHelper.sendError(exchange, 403, "Forbidden: invalid local origin or missing session token.");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if ("/api/dashboard".equals(path)) {
                dashboardHandler.handle(exchange);
            } else if ("/api/products".equals(path) || path.startsWith("/api/products/")) {
                String sub = path.substring("/api/products".length());
                productHandler.handle(exchange, sub);
            } else if ("/api/categories".equals(path) || path.startsWith("/api/categories/")) {
                String sub = path.substring("/api/categories".length());
                categoryHandler.handle(exchange, sub);
            } else if (path.startsWith("/api/pricing/")) {
                String sub = path.substring("/api/pricing".length());
                pricingHandler.handlePricing(exchange, sub);
            } else if ("/api/history".equals(path)) {
                pricingHandler.handleHistory(exchange, "");
            } else if (path.startsWith("/api/reports/")) {
                String sub = path.substring("/api/reports".length());
                reportsHandler.handle(exchange, sub);
            } else if ("/api/settings".equals(path)) {
                dataAndSettingsHandler.handleSettings(exchange);
            } else if ("/api/backups".equals(path) || path.startsWith("/api/backups/")) {
                String sub = path.substring("/api/backups".length());
                dataAndSettingsHandler.handleBackups(exchange, sub);
            } else if (path.startsWith("/api/import/")) {
                String sub = path.substring("/api/import".length());
                dataAndSettingsHandler.handleImport(exchange, sub);
            } else if (path.startsWith("/api/data/")) {
                String sub = path.substring("/api/data".length());
                dataAndSettingsHandler.handleDataActions(exchange, sub);
            } else if (path.startsWith("/api/system/")) {
                String sub = path.substring("/api/system".length());
                dataAndSettingsHandler.handleSystem(exchange, sub);
            } else {
                HttpContextHelper.sendError(exchange, 404, "API endpoint not found: " + path);
            }
        } catch (IllegalArgumentException e) {
            HttpContextHelper.sendError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            HttpContextHelper.sendError(exchange, 409, e.getMessage());
        } catch (SecurityException e) {
            HttpContextHelper.sendError(exchange, 403, e.getMessage());
        } catch (Exception e) {
            System.err.println("[PPMS API Error] " + exchange.getRequestMethod() + " "
                    + exchange.getRequestURI() + ": " + e.getMessage());
            HttpContextHelper.sendError(exchange, 500, "Internal application error: " + e.getMessage());
        }
    }

    private boolean isAllowedLoopbackRequest(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        String host = headers.getFirst("Host");
        if (host != null && !isLocalHostValue(host)) {
            return false;
        }
        String origin = headers.getFirst("Origin");
        if (origin != null && !isLocalOriginValue(origin)) {
            return false;
        }
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method)) {
            String token = headers.getFirst("X-PPMS-Token");
            return sessionToken.equals(token);
        }
        return true;
    }

    private boolean isLocalHostValue(String host) {
        String lower = host.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("127.0.0.1:") || lower.equals("127.0.0.1")
                || lower.startsWith("localhost:") || lower.equals("localhost");
    }

    private boolean isLocalOriginValue(String origin) {
        String lower = origin.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("http://127.0.0.1:") || lower.equals("http://127.0.0.1")
                || lower.startsWith("http://localhost:") || lower.equals("http://localhost");
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
