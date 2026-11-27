// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.SettingsRepository;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP handler for Product CRUD, search, multi-criteria filtering, sorting, archive/restore, and details.
 */
public class ProductHandler {

    private final ProductService productService;
    private final PricingService pricingService;
    private final SettingsRepository settingsRepository;

    public ProductHandler(ProductService productService,
                          PricingService pricingService,
                          SettingsRepository settingsRepository) {
        this.productService = productService;
        this.pricingService = pricingService;
        this.settingsRepository = settingsRepository;
    }

    public void handle(HttpExchange exchange, String subPath) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (subPath.isEmpty() || "/".equals(subPath)) {
            switch (method) {
                case "GET" -> listProducts(exchange);
                case "POST" -> createProduct(exchange);
                default -> HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            }
            return;
        }

        String trimmed = subPath.startsWith("/") ? subPath.substring(1) : subPath;
        String[] parts = trimmed.split("/");
        String productId = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);

        if (parts.length == 1) {
            switch (method) {
                case "GET" -> getProductDetails(exchange, productId);
                case "PUT" -> updateProduct(exchange, productId);
                case "DELETE" -> deleteProduct(exchange, productId);
                default -> HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            }
        } else if (parts.length == 2 && "POST".equals(method)) {
            if ("archive".equalsIgnoreCase(parts[1])) {
                archiveProduct(exchange, productId);
            } else if ("restore".equalsIgnoreCase(parts[1])) {
                restoreProduct(exchange, productId);
            } else {
                HttpContextHelper.sendError(exchange, 404, "Unknown product action");
            }
        } else {
            HttpContextHelper.sendError(exchange, 404, "Endpoint not found");
        }
    }

    private void listProducts(HttpExchange exchange) throws IOException {
        Map<String, String> q = HttpContextHelper.parseQueryParams(exchange);
        int threshold = settingsRepository.getInt("low_stock_threshold", 10);

        String searchQuery = q.getOrDefault("q", "").trim();
        String category = q.getOrDefault("category", "").trim();
        String statusStr = q.getOrDefault("status", "ACTIVE").trim().toUpperCase(Locale.ROOT);
        ProductStatus status = switch (statusStr) {
            case "ALL" -> null;
            case "ARCHIVED" -> ProductStatus.ARCHIVED;
            default -> ProductStatus.ACTIVE;
        };

        BigDecimal minPrice = parseOptionalDecimal(q.get("minPrice"));
        BigDecimal maxPrice = parseOptionalDecimal(q.get("maxPrice"));
        Integer minQty = parseOptionalInt(q.get("minQty"));
        Integer maxQty = parseOptionalInt(q.get("maxQty"));
        boolean belowCost = "true".equalsIgnoreCase(q.get("belowCost"));
        boolean lowStock = "true".equalsIgnoreCase(q.get("lowStock"));
        boolean outOfStock = "true".equalsIgnoreCase(q.get("outOfStock"));

        List<Product> filtered = productService.filter(
                category.isEmpty() ? null : category,
                status,
                minPrice,
                maxPrice,
                minQty,
                maxQty,
                belowCost ? Boolean.TRUE : null,
                lowStock ? Boolean.TRUE : null,
                threshold
        );

        if (outOfStock) {
            filtered = filtered.stream().filter(Product::isOutOfStock).toList();
        }

        if (!searchQuery.isEmpty()) {
            String lower = searchQuery.toLowerCase(Locale.ROOT);
            filtered = filtered.stream()
                    .filter(p -> p.getProductId().toLowerCase(Locale.ROOT).contains(lower)
                            || p.getName().toLowerCase(Locale.ROOT).contains(lower)
                            || p.getCategory().toLowerCase(Locale.ROOT).contains(lower)
                            || (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(lower)))
                    .toList();
        }

        String sortBy = q.getOrDefault("sortBy", "updated");
        boolean asc = !"false".equalsIgnoreCase(q.getOrDefault("asc", "false"));
        List<Product> sorted = productService.sort(filtered, sortBy, asc);

        int page = Math.max(1, parseIntOrDefault(q.get("page"), 1));
        int pageSize = Math.max(1, Math.min(200, parseIntOrDefault(q.get("pageSize"), 25)));
        int totalCount = sorted.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        int safePage = Math.min(page, totalPages);
        int start = (safePage - 1) * pageSize;
        int end = Math.min(start + pageSize, totalCount);
        List<Product> slice = start < totalCount ? sorted.subList(start, end) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", slice.stream().map(HttpContextHelper::toMap).toList());
        result.put("totalCount", totalCount);
        result.put("page", safePage);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        result.put("lowStockThreshold", threshold);

        HttpContextHelper.sendJson(exchange, 200, result);
    }

    private void getProductDetails(HttpExchange exchange, String productId) throws IOException {
        Optional<Product> opt = productService.getProductById(productId);
        if (opt.isEmpty()) {
            HttpContextHelper.sendError(exchange, 404, "Product not found: " + productId);
            return;
        }
        Product product = opt.get();
        List<PriceHistory> history = pricingService.getHistoryForProduct(product.getProductId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("product", HttpContextHelper.toMap(product));
        payload.put("priceHistory", history.stream().map(HttpContextHelper::toMap).toList());
        payload.put("lowStockThreshold", settingsRepository.getInt("low_stock_threshold", 10));
        HttpContextHelper.sendJson(exchange, 200, payload);
    }

    private void createProduct(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String customId = JsonCodec.getString(body, "productId");
        String name = JsonCodec.getStringOrDefault(body, "name", "");
        String category = JsonCodec.getStringOrDefault(body, "category", "");
        String description = JsonCodec.getStringOrDefault(body, "description", "");
        BigDecimal costPrice = JsonCodec.getBigDecimal(body, "costPrice");
        BigDecimal sellingPrice = JsonCodec.getBigDecimal(body, "sellingPrice");
        Integer qty = JsonCodec.getInt(body, "quantity");
        if (qty == null) {
            throw new IllegalArgumentException("Stock quantity is required.");
        }

        Product created;
        if (customId == null || customId.trim().isEmpty()) {
            created = productService.addProduct(name, category, description, costPrice, sellingPrice, qty);
        } else {
            created = productService.addProductWithId(customId.trim(), name, category, description, costPrice, sellingPrice, qty);
        }
        HttpContextHelper.sendJson(exchange, 201, HttpContextHelper.toMap(created));
    }

    private void updateProduct(HttpExchange exchange, String productId) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String name = JsonCodec.getStringOrDefault(body, "name", "");
        String category = JsonCodec.getStringOrDefault(body, "category", "");
        String description = JsonCodec.getStringOrDefault(body, "description", "");
        BigDecimal costPrice = JsonCodec.getBigDecimal(body, "costPrice");
        Integer qty = JsonCodec.getInt(body, "quantity");
        if (qty == null) {
            throw new IllegalArgumentException("Stock quantity is required.");
        }

        Product updated = productService.updateProductFull(productId, name, category, description, costPrice, qty);
        HttpContextHelper.sendJson(exchange, 200, HttpContextHelper.toMap(updated));
    }

    private void archiveProduct(HttpExchange exchange, String productId) throws IOException {
        Product archived = productService.archiveProduct(productId);
        HttpContextHelper.sendJson(exchange, 200, HttpContextHelper.toMap(archived));
    }

    private void restoreProduct(HttpExchange exchange, String productId) throws IOException {
        Product restored = productService.restoreProduct(productId);
        HttpContextHelper.sendJson(exchange, 200, HttpContextHelper.toMap(restored));
    }

    private void deleteProduct(HttpExchange exchange, String productId) throws IOException {
        productService.deleteProduct(productId);
        HttpContextHelper.sendJson(exchange, 200, Map.of("deleted", true, "productId", productId));
    }

    private static BigDecimal parseOptionalDecimal(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        return new BigDecimal(raw.trim());
    }

    private static Integer parseOptionalInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        return Integer.parseInt(raw.trim());
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
