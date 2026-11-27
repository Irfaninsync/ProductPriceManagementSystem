// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.model.ActivityEntry;
import com.ppms.model.BackupSnapshot;
import com.ppms.model.CategorySummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared HTTP request parsing, response serialization, and DTO mapping helpers.
 */
public final class HttpContextHelper {

    public static final int MAX_BODY_BYTES = 5 * 1024 * 1024; // 5 MB max local payload

    private HttpContextHelper() {}

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_BODY_BYTES) {
                    throw new IllegalArgumentException("Request payload exceeds maximum allowed size (5 MB).");
                }
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    public static Map<String, Object> readJsonObject(HttpExchange exchange) throws IOException {
        String raw = readBody(exchange);
        return JsonCodec.parseObject(raw);
    }

    public static Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(k, v);
            } else if (!pair.isEmpty()) {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = JsonCodec.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", message != null ? message : "Unexpected error");
        err.put("status", statusCode);
        sendJson(exchange, statusCode, err);
    }

    public static void sendCsvDownload(HttpExchange exchange, String fileName, String csvContent) throws IOException {
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        String safeFile = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + safeFile + "\"");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("category", p.getCategory());
        m.put("description", p.getDescription());
        m.put("costPrice", p.getCostPrice());
        m.put("sellingPrice", p.getSellingPrice());
        m.put("quantity", p.getQuantity());
        m.put("status", p.getStatus().name());
        m.put("belowCost", p.isBelowCost());
        m.put("outOfStock", p.isOutOfStock());
        m.put("grossMarginAmount", p.getGrossMarginAmount());
        m.put("grossMarginPercentage", p.getGrossMarginPercentage().orElse(null));
        m.put("markupPercentage", p.getMarkupPercentage().orElse(null));
        m.put("inventoryCostValue", p.getInventoryCostValue());
        m.put("inventorySellingValue", p.getInventorySellingValue());
        m.put("createdAt", DateTimeUtil.formatIso(p.getCreatedAt()));
        m.put("updatedAt", DateTimeUtil.formatIso(p.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> toMap(PriceHistory h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("historyId", h.getHistoryId());
        m.put("productId", h.getProductId());
        m.put("oldPrice", h.getOldPrice());
        m.put("newPrice", h.getNewPrice());
        m.put("difference", h.getPriceDifference());
        m.put("percentageChange", h.getPercentageChange().orElse(null));
        m.put("isIncrease", h.isPriceIncrease());
        m.put("isDecrease", h.isPriceDecrease());
        m.put("changedAt", DateTimeUtil.formatIso(h.getChangedAt()));
        m.put("reason", h.getReason());
        return m;
    }

    public static Map<String, Object> toMap(PriceProposal proposal) {
        Product p = proposal.getProduct();
        BigDecimal newMargin = proposal.getProposedPrice().subtract(p.getCostPrice());
        BigDecimal newMarginPct = proposal.getProposedPrice().compareTo(BigDecimal.ZERO) > 0
                ? newMargin.divide(proposal.getProposedPrice(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                : null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", p.getProductId());
        m.put("productName", p.getName());
        m.put("category", p.getCategory());
        m.put("costPrice", p.getCostPrice());
        m.put("currentPrice", proposal.getCurrentPrice());
        m.put("proposedPrice", proposal.getProposedPrice());
        m.put("difference", proposal.getDifference());
        m.put("percentageChange", proposal.getPercentageChange().orElse(null));
        m.put("newMarginAmount", newMargin);
        m.put("newMarginPercentage", newMarginPct);
        m.put("belowCost", proposal.getProposedPrice().compareTo(p.getCostPrice()) < 0);
        m.put("reason", proposal.getReason());
        m.put("valid", proposal.isValid());
        m.put("errors", proposal.getValidationResult() != null
                ? proposal.getValidationResult().getErrors()
                : java.util.List.of());
        return m;
    }

    public static Map<String, Object> toMap(CategorySummary c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categoryId", c.getCategoryId());
        m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("totalProducts", c.getTotalProducts());
        m.put("activeProducts", c.getActiveProducts());
        m.put("archivedProducts", c.getArchivedProducts());
        m.put("totalUnits", c.getTotalUnits());
        m.put("totalCostValue", c.getTotalCostValue());
        m.put("totalSellingValue", c.getTotalSellingValue());
        m.put("totalMarginAmount", c.getTotalMarginAmount());
        m.put("marginPercentage", c.getMarginPercentage().orElse(null));
        m.put("avgSellingPrice", c.getAvgSellingPrice());
        m.put("createdAt", DateTimeUtil.formatIso(c.getCreatedAt()));
        m.put("updatedAt", DateTimeUtil.formatIso(c.getUpdatedAt()));
        return m;
    }

    public static Map<String, Object> toMap(ActivityEntry a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityId", a.getActivityId());
        m.put("eventType", a.getEventType());
        m.put("entityType", a.getEntityType());
        m.put("entityId", a.getEntityId());
        m.put("summary", a.getSummary());
        m.put("createdAt", DateTimeUtil.formatIso(a.getCreatedAt()));
        return m;
    }

    public static Map<String, Object> toMap(BackupSnapshot b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fileName", b.getFileName());
        m.put("createdAt", DateTimeUtil.formatIso(b.getCreatedAt()));
        m.put("sizeBytes", b.getSizeBytes());
        m.put("note", b.getNote());
        return m;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
