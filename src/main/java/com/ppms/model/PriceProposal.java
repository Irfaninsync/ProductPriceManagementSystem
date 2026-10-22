package com.ppms.model;

import com.ppms.validation.ValidationResult;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Value object encapsulating a proposed selling price modification before execution.
 */
public class PriceProposal {

    private final Product product;
    private final BigDecimal currentPrice;
    private final BigDecimal proposedPrice;
    private final String reason;
    private final ValidationResult validationResult;

    public PriceProposal(Product product,
                         BigDecimal proposedPrice,
                         String reason,
                         ValidationResult validationResult) {
        this.product = product;
        this.currentPrice = product != null ? product.getSellingPrice() : BigDecimal.ZERO;
        this.proposedPrice = proposedPrice;
        this.reason = reason;
        this.validationResult = validationResult;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getProposedPrice() {
        return proposedPrice;
    }

    public String getReason() {
        return reason;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public boolean isValid() {
        return validationResult != null && validationResult.isValid();
    }

    public BigDecimal getDifference() {
        if (proposedPrice == null || currentPrice == null) {
            return BigDecimal.ZERO;
        }
        return proposedPrice.subtract(currentPrice);
    }

    public Optional<BigDecimal> getPercentageChange() {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0 || proposedPrice == null) {
            return Optional.empty();
        }
        BigDecimal diff = proposedPrice.subtract(currentPrice);
        BigDecimal pct = diff.divide(currentPrice, 4, java.math.RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                             .setScale(2, java.math.RoundingMode.HALF_UP);
        return Optional.of(pct);
    }
}
