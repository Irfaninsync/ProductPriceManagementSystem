package com.ppms.model;

/**
 * Standard business reasons for product pricing modifications.
 */
public enum PriceChangeReason {
    SUPPLIER_PRICE_INCREASE("Supplier Price Increase"),
    SUPPLIER_PRICE_DECREASE("Supplier Price Decrease"),
    PROMOTIONAL_DISCOUNT("Promotional Discount"),
    SEASONAL_ADJUSTMENT("Seasonal Adjustment"),
    INVENTORY_CLEARANCE("Inventory Clearance"),
    COST_CORRECTION("Cost Correction"),
    COMPETITOR_PRICE_MATCH("Competitor Price Matching"),
    BULK_REVALUATION("Bulk Pricing Revaluation"),
    INFLATION_ADJUSTMENT("Inflationary Adjustment"),
    MANUAL_OVERRIDE("Manual Manager Override");

    private final String description;

    PriceChangeReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
