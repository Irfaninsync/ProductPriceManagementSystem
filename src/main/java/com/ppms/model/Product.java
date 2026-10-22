// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain entity representing a product within the Product Price Management System.
 * Enforces field encapsulation, immutable identifiers, and precise financial calculations.
 */
public class Product implements Comparable<Product> {

    private final String productId;
    private String name;
    private String category;
    private String description;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private int quantity;
    private ProductStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Full constructor for existing or reconstituted products (e.g. from CSV storage).
     */
    public Product(String productId,
                   String name,
                   String category,
                   String description,
                   BigDecimal costPrice,
                   BigDecimal sellingPrice,
                   int quantity,
                   ProductStatus status,
                   LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null").trim();
        this.name = Objects.requireNonNull(name, "Name cannot be null").trim();
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.costPrice = normalizePrice(costPrice);
        this.sellingPrice = normalizePrice(sellingPrice);
        this.quantity = quantity;
        this.status = status != null ? status : ProductStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /**
     * Convenient constructor for newly created products.
     */
    public Product(String productId,
                   String name,
                   String category,
                   String description,
                   BigDecimal costPrice,
                   BigDecimal sellingPrice,
                   int quantity) {
        this(productId, name, category, description, costPrice, sellingPrice, quantity,
             ProductStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Copy constructor for safe defensive duplication and editing previews.
     */
    public Product(Product other) {
        Objects.requireNonNull(other, "Source product cannot be null");
        this.productId = other.productId;
        this.name = other.name;
        this.category = other.category;
        this.description = other.description;
        this.costPrice = other.costPrice;
        this.sellingPrice = other.sellingPrice;
        this.quantity = other.quantity;
        this.status = other.status;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    // --- Getters ---

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE;
    }

    public boolean isArchived() {
        return this.status == ProductStatus.ARCHIVED;
    }

    // --- Controlled Mutators with Timestamp Updates ---

    public void updateDetails(String name, String category, String description) {
        this.name = Objects.requireNonNull(name, "Name cannot be null").trim();
        this.category = Objects.requireNonNull(category, "Category cannot be null").trim();
        this.description = description != null ? description.trim() : "";
        this.updatedAt = LocalDateTime.now();
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = normalizePrice(costPrice);
        this.updatedAt = LocalDateTime.now();
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = normalizePrice(sellingPrice);
        this.updatedAt = LocalDateTime.now();
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(ProductStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    // --- Financial & Inventory Calculations ---

    /**
     * Calculates Gross Margin Amount = Selling Price - Cost Price.
     */
    public BigDecimal getGrossMarginAmount() {
        return sellingPrice.subtract(costPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Gross Margin Percentage = ((Selling Price - Cost Price) / Selling Price) * 100.
     * Returns empty Optional if selling price is zero to avoid division by zero.
     */
    public Optional<BigDecimal> getGrossMarginPercentage() {
        if (sellingPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal margin = sellingPrice.subtract(costPrice);
        BigDecimal percentage = margin.divide(sellingPrice, 4, RoundingMode.HALF_UP)
                                      .multiply(BigDecimal.valueOf(100))
                                      .setScale(2, RoundingMode.HALF_UP);
        return Optional.of(percentage);
    }

    /**
     * Calculates Markup Percentage = ((Selling Price - Cost Price) / Cost Price) * 100.
     * Returns empty Optional if cost price is zero.
     */
    public Optional<BigDecimal> getMarkupPercentage() {
        if (costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal margin = sellingPrice.subtract(costPrice);
        BigDecimal percentage = margin.divide(costPrice, 4, RoundingMode.HALF_UP)
                                      .multiply(BigDecimal.valueOf(100))
                                      .setScale(2, RoundingMode.HALF_UP);
        return Optional.of(percentage);
    }

    /**
     * Checks if the product is priced below cost (sellingPrice < costPrice).
     */
    public boolean isBelowCost() {
        return sellingPrice.compareTo(costPrice) < 0;
    }

    /**
     * Checks if stock level is non-zero but at or below the given threshold.
     */
    public boolean isLowStock(int threshold) {
        return quantity > 0 && quantity <= threshold;
    }

    /**
     * Checks if the product is completely out of stock.
     */
    public boolean isOutOfStock() {
        return quantity == 0;
    }

    /**
     * Inventory Cost Value = Cost Price * Quantity.
     */
    public BigDecimal getInventoryCostValue() {
        return costPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Inventory Selling Value = Selling Price * Quantity.
     */
    public BigDecimal getInventorySellingValue() {
        return sellingPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    // --- Identity & Equality ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return productId.equalsIgnoreCase(product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId.toUpperCase());
    }

    @Override
    public int compareTo(Product other) {
        if (other == null) return 1;
        return this.productId.compareToIgnoreCase(other.productId);
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", costPrice=" + costPrice +
                ", sellingPrice=" + sellingPrice +
                ", quantity=" + quantity +
                ", status=" + status +
                '}';
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

