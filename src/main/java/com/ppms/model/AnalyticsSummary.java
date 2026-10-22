// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Value object summarizing pricing and catalog health analytics across the enterprise.
 */
public class AnalyticsSummary {

    private final int totalProducts;
    private final int activeProductsCount;
    private final int archivedProductsCount;
    private final int totalStockUnits;

    private final BigDecimal minSellingPrice;
    private final BigDecimal maxSellingPrice;
    private final BigDecimal avgSellingPrice;
    private final BigDecimal medianSellingPrice;

    private final BigDecimal totalInventoryCostValue;
    private final BigDecimal totalInventorySellingValue;
    private final BigDecimal totalGrossMarginAmount;

    private final int belowCostCount;
    private final int lowStockCount;
    private final int outOfStockCount;
    private final int totalHistoryRecords;

    public AnalyticsSummary(int totalProducts,
                            int activeProductsCount,
                            int archivedProductsCount,
                            int totalStockUnits,
                            BigDecimal minSellingPrice,
                            BigDecimal maxSellingPrice,
                            BigDecimal avgSellingPrice,
                            BigDecimal medianSellingPrice,
                            BigDecimal totalInventoryCostValue,
                            BigDecimal totalInventorySellingValue,
                            BigDecimal totalGrossMarginAmount,
                            int belowCostCount,
                            int lowStockCount,
                            int outOfStockCount,
                            int totalHistoryRecords) {
        this.totalProducts = totalProducts;
        this.activeProductsCount = activeProductsCount;
        this.archivedProductsCount = archivedProductsCount;
        this.totalStockUnits = totalStockUnits;
        this.minSellingPrice = minSellingPrice;
        this.maxSellingPrice = maxSellingPrice;
        this.avgSellingPrice = avgSellingPrice;
        this.medianSellingPrice = medianSellingPrice;
        this.totalInventoryCostValue = totalInventoryCostValue;
        this.totalInventorySellingValue = totalInventorySellingValue;
        this.totalGrossMarginAmount = totalGrossMarginAmount;
        this.belowCostCount = belowCostCount;
        this.lowStockCount = lowStockCount;
        this.outOfStockCount = outOfStockCount;
        this.totalHistoryRecords = totalHistoryRecords;
    }

    public int getTotalProducts() {
        return totalProducts;
    }

    public int getActiveProductsCount() {
        return activeProductsCount;
    }

    public int getArchivedProductsCount() {
        return archivedProductsCount;
    }

    public int getTotalStockUnits() {
        return totalStockUnits;
    }

    public BigDecimal getMinSellingPrice() {
        return minSellingPrice;
    }

    public BigDecimal getMaxSellingPrice() {
        return maxSellingPrice;
    }

    public BigDecimal getAvgSellingPrice() {
        return avgSellingPrice;
    }

    public BigDecimal getMedianSellingPrice() {
        return medianSellingPrice;
    }

    public BigDecimal getTotalInventoryCostValue() {
        return totalInventoryCostValue;
    }

    public BigDecimal getTotalInventorySellingValue() {
        return totalInventorySellingValue;
    }

    public BigDecimal getTotalGrossMarginAmount() {
        return totalGrossMarginAmount;
    }

    public Optional<BigDecimal> getOverallGrossMarginPercentage() {
        if (totalInventorySellingValue == null || totalInventorySellingValue.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal pct = totalGrossMarginAmount
                .divide(totalInventorySellingValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        return Optional.of(pct);
    }

    public Optional<BigDecimal> getOverallMarkupPercentage() {
        if (totalInventoryCostValue == null || totalInventoryCostValue.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal pct = totalGrossMarginAmount
                .divide(totalInventoryCostValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        return Optional.of(pct);
    }

    public int getBelowCostCount() {
        return belowCostCount;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public int getOutOfStockCount() {
        return outOfStockCount;
    }

    public int getTotalHistoryRecords() {
        return totalHistoryRecords;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

