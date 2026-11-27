// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Aggregated category metrics derived from actual stored products.
 */
public class CategorySummary {

    private final String categoryId;
    private final String name;
    private final String description;
    private final int totalProducts;
    private final int activeProducts;
    private final int archivedProducts;
    private final int totalUnits;
    private final BigDecimal totalCostValue;
    private final BigDecimal totalSellingValue;
    private final BigDecimal totalMarginAmount;
    private final BigDecimal avgSellingPrice;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CategorySummary(String categoryId,
                           String name,
                           String description,
                           int totalProducts,
                           int activeProducts,
                           int archivedProducts,
                           int totalUnits,
                           BigDecimal totalCostValue,
                           BigDecimal totalSellingValue,
                           BigDecimal totalMarginAmount,
                           BigDecimal avgSellingPrice,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.archivedProducts = archivedProducts;
        this.totalUnits = totalUnits;
        this.totalCostValue = totalCostValue;
        this.totalSellingValue = totalSellingValue;
        this.totalMarginAmount = totalMarginAmount;
        this.avgSellingPrice = avgSellingPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTotalProducts() { return totalProducts; }
    public int getActiveProducts() { return activeProducts; }
    public int getArchivedProducts() { return archivedProducts; }
    public int getTotalUnits() { return totalUnits; }
    public BigDecimal getTotalCostValue() { return totalCostValue; }
    public BigDecimal getTotalSellingValue() { return totalSellingValue; }
    public BigDecimal getTotalMarginAmount() { return totalMarginAmount; }
    public BigDecimal getAvgSellingPrice() { return avgSellingPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Optional<BigDecimal> getMarginPercentage() {
        if (totalSellingValue == null || totalSellingValue.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        return Optional.of(totalMarginAmount
                .divide(totalSellingValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
