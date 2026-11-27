// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service calculating live financial, operational, and inventory health metrics.
 */
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public AnalyticsService(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.priceHistoryRepository = Objects.requireNonNull(priceHistoryRepository, "PriceHistoryRepository cannot be null");
    }

    /**
     * Computes the complete real-time analytics summary from current catalog state.
     */
    public AnalyticsSummary computeSummary(int lowStockThreshold) {
        List<Product> allProducts = productRepository.findAll();
        List<Product> activeProducts = productRepository.findActive();
        List<Product> archivedProducts = productRepository.findArchived();

        int totalProducts = allProducts.size();
        int activeCount = activeProducts.size();
        int archivedCount = archivedProducts.size();
        int totalHistoryRecords = priceHistoryRepository.count();

        if (activeProducts.isEmpty()) {
            return new AnalyticsSummary(
                    totalProducts, 0, archivedCount, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0, 0, 0, totalHistoryRecords
            );
        }

        int totalStockUnits = 0;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        BigDecimal sumPrice = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalSellingValue = BigDecimal.ZERO;

        int belowCostCount = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        List<BigDecimal> sortedPrices = new ArrayList<>();

        for (Product p : activeProducts) {
            BigDecimal price = p.getSellingPrice();
            sortedPrices.add(price);

            if (minPrice == null || price.compareTo(minPrice) < 0) {
                minPrice = price;
            }
            if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                maxPrice = price;
            }
            sumPrice = sumPrice.add(price);

            totalStockUnits += p.getQuantity();
            totalCostValue = totalCostValue.add(p.getInventoryCostValue());
            totalSellingValue = totalSellingValue.add(p.getInventorySellingValue());

            if (p.isBelowCost()) {
                belowCostCount++;
            }
            if (p.isLowStock(lowStockThreshold)) {
                lowStockCount++;
            }
            if (p.isOutOfStock()) {
                outOfStockCount++;
            }
        }

        Collections.sort(sortedPrices);
        BigDecimal avgPrice = sumPrice.divide(BigDecimal.valueOf(activeCount), 2, RoundingMode.HALF_UP);
        BigDecimal medianPrice = computeMedian(sortedPrices);
        BigDecimal grossMarginAmount = totalSellingValue.subtract(totalCostValue);

        return new AnalyticsSummary(
                totalProducts,
                activeCount,
                archivedCount,
                totalStockUnits,
                minPrice != null ? minPrice : BigDecimal.ZERO,
                maxPrice != null ? maxPrice : BigDecimal.ZERO,
                avgPrice,
                medianPrice,
                totalCostValue,
                totalSellingValue,
                grossMarginAmount,
                belowCostCount,
                lowStockCount,
                outOfStockCount,
                totalHistoryRecords
        );
    }

    private BigDecimal computeMedian(List<BigDecimal> sortedList) {
        if (sortedList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int size = sortedList.size();
        if (size % 2 == 1) {
            return sortedList.get(size / 2);
        } else {
            BigDecimal mid1 = sortedList.get((size / 2) - 1);
            BigDecimal mid2 = sortedList.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }

    public List<Product> getBelowCostProducts() {
        return productRepository.findActive().stream()
                .filter(Product::isBelowCost)
                .collect(Collectors.toList());
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findActive().stream()
                .filter(p -> p.isLowStock(threshold))
                .collect(Collectors.toList());
    }

    public List<Product> getOutOfStockProducts() {
        return productRepository.findActive().stream()
                .filter(Product::isOutOfStock)
                .collect(Collectors.toList());
    }

    public List<Product> getRecentlyAddedProducts(int limit) {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed()
                        .thenComparing(Product::getProductId))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    public List<PriceHistory> getRecentPriceChanges(int limit) {
        return priceHistoryRepository.findAll().stream()
                .sorted(Comparator.comparing(PriceHistory::getChangedAt).reversed()
                        .thenComparing(PriceHistory::getHistoryId, Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /**
     * Computes real price-band counts across active products for price overview visualization.
     */
    public List<Map<String, Object>> getActivePriceBands() {
        List<Product> active = productRepository.findActive();
        if (active.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal min = active.stream().map(Product::getSellingPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = active.stream().map(Product::getSellingPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        if (min.compareTo(max) == 0) {
            Map<String, Object> single = new LinkedHashMap<>();
            single.put("label", min.toPlainString());
            single.put("minPrice", min);
            single.put("maxPrice", max);
            single.put("count", active.size());
            return List.of(single);
        }

        int bucketCount = Math.min(5, Math.max(2, active.size()));
        BigDecimal range = max.subtract(min);
        BigDecimal step = range.divide(BigDecimal.valueOf(bucketCount), 2, RoundingMode.CEILING);
        if (step.compareTo(BigDecimal.ZERO) <= 0) {
            step = BigDecimal.ONE;
        }

        List<Map<String, Object>> buckets = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            BigDecimal low = min.add(step.multiply(BigDecimal.valueOf(i)));
            BigDecimal high = (i == bucketCount - 1) ? max : min.add(step.multiply(BigDecimal.valueOf(i + 1)));
            boolean isLast = (i == bucketCount - 1);
            long count = active.stream().filter(p -> {
                BigDecimal sp = p.getSellingPrice();
                boolean geLow = sp.compareTo(low) >= 0;
                boolean ltOrLeHigh = isLast ? sp.compareTo(high) <= 0 : sp.compareTo(high) < 0;
                return geLow && ltOrLeHigh;
            }).count();
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("minPrice", low);
            bucket.put("maxPrice", high);
            bucket.put("count", (int) count);
            buckets.add(bucket);
        }
        return Collections.unmodifiableList(buckets);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

