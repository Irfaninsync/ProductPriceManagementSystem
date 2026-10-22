package com.ppms.model;

/**
 * Represents the lifecycle status of a product within the catalog.
 */
public enum ProductStatus {
    ACTIVE("Active"),
    ARCHIVED("Archived");

    private final String displayName;

    ProductStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string value into ProductStatus case-insensitively.
     *
     * @param value raw text value
     * @return matching ProductStatus
     * @throws IllegalArgumentException if unknown
     */
    public static ProductStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product status cannot be null or empty");
        }
        String clean = value.trim().toUpperCase();
        for (ProductStatus status : values()) {
            if (status.name().equals(clean) || status.displayName.equalsIgnoreCase(clean)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown product status: " + value + ". Permitted values: ACTIVE, ARCHIVED");
    }
}
