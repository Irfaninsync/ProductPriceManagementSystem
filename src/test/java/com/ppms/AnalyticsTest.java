// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.service.AnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnalyticsTest {

    private static class InMemoryProductRepository implements ProductRepository {
        private final List<Product> list = new ArrayList<>();

        @Override
        public Product save(Product p) {
            list.removeIf(item -> item.getProductId().equalsIgnoreCase(p.getProductId()));
            list.add(new Product(p));
            return new Product(p);
        }

        @Override
        public Optional<Product> findById(String id) {
            return list.stream().filter(p -> p.getProductId().equalsIgnoreCase(id)).findFirst().map(Product::new);
        }

        @Override
        public boolean existsById(String id) {
            return list.stream().anyMatch(p -> p.getProductId().equalsIgnoreCase(id));
        }

        @Override
        public List<Product> findAll() { return new ArrayList<>(list); }

        @Override
        public List<Product> findActive() { return list.stream().filter(Product::isActive).toList(); }

        @Override
        public List<Product> findArchived() { return list.stream().filter(Product::isArchived).toList(); }

        @Override
        public int count() { return list.size(); }

        @Override
        public void reload() {}
    }

    private static class InMemoryPriceHistoryRepository implements PriceHistoryRepository {
        private final List<PriceHistory> list = new ArrayList<>();

        @Override
        public PriceHistory save(PriceHistory h) { list.add(h); return h; }

        @Override
        public List<PriceHistory> findAll() { return new ArrayList<>(list); }

        @Override
        public List<PriceHistory> findByProductId(String pid) { return List.of(); }

        @Override
        public List<PriceHistory> findByDateRange(LocalDateTime start, LocalDateTime end) { return List.of(); }

        @Override
        public int count() { return list.size(); }

        @Override
        public void reload() {}
    }

    public static void runAll() {
        System.out.println("Running Analytics Tests...");
        testAnalyticsCalculations();
        testEmptyCatalogAnalyticsSafety();
        testBelowCostAndLowStockAlerts();
        System.out.println("  -> All Analytics Tests Passed!");
    }

    private static void testAnalyticsCalculations() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();

        // 3 products with prices: 100, 200, 300
        // Cost: 60, 150, 200
        // Quantities: 10, 5, 2
        Product p1 = new Product("P1", "Item 1", "Cat", "", new BigDecimal("60.00"), new BigDecimal("100.00"), 10);
        Product p2 = new Product("P2", "Item 2", "Cat", "", new BigDecimal("150.00"), new BigDecimal("200.00"), 5);
        Product p3 = new Product("P3", "Item 3", "Cat", "", new BigDecimal("200.00"), new BigDecimal("300.00"), 2);

        prodRepo.save(p1);
        prodRepo.save(p2);
        prodRepo.save(p3);

        AnalyticsService analytics = new AnalyticsService(prodRepo, histRepo);
        AnalyticsSummary summary = analytics.computeSummary(5);

        Assert.assertEquals(3, summary.getActiveProductsCount(), "Active products count");
        Assert.assertEquals(17, summary.getTotalStockUnits(), "Total stock units (10 + 5 + 2 = 17)");
        Assert.assertEquals(new BigDecimal("100.00"), summary.getMinSellingPrice(), "Min selling price");
        Assert.assertEquals(new BigDecimal("300.00"), summary.getMaxSellingPrice(), "Max selling price");
        Assert.assertEquals(new BigDecimal("200.00"), summary.getAvgSellingPrice(), "Avg selling price ((100+200+300)/3 = 200)");
        Assert.assertEquals(new BigDecimal("200.00"), summary.getMedianSellingPrice(), "Median selling price");

        // Inventory valuations:
        // Total Cost: (60*10) + (150*5) + (200*2) = 600 + 750 + 400 = 1750.00
        // Total Retail: (100*10) + (200*5) + (300*2) = 1000 + 1000 + 600 = 2600.00
        // Gross Margin: 2600 - 1750 = 850.00
        Assert.assertEquals(new BigDecimal("1750.00"), summary.getTotalInventoryCostValue(), "Total cost valuation");
        Assert.assertEquals(new BigDecimal("2600.00"), summary.getTotalInventorySellingValue(), "Total retail valuation");
        Assert.assertEquals(new BigDecimal("850.00"), summary.getTotalGrossMarginAmount(), "Gross margin amount");

        // Gross Margin %: 850 / 2600 * 100 = 32.69%
        Assert.assertEquals(new BigDecimal("32.69"), summary.getOverallGrossMarginPercentage().orElseThrow(), "Gross margin %");
        // Markup %: 850 / 1750 * 100 = 48.57%
        Assert.assertEquals(new BigDecimal("48.57"), summary.getOverallMarkupPercentage().orElseThrow(), "Markup %");
    }

    private static void testEmptyCatalogAnalyticsSafety() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();

        AnalyticsService analytics = new AnalyticsService(prodRepo, histRepo);
        AnalyticsSummary summary = analytics.computeSummary(10);

        Assert.assertEquals(0, summary.getTotalProducts(), "Total products 0");
        Assert.assertEquals(0, summary.getActiveProductsCount(), "Active products 0");
        Assert.assertEquals(BigDecimal.ZERO, summary.getAvgSellingPrice(), "Avg price 0 on empty");
        Assert.assertEquals(BigDecimal.ZERO, summary.getMedianSellingPrice(), "Median price 0 on empty");
        Assert.assertTrue(summary.getOverallGrossMarginPercentage().isEmpty(), "Gross margin % should be empty on 0 retail value");
    }

    private static void testBelowCostAndLowStockAlerts() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();

        // Below cost item (selling 40 < cost 50)
        Product pBelowCost = new Product("P4", "Loss Leader", "Promo", "",
                new BigDecimal("50.00"), new BigDecimal("40.00"), 100);
        // Low stock item (qty 3 <= threshold 5)
        Product pLowStock = new Product("P5", "Rare Part", "Parts", "",
                new BigDecimal("100.00"), new BigDecimal("150.00"), 3);

        prodRepo.save(pBelowCost);
        prodRepo.save(pLowStock);

        AnalyticsService analytics = new AnalyticsService(prodRepo, histRepo);
        AnalyticsSummary summary = analytics.computeSummary(5);

        Assert.assertEquals(1, summary.getBelowCostCount(), "1 item below cost");
        Assert.assertEquals(1, summary.getLowStockCount(), "1 item low stock");
        Assert.assertEquals(1, analytics.getBelowCostProducts().size(), "Below cost product list matches");
        Assert.assertEquals(1, analytics.getLowStockProducts(5).size(), "Low stock product list matches");
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

