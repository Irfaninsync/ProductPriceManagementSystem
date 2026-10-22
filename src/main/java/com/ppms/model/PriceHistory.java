package com.ppms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable audit record capturing every approved selling price adjustment.
 */
public class PriceHistory implements Comparable<PriceHistory> {

    private final String historyId;
    private final String productId;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final LocalDateTime changedAt;
    private final String reason;

    public PriceHistory(String historyId,
                        String productId,
                        BigDecimal oldPrice,
                        BigDecimal newPrice,
                        LocalDateTime changedAt,
                        String reason) {
        this.historyId = Objects.requireNonNull(historyId, "History ID cannot be null").trim();
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null").trim();
        this.oldPrice = Product.normalizePrice(oldPrice);
        this.newPrice = Product.normalizePrice(newPrice);
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.reason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Standard Price Adjustment";
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getProductId() {
        return productId;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Absolute or signed price difference: newPrice - oldPrice.
     */
    public BigDecimal getPriceDifference() {
        return newPrice.subtract(oldPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Percentage price change: ((newPrice - oldPrice) / oldPrice) * 100.
     * If oldPrice is zero, percentage change is mathematically undefined; returns empty Optional.
     */
    public Optional<BigDecimal> getPercentageChange() {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal diff = newPrice.subtract(oldPrice);
        BigDecimal pct = diff.divide(oldPrice, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                             .setScale(2, RoundingMode.HALF_UP);
        return Optional.of(pct);
    }

    public boolean isPriceIncrease() {
        return newPrice.compareTo(oldPrice) > 0;
    }

    public boolean isPriceDecrease() {
        return newPrice.compareTo(oldPrice) < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceHistory that = (PriceHistory) o;
        return historyId.equalsIgnoreCase(that.historyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyId.toUpperCase());
    }

    @Override
    public int compareTo(PriceHistory other) {
        if (other == null) return 1;
        // Primary sort: chronological (changedAt)
        int dateCmp = this.changedAt.compareTo(other.changedAt);
        if (dateCmp != 0) {
            return dateCmp;
        }
        return this.historyId.compareToIgnoreCase(other.historyId);
    }

    @Override
    public String toString() {
        return "PriceHistory{" +
                "historyId='" + historyId + '\'' +
                ", productId='" + productId + '\'' +
                ", oldPrice=" + oldPrice +
                ", newPrice=" + newPrice +
                ", changedAt=" + changedAt +
                ", reason='" + reason + '\'' +
                '}';
    }
}
