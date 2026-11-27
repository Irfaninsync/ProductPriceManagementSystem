// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.CategorySummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.persistence.CsvWriter;
import com.ppms.repository.SettingsRepository;
import com.ppms.service.AnalyticsService;
import com.ppms.service.CategoryService;
import com.ppms.service.ImportExportService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.DateTimeUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP handler for business reports (catalogue, category summary, price comparison,
 * history, valuation) and RFC-4180 CSV downloads.
 */
public class ReportsHandler {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final PricingService pricingService;
    private final AnalyticsService analyticsService;
    private final ImportExportService importExportService;
    private final SettingsRepository settingsRepository;

    public ReportsHandler(ProductService productService,
                          CategoryService categoryService,
                          PricingService pricingService,
                          AnalyticsService analyticsService,
                          ImportExportService importExportService,
                          SettingsRepository settingsRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.pricingService = pricingService;
        this.analyticsService = analyticsService;
        this.importExportService = importExportService;
        this.settingsRepository = settingsRepository;
    }

    public void handle(HttpExchange exchange, String subPath) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        switch (subPath) {
            case "/catalogue" -> sendCatalogueReport(exchange);
            case "/categories" -> sendCategoryReport(exchange);
            case "/price-comparison" -> sendPriceComparisonReport(exchange);
            case "/price-history" -> sendPriceHistoryReport(exchange);
            case "/valuation" -> sendValuationReport(exchange);
            case "/export/catalogue" -> exportCatalogueCsv(exchange);
            case "/export/categories" -> exportCategoriesCsv(exchange);
            case "/export/history" -> exportHistoryCsv(exchange);
            case "/export/price-comparison" -> exportPriceComparisonCsv(exchange);
            default -> HttpContextHelper.sendError(exchange, 404, "Report not found");
        }
    }

    private void sendCatalogueReport(HttpExchange exchange) throws IOException {
        List<Product> products = productService.sort(productService.getAllProducts(), "name", true);
        int threshold = settingsRepository.getInt("low_stock_threshold", 10);
        AnalyticsSummary summary = analyticsService.computeSummary(threshold);

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "items", products.stream().map(HttpContextHelper::toMap).toList(),
                "summary", summaryMap(summary, threshold)
        ));
    }

    private void sendCategoryReport(HttpExchange exchange) throws IOException {
        List<CategorySummary> categories = categoryService.getCategorySummaries(null);
        BigDecimal totalCost = categories.stream().map(CategorySummary::getTotalCostValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRetail = categories.stream().map(CategorySummary::getTotalSellingValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMargin = totalRetail.subtract(totalCost);
        int totalUnits = categories.stream().mapToInt(CategorySummary::getTotalUnits).sum();

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "items", categories.stream().map(HttpContextHelper::toMap).toList(),
                "totals", Map.of(
                        "categoryCount", categories.size(),
                        "totalUnits", totalUnits,
                        "totalCostValue", totalCost,
                        "totalSellingValue", totalRetail,
                        "totalMarginAmount", totalMargin
                )
        ));
    }

    private void sendPriceComparisonReport(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> rows = buildPriceComparisonRows();
        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "items", rows,
                "totalCount", rows.size()
        ));
    }

    private List<Map<String, Object>> buildPriceComparisonRows() {
        List<Product> products = productService.sort(productService.getActiveProducts(), "name", true);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Product p : products) {
            List<PriceHistory> hist = pricingService.getHistoryForProduct(p.getProductId());
            PriceHistory latest = hist.isEmpty() ? null : hist.get(hist.size() - 1);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", p.getProductId());
            m.put("name", p.getName());
            m.put("category", p.getCategory());
            m.put("costPrice", p.getCostPrice());
            m.put("currentPrice", p.getSellingPrice());
            m.put("marginAmount", p.getGrossMarginAmount());
            m.put("marginPercentage", p.getGrossMarginPercentage().orElse(null));
            m.put("markupPercentage", p.getMarkupPercentage().orElse(null));
            m.put("previousPrice", latest != null ? latest.getOldPrice() : null);
            m.put("lastDelta", latest != null ? latest.getPriceDifference() : null);
            m.put("lastDeltaPercentage", latest != null ? latest.getPercentageChange().orElse(null) : null);
            m.put("lastChangedAt", latest != null ? DateTimeUtil.formatIso(latest.getChangedAt()) : null);
            m.put("lastReason", latest != null ? latest.getReason() : null);
            m.put("changeCount", hist.size());
            rows.add(m);
        }
        return rows;
    }

    private void sendPriceHistoryReport(HttpExchange exchange) throws IOException {
        List<PriceHistory> all = new ArrayList<>(pricingService.getAllHistory());
        Collections.reverse(all);

        Map<String, String> nameMap = new LinkedHashMap<>();
        for (Product p : productService.getAllProducts()) {
            nameMap.put(p.getProductId().toUpperCase(Locale.ROOT), p.getName());
        }

        List<Map<String, Object>> items = all.stream().map(h -> {
            Map<String, Object> m = HttpContextHelper.toMap(h);
            m.put("productName", nameMap.getOrDefault(h.getProductId().toUpperCase(Locale.ROOT), h.getProductId()));
            return m;
        }).toList();

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "items", items,
                "totalCount", items.size()
        ));
    }

    private void sendValuationReport(HttpExchange exchange) throws IOException {
        int threshold = settingsRepository.getInt("low_stock_threshold", 10);
        AnalyticsSummary summary = analyticsService.computeSummary(threshold);

        List<Product> active = productService.getActiveProducts();
        List<Product> topMargins = active.stream()
                .sorted(Comparator.comparing((Product p) -> p.getGrossMarginPercentage().orElse(BigDecimal.ZERO)).reversed())
                .limit(5)
                .toList();
        List<Product> lowestMargins = active.stream()
                .sorted(Comparator.comparing((Product p) -> p.getGrossMarginPercentage().orElse(BigDecimal.ZERO)))
                .limit(5)
                .toList();

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "summary", summaryMap(summary, threshold),
                "belowCostProducts", analyticsService.getBelowCostProducts().stream().map(HttpContextHelper::toMap).toList(),
                "lowStockProducts", analyticsService.getLowStockProducts(threshold).stream().map(HttpContextHelper::toMap).toList(),
                "outOfStockProducts", analyticsService.getOutOfStockProducts().stream().map(HttpContextHelper::toMap).toList(),
                "topMarginProducts", topMargins.stream().map(HttpContextHelper::toMap).toList(),
                "lowestMarginProducts", lowestMargins.stream().map(HttpContextHelper::toMap).toList()
        ));
    }

    private void exportCatalogueCsv(HttpExchange exchange) throws IOException {
        String csv = importExportService.exportProductsToCsvString(productService.getAllProducts());
        String filename = "ppms_catalogue_" + LocalDate.now() + ".csv";
        HttpContextHelper.sendCsvDownload(exchange, filename, csv);
    }

    private void exportCategoriesCsv(HttpExchange exchange) throws IOException {
        String csv = importExportService.exportCategorySummaryToCsvString(categoryService.getCategorySummaries(null));
        String filename = "ppms_categories_" + LocalDate.now() + ".csv";
        HttpContextHelper.sendCsvDownload(exchange, filename, csv);
    }

    private void exportHistoryCsv(HttpExchange exchange) throws IOException {
        String csv = importExportService.exportPriceHistoryToCsvString(pricingService.getAllHistory());
        String filename = "ppms_price_history_" + LocalDate.now() + ".csv";
        HttpContextHelper.sendCsvDownload(exchange, filename, csv);
    }

    private void exportPriceComparisonCsv(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> rows = buildPriceComparisonRows();
        List<List<String>> csvRows = new ArrayList<>();
        csvRows.add(List.of("productId", "name", "category", "costPrice", "currentPrice",
                "marginAmount", "marginPercentage", "previousPrice", "lastDelta", "lastDeltaPercentage",
                "lastChangedAt", "lastReason", "changeCount"));

        for (Map<String, Object> r : rows) {
            csvRows.add(List.of(
                    str(r.get("productId")),
                    str(r.get("name")),
                    str(r.get("category")),
                    str(r.get("costPrice")),
                    str(r.get("currentPrice")),
                    str(r.get("marginAmount")),
                    str(r.get("marginPercentage")),
                    str(r.get("previousPrice")),
                    str(r.get("lastDelta")),
                    str(r.get("lastDeltaPercentage")),
                    str(r.get("lastChangedAt")),
                    str(r.get("lastReason")),
                    str(r.get("changeCount"))
            ));
        }
        StringWriter sw = new StringWriter();
        CsvWriter.writeAll(sw, csvRows);
        String filename = "ppms_price_comparison_" + LocalDate.now() + ".csv";
        HttpContextHelper.sendCsvDownload(exchange, filename, sw.toString());
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Map<String, Object> summaryMap(AnalyticsSummary summary, int threshold) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalProducts", summary.getTotalProducts());
        m.put("activeProducts", summary.getActiveProductsCount());
        m.put("archivedProducts", summary.getArchivedProductsCount());
        m.put("totalStockUnits", summary.getTotalStockUnits());
        m.put("minSellingPrice", summary.getMinSellingPrice());
        m.put("maxSellingPrice", summary.getMaxSellingPrice());
        m.put("avgSellingPrice", summary.getAvgSellingPrice());
        m.put("medianSellingPrice", summary.getMedianSellingPrice());
        m.put("totalInventoryCostValue", summary.getTotalInventoryCostValue());
        m.put("totalInventorySellingValue", summary.getTotalInventorySellingValue());
        m.put("totalGrossMarginAmount", summary.getTotalGrossMarginAmount());
        m.put("overallGrossMarginPercentage", summary.getOverallGrossMarginPercentage().orElse(null));
        m.put("overallMarkupPercentage", summary.getOverallMarkupPercentage().orElse(null));
        m.put("belowCostCount", summary.getBelowCostCount());
        m.put("lowStockCount", summary.getLowStockCount());
        m.put("outOfStockCount", summary.getOutOfStockCount());
        m.put("totalHistoryRecords", summary.getTotalHistoryRecords());
        m.put("lowStockThreshold", threshold);
        return m;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
