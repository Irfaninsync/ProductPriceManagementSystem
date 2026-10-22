package com.ppms.validation;

import com.ppms.model.Product;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Validates domain constraints on Product instances and pricing updates.
 */
public class ProductValidator {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{3,30}$");
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_CATEGORY_LENGTH = 60;
    private static final int MIN_CATEGORY_LENGTH = 2;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /**
     * Validates an entire Product instance.
     */
    public static ValidationResult validate(Product product) {
        ValidationResult result = new ValidationResult();
        if (product == null) {
            result.addError("Product entity cannot be null.");
            return result;
        }

        validateProductId(product.getProductId(), result);
        validateName(product.getName(), result);
        validateCategory(product.getCategory(), result);
        validateDescription(product.getDescription(), result);
        validateCostPrice(product.getCostPrice(), result);
        validateSellingPrice(product.getSellingPrice(), result);
        validateQuantity(product.getQuantity(), result);

        if (product.getStatus() == null) {
            result.addError("Product status is required.");
        }

        return result;
    }

    public static void validateProductId(String productId, ValidationResult result) {
        if (productId == null || productId.trim().isEmpty()) {
            result.addError("Product ID is required and cannot be empty.");
            return;
        }
        String clean = productId.trim();
        if (!PRODUCT_ID_PATTERN.matcher(clean).matches()) {
            result.addError("Product ID must be 3-30 characters (letters, numbers, hyphens, and underscores only).");
        }
    }

    public static void validateName(String name, ValidationResult result) {
        if (name == null || name.trim().isEmpty()) {
            result.addError("Product name is required and cannot be empty or whitespace.");
            return;
        }
        String clean = name.trim();
        if (clean.length() < MIN_NAME_LENGTH || clean.length() > MAX_NAME_LENGTH) {
            result.addError("Product name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.");
        }
    }

    public static void validateCategory(String category, ValidationResult result) {
        if (category == null || category.trim().isEmpty()) {
            result.addError("Category is required and cannot be empty or whitespace.");
            return;
        }
        String clean = category.trim();
        if (clean.length() < MIN_CATEGORY_LENGTH || clean.length() > MAX_CATEGORY_LENGTH) {
            result.addError("Category must be between " + MIN_CATEGORY_LENGTH + " and " + MAX_CATEGORY_LENGTH + " characters.");
        }
    }

    public static void validateDescription(String description, ValidationResult result) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            result.addError("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
    }

    public static void validateCostPrice(BigDecimal costPrice, ValidationResult result) {
        if (costPrice == null) {
            result.addError("Cost price is required.");
            return;
        }
        if (costPrice.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Cost price cannot be negative (provided: " + costPrice + ").");
        }
    }

    public static void validateSellingPrice(BigDecimal sellingPrice, ValidationResult result) {
        if (sellingPrice == null) {
            result.addError("Selling price is required.");
            return;
        }
        if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Selling price cannot be negative (provided: " + sellingPrice + ").");
        }
    }

    public static void validateQuantity(int quantity, ValidationResult result) {
        if (quantity < 0) {
            result.addError("Quantity in stock cannot be negative (provided: " + quantity + ").");
        }
    }

    /**
     * Validates a proposed price change workflow.
     */
    public static ValidationResult validatePriceChange(BigDecimal currentPrice, BigDecimal newPrice, String reason) {
        ValidationResult result = new ValidationResult();
        if (newPrice == null) {
            result.addError("Proposed new price is required.");
            return result;
        }
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Proposed selling price cannot be negative.");
        }
        if (currentPrice != null && currentPrice.compareTo(newPrice) == 0) {
            result.addError("New price is identical to current price (" + currentPrice + "). No price change recorded.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            result.addError("A valid reason for the price adjustment is mandatory for audit logging.");
        }
        return result;
    }
}
