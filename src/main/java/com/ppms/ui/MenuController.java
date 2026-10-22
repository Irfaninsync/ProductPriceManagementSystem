// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.ui;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.ImportReport;
import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.persistence.MetadataHandler;
import com.ppms.report.ReportGenerator;
import com.ppms.service.AnalyticsService;
import com.ppms.service.BackupService;
import com.ppms.service.ImportExportService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.CurrencyFormatter;
import com.ppms.util.DateTimeUtil;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller coordinating all terminal menus, interactive workflows, input routing, and presentations.
 */
public class MenuController {

    private final ConsoleUI ui;
    private final ProductService productService;
    private final PricingService pricingService;
    private final AnalyticsService analyticsService;
    private final ImportExportService importExportService;
    private final BackupService backupService;
    private final ReportGenerator reportGenerator;
    private final MetadataHandler metadataHandler;

    private int lowStockThreshold = 10;
    private int pageSize = 8;

    public MenuController(ConsoleUI ui,
                          ProductService productService,
                          PricingService pricingService,
                          AnalyticsService analyticsService,
                          ImportExportService importExportService,
                          BackupService backupService,
                          ReportGenerator reportGenerator,
                          MetadataHandler metadataHandler) {
        this.ui = ui;
        this.productService = productService;
        this.pricingService = pricingService;
        this.analyticsService = analyticsService;
        this.importExportService = importExportService;
        this.backupService = backupService;
        this.reportGenerator = reportGenerator;
        this.metadataHandler = metadataHandler;

        if (metadataHandler != null) {
            this.lowStockThreshold = metadataHandler.getInt("low_stock_threshold", 10);
            String savedCurrency = metadataHandler.get("currency_symbol", "\u20B9");
            CurrencyFormatter.setCurrencySymbol(savedCurrency);
        }
    }

    /**
     * Main application loop.
     */
    public void start() {
        boolean running = true;
        while (running) {
            try {
                renderDashboard();
                printMainMenu();
                int choice = ui.readInt("Select an option", 0, 6);

                switch (choice) {
                    case 1:
                        handleCatalogMenu();
                        break;
                    case 2:
                        handlePricingMenu();
                        break;
                    case 3:
                        handleHistoryMenu();
                        break;
                    case 4:
                        handleAnalyticsMenu();
                        break;
                    case 5:
                        handleDataManagementMenu();
                        break;
                    case 6:
                        handleSettingsAndHelpMenu();
                        break;
                    case 0:
                        if (ui.readConfirmation("Are you sure you want to exit PPMS?")) {
                            ui.printInfo("Flushing persistent files and shutting down cleanly. Goodbye!");
                            running = false;
                        }
                        break;
                }
            } catch (Exception e) {
                ui.printError("Unexpected error occurred: " + e.getMessage());
                ui.pause();
            }
        }
    }

    private void renderDashboard() {
        ui.renderBanner();
        AnalyticsSummary summary = analyticsService.computeSummary(lowStockThreshold);

        TableFormatter dashTable = new TableFormatter(List.of("METRIC", "VALUE", "METRIC", "VALUE"), ui.isBoxDrawingEnabled());
        dashTable.addRow(
                "Total Registered Products", String.valueOf(summary.getTotalProducts()),
                "Lowest Active Price", CurrencyFormatter.format(summary.getMinSellingPrice())
        );
        dashTable.addRow(
                "Active Products", String.valueOf(summary.getActiveProductsCount()),
                "Highest Active Price", CurrencyFormatter.format(summary.getMaxSellingPrice())
        );
        dashTable.addRow(
                "Archived Products", String.valueOf(summary.getArchivedProductsCount()),
                "Average Active Price", CurrencyFormatter.format(summary.getAvgSellingPrice())
        );
        dashTable.addRow(
                "Total Stock Units", String.valueOf(summary.getTotalStockUnits()),
                "Median Active Price", CurrencyFormatter.format(summary.getMedianSellingPrice())
        );
        dashTable.addRow(
                "Priced Below Cost [ALERT]", String.valueOf(summary.getBelowCostCount()),
                "Total Inventory Valuation", CurrencyFormatter.format(summary.getTotalInventorySellingValue())
        );
        dashTable.addRow(
                "Low Stock Items (<= " + lowStockThreshold + ")", String.valueOf(summary.getLowStockCount()),
                "Price Changes Logged", String.valueOf(summary.getTotalHistoryRecords())
        );

        ui.println(dashTable.render());

        // Show recent price changes banner if any
        List<PriceHistory> allHistory = pricingService.getAllHistory();
        if (!allHistory.isEmpty()) {
            int recentCount = Math.min(3, allHistory.size());
            List<PriceHistory> recent = allHistory.subList(allHistory.size() - recentCount, allHistory.size());
            ui.println("Recent Price Adjustments:");
            for (PriceHistory h : recent) {
                String pctStr = h.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A");
                ui.println(String.format("  * [%s] Product %s: %s -> %s (%s) | Reason: %s",
                        DateTimeUtil.formatDisplay(h.getChangedAt()),
                        h.getProductId(),
                        CurrencyFormatter.format(h.getOldPrice()),
                        CurrencyFormatter.format(h.getNewPrice()),
                        pctStr,
                        h.getReason()));
            }
            ui.println();
        }
    }

