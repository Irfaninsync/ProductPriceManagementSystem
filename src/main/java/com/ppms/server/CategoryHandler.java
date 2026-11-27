// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.model.Category;
import com.ppms.model.CategorySummary;
import com.ppms.service.CategoryService;
import com.ppms.util.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP handler for Category CRUD, search, summary metrics, and safe reassignment deletion.
 */
public class CategoryHandler {

    private final CategoryService categoryService;

    public CategoryHandler(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void handle(HttpExchange exchange, String subPath) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (subPath.isEmpty() || "/".equals(subPath)) {
            switch (method) {
                case "GET" -> listCategories(exchange);
                case "POST" -> createCategory(exchange);
                default -> HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            }
            return;
        }

        String trimmed = subPath.startsWith("/") ? subPath.substring(1) : subPath;
        String categoryId = URLDecoder.decode(trimmed, StandardCharsets.UTF_8);

        switch (method) {
            case "PUT" -> updateCategory(exchange, categoryId);
            case "DELETE" -> deleteCategory(exchange, categoryId);
            default -> HttpContextHelper.sendError(exchange, 405, "Method not allowed");
        }
    }

    private void listCategories(HttpExchange exchange) throws IOException {
        Map<String, String> q = HttpContextHelper.parseQueryParams(exchange);
        String query = q.getOrDefault("q", "");
        List<CategorySummary> summaries = categoryService.getCategorySummaries(query);
        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "items", summaries.stream().map(HttpContextHelper::toMap).toList(),
                "totalCount", summaries.size()
        ));
    }

    private void createCategory(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String name = JsonCodec.getStringOrDefault(body, "name", "");
        String description = JsonCodec.getStringOrDefault(body, "description", "");
        Category created = categoryService.createCategory(name, description);
        CategorySummary summary = categoryService.getCategorySummaries(null).stream()
                .filter(c -> c.getCategoryId().equalsIgnoreCase(created.getCategoryId()))
                .findFirst()
                .orElseThrow();
        HttpContextHelper.sendJson(exchange, 201, HttpContextHelper.toMap(summary));
    }

    private void updateCategory(HttpExchange exchange, String categoryId) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String name = JsonCodec.getStringOrDefault(body, "name", "");
        String description = JsonCodec.getStringOrDefault(body, "description", "");
        Category updated = categoryService.updateCategory(categoryId, name, description);
        CategorySummary summary = categoryService.getCategorySummaries(null).stream()
                .filter(c -> c.getCategoryId().equalsIgnoreCase(updated.getCategoryId()))
                .findFirst()
                .orElseThrow();
        HttpContextHelper.sendJson(exchange, 200, HttpContextHelper.toMap(summary));
    }

    private void deleteCategory(HttpExchange exchange, String categoryId) throws IOException {
        Map<String, String> q = HttpContextHelper.parseQueryParams(exchange);
        String reassignTo = q.get("reassignTo");
        categoryService.deleteCategory(categoryId, reassignTo);
        HttpContextHelper.sendJson(exchange, 200, Map.of("deleted", true, "categoryId", categoryId));
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
