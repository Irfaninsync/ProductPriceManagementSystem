// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.model.AnalyticsSummary;
import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.persistence.CsvPriceHistoryRepository;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.MetadataHandler;
import com.ppms.service.AnalyticsService;
import com.ppms.service.ImportExportService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EndToEndIntegrationTest {

    public static void runAll() throws IOException {
        System.out.println("Running End-to-End Integration Workflow Test...");

        Path sandboxDir = Files.createTempDirectory("ppms_e2e_sandbox_");
        Path productsFile = sandboxDir.resolve("products.csv");
        Path historyFile = sandboxDir.resolve("price_history.csv");
        Path metadataFile = sandboxDir.resolve("metadata.csv");

        // --- SESSION 1: Startup & Operations ---
        IdGenerator prodGen1 = new IdGenerator("PRD-", 1000);
        IdGenerator histGen1 = new IdGenerator("HST-", 1000);

        CsvProductRepository prodRepo1 = new CsvProductRepository(productsFile, prodGen1);
        CsvPriceHistoryRepository histRepo1 = new CsvPriceHistoryRepository(historyFile, histGen1);
        MetadataHandler meta1 = new MetadataHandler(metadataFile);

        ProductService prodService1 = new ProductService(prodRepo1, prodGen1);
        PricingService pricingService1 = new PricingService(prodRepo1, histRepo1, histGen1);
        AnalyticsService analyticsService1 = new AnalyticsService(prodRepo1, histRepo1);
        ImportExportService ioService1 = new ImportExportService(prodRepo1, histRepo1, prodGen1);

        // Step 2: Add a product
        Product created = prodService1.addProductWithId("PRD-9999", "Pro Stream Webcam 4K", "Video",
                "Ultra HD 60fps streaming camera", new BigDecimal("8500.00"), new BigDecimal("12999.00"), 25);
        Assert.assertNotNull(created, "Product created");
        Assert.assertEquals("PRD-9999", created.getProductId(), "Product ID matches PRD-9999");

        // Step 3: Search for it
        List<Product> searchResults = prodService1.search("webcam");
        Assert.assertEquals(1, searchResults.size(), "Search should find Webcam");
        Assert.assertEquals("PRD-9999", searchResults.get(0).getProductId(), "Found product ID");

        // Step 4 & 5: Update its selling price and record a reason
        PriceProposal proposal = pricingService1.createProposal("PRD-9999", new BigDecimal("14499.00"), "Component cost increase and currency fluctuation");
        Assert.assertTrue(proposal.isValid(), "Proposal is valid");
        PriceHistory historyRecord = pricingService1.applyPriceChange(proposal);
        Assert.assertNotNull(historyRecord, "History record created");
        Assert.assertEquals("HST-1001", historyRecord.getHistoryId(), "History ID HST-1001");

        // Step 6: Inspect history
        List<PriceHistory> productHistory = pricingService1.getHistoryForProduct("PRD-9999");
        Assert.assertEquals(1, productHistory.size(), "Product history count 1");
        Assert.assertEquals(new BigDecimal("12999.00"), productHistory.get(0).getOldPrice(), "Old price 12999.00");
        Assert.assertEquals(new BigDecimal("14499.00"), productHistory.get(0).getNewPrice(), "New price 14499.00");

        // Step 7: View Analytics
        AnalyticsSummary summary1 = analyticsService1.computeSummary(10);
        Assert.assertEquals(1, summary1.getActiveProductsCount(), "1 active product");
        Assert.assertEquals(new BigDecimal("14499.00"), summary1.getAvgSellingPrice(), "Avg price matches current price");
        Assert.assertEquals(1, summary1.getTotalHistoryRecords(), "1 history record logged");

        // Step 8: Export product list
        Path exportedCsv = sandboxDir.resolve("exported_test.csv");
        ioService1.exportProducts(prodService1.getAllProducts(), exportedCsv);
        Assert.assertTrue(Files.exists(exportedCsv), "Exported CSV must exist");

        // Step 9: Simulate Exit (instances discarded)

        // Step 10: Restart (fresh initialization reading existing sandbox files)
        IdGenerator prodGen2 = new IdGenerator("PRD-", 1000);
        IdGenerator histGen2 = new IdGenerator("HST-", 1000);

        CsvProductRepository prodRepo2 = new CsvProductRepository(productsFile, prodGen2);
        CsvPriceHistoryRepository histRepo2 = new CsvPriceHistoryRepository(historyFile, histGen2);

        // Step 11: Verify product and history remain intact
        Assert.assertEquals(1, prodRepo2.count(), "Reloaded products count must be 1");
        Product reloadedProduct = prodRepo2.findById("PRD-9999").orElseThrow();
        Assert.assertEquals("Pro Stream Webcam 4K", reloadedProduct.getName(), "Name preserved");
        Assert.assertEquals(new BigDecimal("14499.00"), reloadedProduct.getSellingPrice(), "Updated price 14499.00 preserved");
        Assert.assertEquals(25, reloadedProduct.getQuantity(), "Quantity preserved");
        Assert.assertEquals(ProductStatus.ACTIVE, reloadedProduct.getStatus(), "Status preserved");

        Assert.assertEquals(1, histRepo2.count(), "Reloaded price history count must be 1");
        PriceHistory reloadedHistory = histRepo2.findByProductId("PRD-9999").get(0);
        Assert.assertEquals("HST-1001", reloadedHistory.getHistoryId(), "History ID preserved");
        Assert.assertEquals(new BigDecimal("12999.00"), reloadedHistory.getOldPrice(), "Old price preserved");
        Assert.assertEquals(new BigDecimal("14499.00"), reloadedHistory.getNewPrice(), "New price preserved");
        Assert.assertEquals("Component cost increase and currency fluctuation", reloadedHistory.getReason(), "Reason preserved");

        System.out.println("  -> End-to-End Workflow Verification Successfully Passed!");
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