    private void printMainMenu() {
        ui.println("MAIN NAVIGATION:");
        ui.println("  [1] Product Catalog Management (Add, View, Edit, Search, Filter, Sort)");
        ui.println("  [2] Pricing Operations (Preview/Confirm Adjustments, Bulk Revaluation)");
        ui.println("  [3] Price History & Audit Trail (Product History, Date Filters, Extremes)");
        ui.println("  [4] Analytical Reports (Valuation, Margins, Category Breakdown, Alerts)");
        ui.println("  [5] Data Management (CSV Import/Export, Snapshot Backup & Restore)");
        ui.println("  [6] Application Settings & User Guide");
        ui.println("  [0] Exit System");
    }

    // ==========================================
    // MODULE B: PRODUCT CATALOG MANAGEMENT
    // ==========================================

    private void handleCatalogMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("Product Catalog Management");
            ui.println("  [1] View All Active Products");
            ui.println("  [2] View All Archived Products");
            ui.println("  [3] Add New Product");
            ui.println("  [4] Inspect Single Product Details");
            ui.println("  [5] Search Catalog (ID, Name, Category, Description)");
            ui.println("  [6] Multi-Criteria Filter");
            ui.println("  [7] Sort Catalog");
            ui.println("  [8] Edit Product Non-Price Details (Name, Category, Description)");
            ui.println("  [9] Update Product Cost Price");
            ui.println("  [10] Update Product Stock Quantity");
            ui.println("  [11] Archive Product");
            ui.println("  [12] Restore Archived Product");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select catalog action", 0, 12);
            switch (opt) {
                case 1:
                    renderProductsPaginated(productService.getActiveProducts(), "Active Product Catalog");
                    break;
                case 2:
                    renderProductsPaginated(productService.getArchivedProducts(), "Archived Products");
                    break;
                case 3:
                    doAddProduct();
                    break;
                case 4:
                    doInspectProduct();
                    break;
                case 5:
                    doSearchProducts();
                    break;
                case 6:
                    doFilterProducts();
                    break;
                case 7:
                    doSortProducts();
                    break;
                case 8:
                    doEditProductDetails();
                    break;
                case 9:
                    doUpdateCostPrice();
                    break;
                case 10:
                    doUpdateQuantity();
                    break;
                case 11:
                    doArchiveProduct();
                    break;
                case 12:
                    doRestoreProduct();
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    private void doAddProduct() {
        ui.renderSectionHeader("Add New Product");
        String idChoice = ui.readString("Enter custom Product ID or leave blank for automatic ID", true);
        String name = ui.readString("Product Name", false);
        String category = ui.readString("Category", false);
        String description = ui.readString("Description", true);
        BigDecimal costPrice = ui.readBigDecimal("Cost Price", BigDecimal.ZERO, null);
        BigDecimal sellingPrice = ui.readBigDecimal("Selling Price", BigDecimal.ZERO, null);
        int quantity = ui.readInt("Stock Quantity", 0, 1_000_000);

        try {
            Product created;
            if (idChoice.isEmpty()) {
                created = productService.addProduct(name, category, description, costPrice, sellingPrice, quantity);
            } else {
                created = productService.addProductWithId(idChoice, name, category, description, costPrice, sellingPrice, quantity);
            }
            ui.printSuccess("Product successfully registered with ID: " + created.getProductId());
            if (created.isBelowCost()) {
                ui.printWarning("Note: This product is priced below cost (Selling: "
                        + CurrencyFormatter.format(created.getSellingPrice())
                        + " < Cost: " + CurrencyFormatter.format(created.getCostPrice()) + ")");
            }
        } catch (Exception e) {
            ui.printError("Registration failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doInspectProduct() {
        String id = ui.readString("Enter Product ID to inspect", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("No product found with ID: " + id);
            ui.pause();
            return;
        }

        Product p = opt.get();
        ui.renderSectionHeader("Product Card: " + p.getProductId());
        ui.println("  Product ID:          " + p.getProductId());
        ui.println("  Name:                " + p.getName());
        ui.println("  Category:            " + p.getCategory());
        ui.println("  Description:         " + (p.getDescription().isEmpty() ? "(None)" : p.getDescription()));
        ui.println("  Status:              " + p.getStatus().getDisplayName());
        ui.println("  Cost Price:          " + CurrencyFormatter.format(p.getCostPrice()));
        ui.println("  Selling Price:       " + CurrencyFormatter.format(p.getSellingPrice()));
        ui.println("  Stock Quantity:      " + p.getQuantity() + " units");
        ui.println("  Stock Cost Value:    " + CurrencyFormatter.format(p.getInventoryCostValue()));
        ui.println("  Stock Selling Value: " + CurrencyFormatter.format(p.getInventorySellingValue()));
        ui.println("  Gross Margin/Unit:   " + CurrencyFormatter.format(p.getGrossMarginAmount()));
        ui.println("  Gross Margin %:      " + p.getGrossMarginPercentage().map(CurrencyFormatter::formatPercentage).orElse("N/A"));
        ui.println("  Markup %:            " + p.getMarkupPercentage().map(CurrencyFormatter::formatPercentage).orElse("N/A"));
        ui.println("  Priced Below Cost:   " + (p.isBelowCost() ? "YES [ALERT]" : "No"));
        ui.println("  Created Timestamp:   " + DateTimeUtil.formatDisplay(p.getCreatedAt()));
        ui.println("  Last Updated:        " + DateTimeUtil.formatDisplay(p.getUpdatedAt()));

        List<PriceHistory> history = pricingService.getHistoryForProduct(p.getProductId());
        ui.println("\n  Price Changes Recorded: " + history.size());
        if (!history.isEmpty()) {
            ui.println("  Latest Change: " + CurrencyFormatter.format(history.get(history.size() - 1).getOldPrice())
                    + " -> " + CurrencyFormatter.format(history.get(history.size() - 1).getNewPrice())
                    + " (" + history.get(history.size() - 1).getReason() + ")");
        }
        ui.pause();
    }

    private void doSearchProducts() {
        String query = ui.readString("Enter search keyword", false);
        List<Product> matches = productService.search(query);
        renderProductsPaginated(matches, "Search Results for '" + query + "'");
    }

    private void doFilterProducts() {
        ui.renderSectionHeader("Filter Products");
        String category = ui.readString("Category filter", true);
        ui.println("Status filter: [1] All, [2] Active Only, [3] Archived Only");
        int statusOpt = ui.readInt("Choice", 1, 3);
        ProductStatus status = (statusOpt == 2) ? ProductStatus.ACTIVE : (statusOpt == 3) ? ProductStatus.ARCHIVED : null;

        String minPriceStr = ui.readString("Minimum Selling Price", true);
        BigDecimal minPrice = minPriceStr.isEmpty() ? null : CurrencyFormatter.parse(minPriceStr);

        String maxPriceStr = ui.readString("Maximum Selling Price", true);
        BigDecimal maxPrice = maxPriceStr.isEmpty() ? null : CurrencyFormatter.parse(maxPriceStr);

        boolean belowCostOnly = ui.readConfirmation("Filter only products priced below cost?");
        boolean lowStockOnly = ui.readConfirmation("Filter only low-stock products?");

        try {
            List<Product> results = productService.filter(category, status, minPrice, maxPrice, null, null,
                    belowCostOnly, lowStockOnly, lowStockThreshold);
            renderProductsPaginated(results, "Filtered Product Results (" + results.size() + " matches)");
        } catch (Exception e) {
            ui.printError("Filter error: " + e.getMessage());
            ui.pause();
        }
    }

    private void doSortProducts() {
        ui.renderSectionHeader("Sort Products");
        ui.println("Sort by: [1] Name, [2] Product ID, [3] Category, [4] Selling Price, [5] Cost Price, [6] Quantity, [7] Last Updated");
        int sortFieldOpt = ui.readInt("Choice", 1, 7);
        String field = switch (sortFieldOpt) {
            case 2 -> "id";
            case 3 -> "category";
            case 4 -> "price";
            case 5 -> "cost";
            case 6 -> "quantity";
            case 7 -> "updated";
            default -> "name";
        };

        boolean ascending = ui.readConfirmation("Sort in Ascending order?");
        List<Product> sorted = productService.sort(productService.getAllProducts(), field, ascending);
        renderProductsPaginated(sorted, "Sorted Products (" + field.toUpperCase() + (ascending ? " ASC" : " DESC") + ")");
    }

    private void doEditProductDetails() {
        String id = ui.readString("Enter Product ID to edit", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }

        Product p = opt.get();
        ui.println("Current details for " + p.getProductId() + ":");
        ui.println("  Name:        " + p.getName());
        ui.println("  Category:    " + p.getCategory());
        ui.println("  Description: " + p.getDescription());

        String newName = ui.readString("New Name (leave blank to keep current)", true);
        String newCategory = ui.readString("New Category (leave blank to keep current)", true);
        String newDesc = ui.readString("New Description (leave blank to keep current)", true);

        try {
            productService.updateProductDetails(
                    id,
                    newName.isEmpty() ? p.getName() : newName,
                    newCategory.isEmpty() ? p.getCategory() : newCategory,
                    newDesc.isEmpty() ? p.getDescription() : newDesc
            );
            ui.printSuccess("Product details updated successfully.");
        } catch (Exception e) {
            ui.printError("Update failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doUpdateCostPrice() {
        String id = ui.readString("Enter Product ID to update cost price", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }
        Product p = opt.get();
        ui.println("Product: " + p.getName() + " | Current Cost Price: " + CurrencyFormatter.format(p.getCostPrice()));
        BigDecimal newCost = ui.readBigDecimal("Enter new Cost Price", BigDecimal.ZERO, null);

        if (ui.readConfirmation("Confirm cost price update to " + CurrencyFormatter.format(newCost) + "?")) {
            productService.updateCostPrice(id, newCost);
            ui.printSuccess("Cost price updated successfully.");
        } else {
            ui.printInfo("Update cancelled.");
        }
        ui.pause();
    }

    private void doUpdateQuantity() {
        String id = ui.readString("Enter Product ID to update inventory", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }
        Product p = opt.get();
        ui.println("Product: " + p.getName() + " | Current Stock: " + p.getQuantity() + " units");
        int newQty = ui.readInt("Enter new Quantity", 0, 1_000_000);

        if (ui.readConfirmation("Confirm inventory level update to " + newQty + " units?")) {
            productService.updateQuantity(id, newQty);
            ui.printSuccess("Stock quantity updated successfully.");
        } else {
            ui.printInfo("Update cancelled.");
        }
        ui.pause();
    }

    private void doArchiveProduct() {
        String id = ui.readString("Enter Product ID to archive", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }
        Product p = opt.get();
        ui.printWarning("Archiving product '" + p.getName() + "' (" + p.getProductId() + ").");
        ui.println("Historical price records will be preserved.");

        if (ui.readConfirmation("Are you sure you want to archive this product?")) {
            try {
                productService.archiveProduct(id);
                ui.printSuccess("Product successfully archived.");
            } catch (Exception e) {
                ui.printError("Archive failed: " + e.getMessage());
            }
        } else {
            ui.printInfo("Operation cancelled.");
        }
        ui.pause();
    }

    private void doRestoreProduct() {
        String id = ui.readString("Enter Product ID to restore", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }
        Product p = opt.get();
        if (p.isActive()) {
            ui.printWarning("Product is already active.");
            ui.pause();
            return;
        }

        if (ui.readConfirmation("Restore product '" + p.getName() + "' back to active catalog?")) {
            try {
                productService.restoreProduct(id);
                ui.printSuccess("Product restored successfully.");
            } catch (Exception e) {
                ui.printError("Restore failed: " + e.getMessage());
            }
        } else {
            ui.printInfo("Operation cancelled.");
        }
        ui.pause();
    }

    // ==========================================
    // MODULE C: PRICING OPERATIONS
    // ==========================================

    private void handlePricingMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("Pricing Management & Adjustments");
            ui.println("  [1] Adjust Single Product Selling Price (Preview & Confirmation)");
            ui.println("  [2] Bulk Percentage Price Adjustment (Category or Catalog Wide)");
            ui.println("  [3] View Current Product Price & Historical Audit Summary");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select pricing action", 0, 3);
            switch (opt) {
                case 1:
                    doSinglePriceAdjustment();
                    break;
                case 2:
                    doBulkPriceAdjustment();
                    break;
                case 3:
                    doInspectProduct();
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    private void doSinglePriceAdjustment() {
        ui.renderSectionHeader("Single Selling Price Adjustment");
        String id = ui.readString("Enter Product ID", false);
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            ui.printError("Product not found: " + id);
            ui.pause();
            return;
        }

        Product product = opt.get();
        BigDecimal currentPrice = product.getSellingPrice();
        ui.println("Product: " + product.getName() + " (" + product.getProductId() + ")");
        ui.println("Current Selling Price: " + CurrencyFormatter.format(currentPrice));
        ui.println("Cost Price:            " + CurrencyFormatter.format(product.getCostPrice()));

        BigDecimal proposedPrice = ui.readBigDecimal("Enter Proposed New Selling Price", BigDecimal.ZERO, null);
        String reason = ui.readString("Enter reason for price modification", false);

        PriceProposal proposal = pricingService.createProposal(id, proposedPrice, reason);

        // Display comprehensive proposal preview
        ui.renderSectionHeader("PRICE CHANGE PREVIEW");
        ui.println("  Product:           " + proposal.getProduct().getName());
        ui.println("  Current Price:     " + CurrencyFormatter.format(proposal.getCurrentPrice()));
        ui.println("  Proposed Price:    " + CurrencyFormatter.format(proposal.getProposedPrice()));
        ui.println("  Price Difference:  " + CurrencyFormatter.format(proposal.getDifference()));
        ui.println("  Percentage Change: " + proposal.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A"));
        ui.println("  Audit Reason:      " + proposal.getReason());

        if (proposedPrice.compareTo(product.getCostPrice()) < 0) {
            ui.printWarning("Warning: Proposed price is lower than the product's cost price ("
                    + CurrencyFormatter.format(product.getCostPrice()) + ")");
        }

        if (!proposal.isValid()) {
            ui.printError("Proposal is INVALID:\n" + proposal.getValidationResult().getFormattedErrors());
            ui.pause();
            return;
        }

        if (ui.readConfirmation("Do you confirm and approve this selling price update?")) {
            try {
                PriceHistory saved = pricingService.applyPriceChange(proposal);
                ui.printSuccess("Price successfully modified and logged under Audit ID: " + saved.getHistoryId());
            } catch (Exception e) {
                ui.printError("Failed to apply price change: " + e.getMessage());
            }
        } else {
            ui.printInfo("Price change operation cancelled. No data modified.");
        }
        ui.pause();
    }

    private void doBulkPriceAdjustment() {
        ui.renderSectionHeader("Bulk Pricing Adjustment Workflow");
        ui.println("Target Scope: [1] By Category, [2] All Active Products");
        int scopeOpt = ui.readInt("Select target scope", 1, 2);

        List<Product> targets;
        if (scopeOpt == 1) {
            String category = ui.readString("Enter category name", false);
            targets = productService.filter(category, ProductStatus.ACTIVE, null, null, null, null, null, null, lowStockThreshold);
        } else {
            targets = productService.getActiveProducts();
        }

        if (targets.isEmpty()) {
            ui.printWarning("No active products match the selected scope.");
            ui.pause();
            return;
        }

        ui.println("Selected " + targets.size() + " products for bulk adjustment.");
        ui.println("Enter percentage change (e.g., enter 10 for +10% increase, or -5 for -5% discount):");
        BigDecimal pct = ui.readBigDecimal("Percentage Adjustment", new BigDecimal("-99.00"), new BigDecimal("1000.00"));
        String reason = ui.readString("Enter business reason for bulk adjustment", false);

        List<PriceProposal> proposals = pricingService.previewBulkAdjustment(targets, pct, reason);

        // Render preview table
        TableFormatter previewTable = new TableFormatter(
                List.of("ID", "PRODUCT", "OLD PRICE", "NEW PRICE", "DIFF", "% CHG", "VALID?"),
                ui.isBoxDrawingEnabled()
        );

        int validCount = 0;
        for (PriceProposal p : proposals) {
            if (p.isValid()) validCount++;
            previewTable.addRow(
                    p.getProduct().getProductId(),
                    p.getProduct().getName().length() > 20 ? p.getProduct().getName().substring(0, 17) + "..." : p.getProduct().getName(),
                    CurrencyFormatter.format(p.getCurrentPrice()),
                    CurrencyFormatter.format(p.getProposedPrice()),
                    CurrencyFormatter.format(p.getDifference()),
                    p.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A"),
                    p.isValid() ? "YES" : "INVALID"
            );
        }

        ui.println(previewTable.render());
        ui.println(String.format("Bulk Adjustment Summary: %d valid proposals out of %d candidates.", validCount, proposals.size()));

        if (validCount == 0) {
            ui.printError("No valid proposals to execute.");
            ui.pause();
            return;
        }

        if (ui.readConfirmation("Proceed and execute bulk price update for " + validCount + " products?")) {
            List<PriceHistory> executed = pricingService.applyBulkAdjustments(proposals);
            ui.printSuccess("Bulk adjustment successfully completed! " + executed.size() + " price adjustments recorded.");
        } else {
            ui.printInfo("Bulk price adjustment cancelled without any data changes.");
        }
        ui.pause();
    }

    // ==========================================
    // MODULE D: PRICE HISTORY & AUDIT TRAIL
    // ==========================================

    private void handleHistoryMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("Price History & Audit Trail");
            ui.println("  [1] View Complete Audit Log (Chronological)");
            ui.println("  [2] View Price History for Specific Product");
            ui.println("  [3] Filter Audit Log by Date Range");
            ui.println("  [4] Filter Audit Log by Reason Keyword");
            ui.println("  [5] Identify Largest Historical Price Increase");
            ui.println("  [6] Identify Largest Historical Price Decrease");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select history action", 0, 6);
            switch (opt) {
                case 1:
                    renderHistoryPaginated(pricingService.getAllHistory(), "Complete Price Audit History");
                    break;
                case 2:
                    String pid = ui.readString("Enter Product ID", false);
                    renderHistoryPaginated(pricingService.getHistoryForProduct(pid), "Price History for Product " + pid);
                    break;
                case 3:
                    doFilterHistoryByDate();
                    break;
                case 4:
                    String keyword = ui.readString("Enter reason keyword", false);
                    renderHistoryPaginated(pricingService.getHistoryByReason(keyword), "History Filtered by Reason '" + keyword + "'");
                    break;
                case 5:
                    doLargestIncrease();
                    break;
                case 6:
                    doLargestDecrease();
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    private void doFilterHistoryByDate() {
        ui.renderSectionHeader("Filter Price History by Date");
        String startStr = ui.readString("Start Date (yyyy-MM-dd, or blank for all past)", true);
        String endStr = ui.readString("End Date (yyyy-MM-dd, or blank for all future)", true);

        try {
            LocalDate startDate = startStr.isEmpty() ? null : DateTimeUtil.parseDateOnly(startStr);
            LocalDate endDate = endStr.isEmpty() ? null : DateTimeUtil.parseDateOnly(endStr);

            LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDt = endDate != null ? endDate.atTime(23, 59, 59) : null;

            List<PriceHistory> filtered = pricingService.getHistoryByDateRange(startDt, endDt);
            renderHistoryPaginated(filtered, "Price History (" + (startStr.isEmpty() ? "Beginning" : startStr) + " to " + (endStr.isEmpty() ? "Now" : endStr) + ")");
        } catch (Exception e) {
            ui.printError("Invalid date format: " + e.getMessage());
            ui.pause();
        }
    }

    private void doLargestIncrease() {
        Optional<PriceHistory> opt = pricingService.getLargestPriceIncrease();
        if (opt.isEmpty()) {
            ui.printInfo("No price increases recorded in the audit trail.");
        } else {
            PriceHistory h = opt.get();
            ui.renderSectionHeader("LARGEST HISTORICAL PRICE INCREASE");
            ui.println("  Audit ID:          " + h.getHistoryId());
            ui.println("  Product ID:        " + h.getProductId());
            ui.println("  Previous Price:    " + CurrencyFormatter.format(h.getOldPrice()));
            ui.println("  New Price:         " + CurrencyFormatter.format(h.getNewPrice()));
            ui.println("  Absolute Increase: +" + CurrencyFormatter.format(h.getPriceDifference()));
            ui.println("  Percentage Change: " + h.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A"));
            ui.println("  Date & Time:       " + DateTimeUtil.formatDisplay(h.getChangedAt()));
            ui.println("  Audit Reason:      " + h.getReason());
        }
        ui.pause();
    }

    private void doLargestDecrease() {
        Optional<PriceHistory> opt = pricingService.getLargestPriceDecrease();
        if (opt.isEmpty()) {
            ui.printInfo("No price decreases recorded in the audit trail.");
        } else {
            PriceHistory h = opt.get();
            ui.renderSectionHeader("LARGEST HISTORICAL PRICE DECREASE");
            ui.println("  Audit ID:          " + h.getHistoryId());
            ui.println("  Product ID:        " + h.getProductId());
            ui.println("  Previous Price:    " + CurrencyFormatter.format(h.getOldPrice()));
            ui.println("  New Price:         " + CurrencyFormatter.format(h.getNewPrice()));
            ui.println("  Absolute Decrease: " + CurrencyFormatter.format(h.getPriceDifference()));
            ui.println("  Percentage Change: " + h.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A"));
            ui.println("  Date & Time:       " + DateTimeUtil.formatDisplay(h.getChangedAt()));
            ui.println("  Audit Reason:      " + h.getReason());
        }
        ui.pause();
    }

    // ==========================================
    // MODULE E & G: ANALYTICS & REPORTS
    // ==========================================

    private void handleAnalyticsMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("Analytical Reports & Inventory Risk Alerts");
            ui.println("  [1] Executive Pricing & Valuation Summary");
            ui.println("  [2] Category Performance & Gross Margin Breakdown");
            ui.println("  [3] Products Priced Below Cost Report (Margin Deficit Alert)");
            ui.println("  [4] Low-Stock & Depleted Inventory Warnings");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select report", 0, 4);
            switch (opt) {
                case 1:
                    ui.println(reportGenerator.generateExecutiveSummary(lowStockThreshold));
                    ui.pause();
                    break;
                case 2:
                    ui.println(reportGenerator.generateCategoryBreakdown());
                    ui.pause();
                    break;
                case 3:
                    List<Product> belowCost = analyticsService.getBelowCostProducts();
                    renderProductsPaginated(belowCost, "Products Priced Below Cost (Loss-Leaders / Potential Mispricing)");
                    break;
                case 4:
                    List<Product> lowStock = analyticsService.getLowStockProducts(lowStockThreshold);
                    renderProductsPaginated(lowStock, "Low Stock Warning (Threshold <= " + lowStockThreshold + ")");
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    // ==========================================
    // MODULE H & J: DATA MANAGEMENT & BACKUPS
    // ==========================================

    private void handleDataManagementMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("Data Management, CSV Import/Export & Backups");
            ui.println("  [1] Export Active Products to CSV");
            ui.println("  [2] Export Complete Price Audit Trail to CSV");
            ui.println("  [3] Import Products from CSV (Validation Preview & Commit)");
            ui.println("  [4] Create Instant Data Snapshot Backup");
            ui.println("  [5] List All Available Backups");
            ui.println("  [6] Restore Catalog from Backup Snapshot");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select data action", 0, 6);
            switch (opt) {
                case 1:
                    doExportProducts();
                    break;
                case 2:
                    doExportHistory();
                    break;
                case 3:
                    doImportProducts();
                    break;
                case 4:
                    doCreateBackup();
                    break;
                case 5:
                    doListBackups();
                    break;
                case 6:
                    doRestoreBackup();
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    private void doExportProducts() {
        String dest = ui.readString("Enter destination CSV file path (e.g. data/exported_products.csv)", false);
        try {
            Path path = Paths.get(dest);
            List<Product> list = productService.getAllProducts();
            importExportService.exportProducts(list, path);
            ui.printSuccess("Exported " + list.size() + " products successfully to: " + path.toAbsolutePath());
        } catch (Exception e) {
            ui.printError("Export failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doExportHistory() {
        String dest = ui.readString("Enter destination CSV file path (e.g. data/exported_history.csv)", false);
        try {
            Path path = Paths.get(dest);
            List<PriceHistory> list = pricingService.getAllHistory();
            importExportService.exportPriceHistory(list, path);
            ui.printSuccess("Exported " + list.size() + " audit records successfully to: " + path.toAbsolutePath());
        } catch (Exception e) {
            ui.printError("Export failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doImportProducts() {
        ui.renderSectionHeader("Import Products from CSV");
        String src = ui.readString("Enter source CSV file path", false);
        Path path = Paths.get(src);

        try {
            ImportReport report = importExportService.previewImport(path);

            ui.renderSectionHeader("IMPORT VALIDATION PREVIEW");
            ui.println("  Total File Rows Read:   " + report.getTotalRowsProcessed());
            ui.println("  Valid Records Accepted: " + report.getAcceptedCount());
            ui.println("  Rejected Records:       " + report.getRejectedCount());

            if (report.hasRejections()) {
                ui.printWarning("Rejections Summary (First 10 shown):");
                int shown = 0;
                for (ImportReport.RowRejection rej : report.getRejections()) {
                    ui.println("  - Line " + rej.getLineNumber() + ": " + rej.getReason());
                    if (++shown >= 10) {
                        ui.println("  ... and " + (report.getRejections().size() - 10) + " more rejected rows.");
                        break;
                    }
                }
            }

            if (report.getAcceptedCount() == 0) {
                ui.printError("No valid records found in the import file. Nothing to import.");
                ui.pause();
                return;
            }

            if (ui.readConfirmation("Proceed to import " + report.getAcceptedCount() + " valid products into catalog?")) {
                int imported = importExportService.applyImport(report);
                ui.printSuccess("Successfully imported " + imported + " products!");
            } else {
                ui.printInfo("Import cancelled. No records added.");
            }
        } catch (Exception e) {
            ui.printError("Import failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doCreateBackup() {
        String reason = ui.readString("Enter reason or note for this backup snapshot", true);
        try {
            Path backupDir = backupService.createBackup(reason.isEmpty() ? "Manual Backup" : reason);
            ui.printSuccess("Backup successfully created at: " + backupDir.toAbsolutePath());
        } catch (Exception e) {
            ui.printError("Backup creation failed: " + e.getMessage());
        }
        ui.pause();
    }

    private void doListBackups() {
        List<BackupService.BackupInfo> backups = backupService.listBackups();
        if (backups.isEmpty()) {
            ui.printInfo("No backups found in data/backups.");
            ui.pause();
            return;
        }

        TableFormatter bkpTable = new TableFormatter(
                List.of("BACKUP DIRECTORY", "TIMESTAMP", "SIZE (BYTES)"),
                ui.isBoxDrawingEnabled()
        );
        for (BackupService.BackupInfo b : backups) {
            bkpTable.addRow(
                    b.getBackupId(),
                    DateTimeUtil.formatDisplay(b.getTimestamp()),
                    String.valueOf(b.getTotalSizeBytes())
            );
        }
        ui.println(bkpTable.render());
        ui.pause();
    }

    private void doRestoreBackup() {
        List<BackupService.BackupInfo> backups = backupService.listBackups();
        if (backups.isEmpty()) {
            ui.printError("No backups available to restore.");
            ui.pause();
            return;
        }

        ui.println("Available Backups:");
        for (int i = 0; i < backups.size(); i++) {
            BackupService.BackupInfo b = backups.get(i);
            ui.println(String.format("  [%d] %s (%s, %d bytes)",
                    i + 1, b.getBackupId(), DateTimeUtil.formatDisplay(b.getTimestamp()), b.getTotalSizeBytes()));
        }

        int choice = ui.readInt("Select backup to restore", 1, backups.size());
        BackupService.BackupInfo selected = backups.get(choice - 1);

        ui.printWarning("CAUTION: Restoring will overwrite current live data with snapshot from "
                + DateTimeUtil.formatDisplay(selected.getTimestamp()) + ".");
        ui.println("A safety backup of your current live data will be automatically created first.");

        if (ui.readConfirmation("Confirm restoring this backup?")) {
            try {
                backupService.restoreBackup(selected.getPath());
                ui.printSuccess("Backup restored successfully and repositories reloaded.");
            } catch (Exception e) {
                ui.printError("Restoration failed: " + e.getMessage());
            }
        } else {
            ui.printInfo("Restoration aborted.");
        }
        ui.pause();
    }

    // ==========================================
    // MODULE K: SETTINGS & HELP
    // ==========================================

    private void handleSettingsAndHelpMenu() {
        boolean back = false;
        while (!back) {
            ui.renderSectionHeader("System Settings & User Manual");
            ui.println("  [1] Set Low-Stock Warning Threshold (Current: " + lowStockThreshold + ")");
            ui.println("  [2] Configure Currency Display Symbol (Current: " + CurrencyFormatter.getCurrencySymbol() + ")");
            ui.println("  [3] Toggle ANSI Terminal Colors (Current: " + (ui.isAnsiEnabled() ? "ENABLED" : "DISABLED") + ")");
            ui.println("  [4] Toggle Clean Box Borders (Current: " + (ui.isBoxDrawingEnabled() ? "ENABLED" : "PLAIN TEXT") + ")");
            ui.println("  [5] Display Comprehensive User Guide");
            ui.println("  [0] Return to Main Menu");

            int opt = ui.readInt("Select option", 0, 5);
            switch (opt) {
                case 1:
                    int newThresh = ui.readInt("Enter new low-stock threshold", 1, 10000);
                    lowStockThreshold = newThresh;
                    if (metadataHandler != null) {
                        metadataHandler.setInt("low_stock_threshold", newThresh);
                    }
                    ui.printSuccess("Low-stock threshold updated to " + newThresh);
                    ui.pause();
                    break;
                case 2:
                    String sym = ui.readString("Enter currency symbol (e.g. Rs., $, EUR, \u20B9)", false);
                    CurrencyFormatter.setCurrencySymbol(sym);
                    if (metadataHandler != null) {
                        metadataHandler.set("currency_symbol", sym);
                    }
                    ui.printSuccess("Currency symbol set to " + sym);
                    ui.pause();
                    break;
                case 3:
                    ui.setAnsiEnabled(!ui.isAnsiEnabled());
                    ui.printSuccess("ANSI terminal colors " + (ui.isAnsiEnabled() ? "enabled" : "disabled") + ".");
                    ui.pause();
                    break;
                case 4:
                    ui.setBoxDrawingEnabled(!ui.isBoxDrawingEnabled());
                    ui.printSuccess("Box drawing characters " + (ui.isBoxDrawingEnabled() ? "enabled" : "switched to plain ASCII") + ".");
                    ui.pause();
                    break;
                case 5:
                    renderUserGuide();
                    break;
                case 0:
                    back = true;
                    break;
            }
        }
    }

    private void renderUserGuide() {
        ui.renderSectionHeader("PPMS USER GUIDE & SYSTEM ARCHITECTURE");
        ui.println("SYSTEM ARCHITECTURE:");
        ui.println("  * Layered Architecture: UI -> Service -> Repository -> Persistence -> CSV Files.");
        ui.println("  * Zero external dependencies: purely standard Java SE 17.");
        ui.println("  * Financial Precision: Standardized BigDecimal with RoundingMode.HALF_UP.");
        ui.println("  * Safe File Writes: Atomic staging files (.tmp), automatic backup (.bak), atomic rename.");
        ui.println();
        ui.println("CORE FORMULAS:");
        ui.println("  * Gross Margin Amount  = Selling Price - Cost Price");
        ui.println("  * Gross Margin %       = ((Selling Price - Cost Price) / Selling Price) * 100");
        ui.println("  * Markup %             = ((Selling Price - Cost Price) / Cost Price) * 100");
        ui.println("  * Price Difference     = New Price - Old Price");
        ui.println("  * Percentage Change    = ((New Price - Old Price) / Old Price) * 100");
        ui.println();
        ui.println("AUDIT & HISTORY:");
        ui.println("  * Every selling price update generates an immutable PriceHistory audit log record.");
        ui.println("  * History is preserved even if a product is subsequently archived.");
        ui.pause();
    }

    // ==========================================
    // PAGINATION HELPERS
    // ==========================================

    private void renderProductsPaginated(List<Product> products, String title) {
        ui.renderSectionHeader(title);
        ui.renderPaginated(products, pageSize, pageItems -> {
            TableFormatter table = new TableFormatter(
                    List.of("ID", "NAME", "CATEGORY", "COST", "SELLING", "QTY", "MARGIN %", "STATUS"),
                    ui.isBoxDrawingEnabled()
            );
            for (Product p : pageItems) {
                String marginStr = p.getGrossMarginPercentage().map(CurrencyFormatter::formatPercentage).orElse("N/A");
                table.addRow(
                        p.getProductId(),
                        p.getName().length() > 22 ? p.getName().substring(0, 19) + "..." : p.getName(),
                        p.getCategory().length() > 15 ? p.getCategory().substring(0, 12) + "..." : p.getCategory(),
                        CurrencyFormatter.format(p.getCostPrice()),
                        CurrencyFormatter.format(p.getSellingPrice()),
                        String.valueOf(p.getQuantity()),
                        marginStr,
                        p.getStatus().name()
                );
            }
            return table.render();
        });
    }

    private void renderHistoryPaginated(List<PriceHistory> historyList, String title) {
        ui.renderSectionHeader(title);
        ui.renderPaginated(historyList, pageSize, pageItems -> {
            TableFormatter table = new TableFormatter(
                    List.of("AUDIT ID", "PRD ID", "OLD PRICE", "NEW PRICE", "DIFF", "% CHG", "TIMESTAMP", "REASON"),
                    ui.isBoxDrawingEnabled()
            );
            for (PriceHistory h : pageItems) {
                table.addRow(
                        h.getHistoryId(),
                        h.getProductId(),
                        CurrencyFormatter.format(h.getOldPrice()),
                        CurrencyFormatter.format(h.getNewPrice()),
                        CurrencyFormatter.format(h.getPriceDifference()),
                        h.getPercentageChange().map(CurrencyFormatter::formatPercentage).orElse("N/A"),
                        DateTimeUtil.formatDisplay(h.getChangedAt()),
                        h.getReason().length() > 22 ? h.getReason().substring(0, 19) + "..." : h.getReason()
                );
            }
            return table.render();
        });
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

