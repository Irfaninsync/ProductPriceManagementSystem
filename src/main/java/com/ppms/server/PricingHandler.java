// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.db.SqliteBackupService;
import com.ppms.model.PriceChangeReason;
import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.SettingsRepository;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP handler for single/bulk price proposals, price updates, and historical price audit queries.
 */
public class PricingHandler {

    private final PricingService pricingService;
    private final ProductService productService;
    private final SqliteBackupService backupService;
    private final SettingsRepository settingsRepository;

    public PricingHandler(PricingService pricingService,
                          ProductService productService,
                          SqliteBackupService backupService,
                          SettingsRepository settingsRepository) {
        this.pricingService = pricingService;
        this.productService = productService;
        this.backupService = backupService;
        this.settingsRepository = settingsRepository;
    }

    public void handlePricing(HttpExchange exchange, String subPath) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        switch (subPath) {
            case "/reasons" -> {
                if (!"GET".equals(method)) {
                    HttpContextHelper.sendError(exchange, 405, "Method not allowed");
                    return;
                }
                List<Map<String, String>> reasons = new ArrayList<>();
                for (PriceChangeReason r : PriceChangeReason.values()) {
                    reasons.add(Map.of("code", r.name(), "label", r.getDescription()));
                }
                HttpContextHelper.sendJson(exchange, 200, Map.of("items", reasons));
            }
            case "/preview" -> {
                if (!"POST".equals(method)) {
                    HttpContextHelper.sendError(exchange, 405, "Method not allowed");
                    return;
                }
                previewSingle(exchange);
            }
            case "/apply" -> {
                if (!"POST".equals(method)) {
                    HttpContextHelper.sendError(exchange, 405, "Method not allowed");
                    return;
                }
                applySingle(exchange);
            }
            case "/bulk/preview" -> {
                if (!"POST".equals(method)) {
                    HttpContextHelper.sendError(exchange, 405, "Method not allowed");
                    return;
                }
                previewBulk(exchange);
            }
            case "/bulk/apply" -> {
                if (!"POST".equals(method)) {
                    HttpContextHelper.sendError(exchange, 405, "Method not allowed");
                    return;
                }
                applyBulk(exchange);
            }
            default -> HttpContextHelper.sendError(exchange, 404, "Pricing endpoint not found");
        }
    }

    public void handleHistory(HttpExchange exchange, String subPath) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }
        Map<String, String> q = HttpContextHelper.parseQueryParams(exchange);
        String productId = q.getOrDefault("productId", "").trim();
        String reason = q.getOrDefault("reason", "").trim();
        String startStr = q.getOrDefault("startDate", "").trim();
        String endStr = q.getOrDefault("endDate", "").trim();

        LocalDateTime startDt = startStr.isEmpty() ? null : DateTimeUtil.parseDateOnly(startStr).atStartOfDay();
        LocalDateTime endDt = endStr.isEmpty() ? null : DateTimeUtil.parseDateOnly(endStr).atTime(23, 59, 59);

        List<PriceHistory> records;
        if (!productId.isEmpty()) {
            records = pricingService.getHistoryForProduct(productId);
        } else if (startDt != null || endDt != null) {
            records = pricingService.getHistoryByDateRange(startDt, endDt);
        } else {
            records = pricingService.getAllHistory();
        }

        if (!productId.isEmpty() && (startDt != null || endDt != null)) {
            records = records.stream()
                    .filter(h -> (startDt == null || !h.getChangedAt().isBefore(startDt))
                            && (endDt == null || !h.getChangedAt().isAfter(endDt)))
                    .toList();
        }

        if (!reason.isEmpty()) {
            String lowerReason = reason.toLowerCase(Locale.ROOT);
            records = records.stream()
                    .filter(h -> h.getReason().toLowerCase(Locale.ROOT).contains(lowerReason))
                    .toList();
        }

        // Most recent first for ledger clarity
        List<PriceHistory> reversed = new ArrayList<>(records);
        Collections.reverse(reversed);

        Map<String, String> nameMap = new LinkedHashMap<>();
        for (Product p : productService.getAllProducts()) {
            nameMap.put(p.getProductId().toUpperCase(Locale.ROOT), p.getName());
        }

        int page = Math.max(1, parseIntOrDefault(q.get("page"), 1));
        int pageSize = Math.max(1, Math.min(200, parseIntOrDefault(q.get("pageSize"), 25)));
        int totalCount = reversed.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        int safePage = Math.min(page, totalPages);
        int start = (safePage - 1) * pageSize;
        int end = Math.min(start + pageSize, totalCount);
        List<PriceHistory> slice = start < totalCount ? reversed.subList(start, end) : List.of();

        List<Map<String, Object>> items = slice.stream().map(h -> {
            Map<String, Object> m = HttpContextHelper.toMap(h);
            m.put("productName", nameMap.getOrDefault(h.getProductId().toUpperCase(Locale.ROOT), h.getProductId()));
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("page", safePage);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        result.put("largestIncrease", pricingService.getLargestPriceIncrease().map(h -> {
            Map<String, Object> m = HttpContextHelper.toMap(h);
            m.put("productName", nameMap.getOrDefault(h.getProductId().toUpperCase(Locale.ROOT), h.getProductId()));
            return m;
        }).orElse(null));
        result.put("largestDecrease", pricingService.getLargestPriceDecrease().map(h -> {
            Map<String, Object> m = HttpContextHelper.toMap(h);
            m.put("productName", nameMap.getOrDefault(h.getProductId().toUpperCase(Locale.ROOT), h.getProductId()));
            return m;
        }).orElse(null));

        HttpContextHelper.sendJson(exchange, 200, result);
    }

    private void previewSingle(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String productId = JsonCodec.getStringOrDefault(body, "productId", "");
        BigDecimal proposedPrice = JsonCodec.getBigDecimal(body, "proposedPrice");
        String reason = JsonCodec.getStringOrDefault(body, "reason", "");

        PriceProposal proposal = pricingService.createProposal(productId, proposedPrice, reason);
        HttpContextHelper.sendJson(exchange, 200, HttpContextHelper.toMap(proposal));
    }

    private void applySingle(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String productId = JsonCodec.getStringOrDefault(body, "productId", "");
        BigDecimal proposedPrice = JsonCodec.getBigDecimal(body, "proposedPrice");
        String reason = JsonCodec.getStringOrDefault(body, "reason", "");

        PriceProposal proposal = pricingService.createProposal(productId, proposedPrice, reason);
        if (!proposal.isValid()) {
            HttpContextHelper.sendError(exchange, 400, proposal.getValidationResult().getFormattedErrors());
            return;
        }
        PriceHistory history = pricingService.applyPriceChange(proposal);
        Product updated = productService.getProductById(productId).orElseThrow();

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "history", HttpContextHelper.toMap(history),
                "product", HttpContextHelper.toMap(updated)
        ));
    }

    private void previewBulk(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        List<Product> targets = resolveBulkTargets(body);
        BigDecimal percentage = JsonCodec.getBigDecimal(body, "percentage");
        String reason = JsonCodec.getStringOrDefault(body, "reason", "");
        if (percentage == null) {
            throw new IllegalArgumentException("Percentage adjustment is required.");
        }
        if (percentage.compareTo(new BigDecimal("-99.00")) < 0 || percentage.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new IllegalArgumentException("Percentage adjustment must be between -99% and +1000%.");
        }

        List<PriceProposal> proposals = pricingService.previewBulkAdjustment(targets, percentage, reason);
        long validCount = proposals.stream().filter(PriceProposal::isValid).count();

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "proposals", proposals.stream().map(HttpContextHelper::toMap).toList(),
                "totalCount", proposals.size(),
                "validCount", validCount
        ));
    }

    private void applyBulk(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        List<Product> targets = resolveBulkTargets(body);
        BigDecimal percentage = JsonCodec.getBigDecimal(body, "percentage");
        String reason = JsonCodec.getStringOrDefault(body, "reason", "");
        if (percentage == null) {
            throw new IllegalArgumentException("Percentage adjustment is required.");
        }

        List<PriceProposal> proposals = pricingService.previewBulkAdjustment(targets, percentage, reason);
        long validCount = proposals.stream().filter(PriceProposal::isValid).count();
        if (validCount == 0) {
            throw new IllegalArgumentException("No valid proposals to apply.");
        }

        if (backupService != null) {
            backupService.createBackup("Pre-bulk price adjustment snapshot (" + percentage.toPlainString() + "%)");
        }

        List<PriceHistory> applied = pricingService.applyBulkAdjustments(proposals);
        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "appliedCount", applied.size(),
                "items", applied.stream().map(HttpContextHelper::toMap).toList()
        ));
    }

    private List<Product> resolveBulkTargets(Map<String, Object> body) {
        String scope = JsonCodec.getStringOrDefault(body, "scope", "ALL").toUpperCase(Locale.ROOT);
        int threshold = settingsRepository.getInt("low_stock_threshold", 10);
        if ("CATEGORY".equals(scope)) {
            String category = JsonCodec.getStringOrDefault(body, "category", "");
            if (category.trim().isEmpty()) {
                throw new IllegalArgumentException("Category is required when scope is CATEGORY.");
            }
            return productService.filter(category.trim(), ProductStatus.ACTIVE, null, null, null, null, null, null, threshold);
        }
        return productService.getActiveProducts();
    }

    private static int parseIntOrDefault(String raw, int def) {
        if (raw == null || raw.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
