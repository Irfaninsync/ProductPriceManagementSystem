package com.ppms.report;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.service.AnalyticsService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.CurrencyFormatter;
import com.ppms.util.DateTimeUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates formatted operational and analytical reports for terminal display and file export.
 */
public class ReportGenerator {

    private final ProductService productService;
    private final PricingService pricingService;
    private final AnalyticsService analyticsService;

    public ReportGenerator(ProductService productService,
                           PricingService pricingService,
                           AnalyticsService analyticsService) {
        this.productService = productService;
        this.pricingService = pricingService;
        this.analyticsService = analyticsService;
    }

    /**
     * Generates an Executive Pricing & Valuation Summary.
     */
    public String generateExecutiveSummary(int lowStockThreshold) {
        AnalyticsSummary summary = analyticsService.computeSummary(lowStockThreshold);
        StringBuilder sb = new StringBuilder();
        sb.append("======================================================================\n");
        sb.append("         EXECUTIVE PRICING & INVENTORY VALUATION REPORT               \n");
        sb.append("         Generated At: ").append(DateTimeUtil.formatDisplay(LocalDateTime.now())).append("\n");
        sb.append("======================================================================\n\n");

        sb.append("--- CATALOG OVERVIEW ---\n");
        sb.append(String.format("  Total Registered Products:  %d\n", summary.getTotalProducts()));
        sb.append(String.format("  Active Products:            %d\n", summary.getActiveProductsCount()));
        sb.append(String.format("  Archived Products:          %d\n", summary.getArchivedProductsCount()));
        sb.append(String.format("  Total Units In Stock:       %d\n\n", summary.getTotalStockUnits()));

        sb.append("--- PRICING METRICS (ACTIVE PRODUCTS) ---\n");
        sb.append(String.format("  Lowest Selling Price:       %s\n", CurrencyFormatter.format(summary.getMinSellingPrice())));
        sb.append(String.format("  Highest Selling Price:      %s\n", CurrencyFormatter.format(summary.getMaxSellingPrice())));
        sb.append(String.format("  Average Selling Price:      %s\n", CurrencyFormatter.format(summary.getAvgSellingPrice())));
        sb.append(String.format("  Median Selling Price:       %s\n\n", CurrencyFormatter.format(summary.getMedianSellingPrice())));

        sb.append("--- FINANCIAL VALUATION & MARGINS ---\n");
        sb.append(String.format("  Total Inventory Cost:       %s\n", CurrencyFormatter.format(summary.getTotalInventoryCostValue())));
        sb.append(String.format("  Total Inventory Retail:     %s\n", CurrencyFormatter.format(summary.getTotalInventorySellingValue())));
        sb.append(String.format("  Gross Margin (Potential):   %s\n", CurrencyFormatter.format(summary.getTotalGrossMarginAmount())));
        sb.append(String.format("  Overall Gross Margin %%:     %s\n",
                summary.getOverallGrossMarginPercentage().map(CurrencyFormatter::formatPercentage).orElse("N/A")));
        sb.append(String.format("  Overall Markup %%:           %s\n\n",
                summary.getOverallMarkupPercentage().map(CurrencyFormatter::formatPercentage).orElse("N/A")));

        sb.append("--- RISK & INVENTORY ALERTS ---\n");
        sb.append(String.format("  Products Priced Below Cost: %d\n", summary.getBelowCostCount()));
        sb.append(String.format("  Low Stock Items (<= %d):    %d\n", lowStockThreshold, summary.getLowStockCount()));
        sb.append(String.format("  Out of Stock Items:         %d\n", summary.getOutOfStockCount()));
        sb.append(String.format("  Total Price Adjustments:    %d\n", summary.getTotalHistoryRecords()));
        sb.append("======================================================================\n");

        return sb.toString();
    }

    /**
     * Generates a Category Performance and Margin Breakdown.
     */
    public String generateCategoryBreakdown() {
        List<Product> activeProducts = productService.getActiveProducts();
        Map<String, List<Product>> byCategory = activeProducts.stream()
                .collect(Collectors.groupingBy(Product::getCategory, LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("                         CATEGORY PERFORMANCE BREAKDOWN                                 \n");
        sb.append("========================================================================================\n");
        sb.append(String.format("%-20s %-8s %-12s %-15s %-15s %-10s\n",
                "Category", "Items", "Units", "Total Cost", "Total Value", "Avg Margin"));
        sb.append("----------------------------------------------------------------------------------------\n");

        for (Map.Entry<String, List<Product>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            List<Product> list = entry.getValue();
            int count = list.size();
            int units = list.stream().mapToInt(Product::getQuantity).sum();
            BigDecimal costSum = list.stream().map(Product::getInventoryCostValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal retailSum = list.stream().map(Product::getInventorySellingValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal marginSum = retailSum.subtract(costSum);

            String marginPct = "0.00%";
            if (retailSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = marginSum.divide(retailSum, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                marginPct = pct + "%";
            }

            sb.append(String.format("%-20s %-8d %-12d %-15s %-15s %-10s\n",
                    cat.length() > 19 ? cat.substring(0, 16) + "..." : cat,
                    count,
                    units,
                    CurrencyFormatter.format(costSum),
                    CurrencyFormatter.format(retailSum),
                    marginPct));
        }

        sb.append("========================================================================================\n");
        return sb.toString();
    }
}
