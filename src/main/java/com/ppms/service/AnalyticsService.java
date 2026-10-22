package com.ppms.service;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
}
