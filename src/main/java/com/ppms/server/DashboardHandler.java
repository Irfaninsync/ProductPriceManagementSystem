// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.model.ActivityEntry;
import com.ppms.model.AnalyticsSummary;
import com.ppms.model.CategorySummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.repository.ActivityRepository;
import com.ppms.repository.SettingsRepository;
import com.ppms.service.AnalyticsService;
import com.ppms.service.CategoryService;
import com.ppms.service.ProductService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves real-time dashboard metrics, alerts, recent prices, and business activity.
 */
public class DashboardHandler {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final AnalyticsService analyticsService;
    private final ActivityRepository activityRepository;
    private final SettingsRepository settingsRepository;

    public DashboardHandler(ProductService productService,
                            CategoryService categoryService,
                            AnalyticsService analyticsService,
                            ActivityRepository activityRepository,
                            SettingsRepository settingsRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.analyticsService = analyticsService;
        this.activityRepository = activityRepository;
        this.settingsRepository = settingsRepository;
    }

    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }

        int threshold = settingsRepository.getInt("low_stock_threshold", 10);
        AnalyticsSummary summary = analyticsService.computeSummary(threshold);

        List<Product> recentProducts = analyticsService.getRecentlyAddedProducts(5);
        List<PriceHistory> recentPriceUpdates = analyticsService.getRecentPriceChanges(6);
        List<ActivityEntry> recentActivity = activityRepository.findRecent(10);
        List<CategorySummary> categories = categoryService.getCategorySummaries(null);

        Map<String, String> productNames = new LinkedHashMap<>();
        for (Product p : productService.getAllProducts()) {
            productNames.put(p.getProductId().toUpperCase(), p.getName());
        }

        List<Map<String, Object>> recentPriceItems = recentPriceUpdates.stream().map(h -> {
            Map<String, Object> m = HttpContextHelper.toMap(h);
            m.put("productName", productNames.getOrDefault(h.getProductId().toUpperCase(), h.getProductId()));
            return m;
        }).toList();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalProducts", summary.getTotalProducts());
        kpis.put("activeProducts", summary.getActiveProductsCount());
        kpis.put("archivedProducts", summary.getArchivedProductsCount());
        kpis.put("totalCategories", categories.size());
        kpis.put("totalStockUnits", summary.getTotalStockUnits());
        kpis.put("minSellingPrice", summary.getMinSellingPrice());
        kpis.put("maxSellingPrice", summary.getMaxSellingPrice());
        kpis.put("avgSellingPrice", summary.getAvgSellingPrice());
        kpis.put("medianSellingPrice", summary.getMedianSellingPrice());
        kpis.put("totalInventoryCostValue", summary.getTotalInventoryCostValue());
        kpis.put("totalInventorySellingValue", summary.getTotalInventorySellingValue());
        kpis.put("totalGrossMarginAmount", summary.getTotalGrossMarginAmount());
        kpis.put("overallGrossMarginPercentage", summary.getOverallGrossMarginPercentage().orElse(null));
        kpis.put("overallMarkupPercentage", summary.getOverallMarkupPercentage().orElse(null));
        kpis.put("belowCostCount", summary.getBelowCostCount());
        kpis.put("lowStockCount", summary.getLowStockCount());
        kpis.put("outOfStockCount", summary.getOutOfStockCount());
        kpis.put("totalHistoryRecords", summary.getTotalHistoryRecords());
        kpis.put("lowStockThreshold", threshold);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kpis", kpis);
        payload.put("recentProducts", recentProducts.stream().map(HttpContextHelper::toMap).toList());
        payload.put("recentPriceUpdates", recentPriceItems);
        payload.put("recentActivity", recentActivity.stream().map(HttpContextHelper::toMap).toList());
        payload.put("categoryValuation", categories.stream().map(HttpContextHelper::toMap).toList());
        payload.put("priceBands", analyticsService.getActivePriceBands());
        payload.put("belowCostAlerts", analyticsService.getBelowCostProducts().stream().map(HttpContextHelper::toMap).toList());
        payload.put("lowStockAlerts", analyticsService.getLowStockProducts(threshold).stream().map(HttpContextHelper::toMap).toList());
        payload.put("isEmptyCatalog", summary.getTotalProducts() == 0);
        payload.put("hasLegacyCsv", Files.isRegularFile(Paths.get("data", "products.csv")));

        HttpContextHelper.sendJson(exchange, 200, payload);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
