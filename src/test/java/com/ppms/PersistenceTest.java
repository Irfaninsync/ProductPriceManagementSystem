package com.ppms;

import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.persistence.CsvParser;
import com.ppms.persistence.CsvPriceHistoryRepository;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.CsvWriter;
import com.ppms.service.BackupService;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class PersistenceTest {

    public static void runAll() throws IOException {
        System.out.println("Running Persistence Tests...");
        testRfc4180CsvParserWithCommasAndQuotes();
        testSaveAndReloadProductsAcrossRestarts();
        testSaveAndReloadPriceHistory();
        testMalformedRowResilience();
        testBackupAndRestore();
        System.out.println("  -> All Persistence Tests Passed!");
    }

    private static void testRfc4180CsvParserWithCommasAndQuotes() throws IOException {
        // Test embedded comma, embedded newline, and escaped double quotes
        String rawCsv = "productId,name,description\r\n"
                + "PRD-1,\"Gaming Mouse, RGB Edition\",\"Features: \"\"Ultra Precision\"\"\nNext line\"\r\n"
                + "PRD-2,Standard Mouse,Plain description\r\n";

        List<List<String>> records = CsvParser.parse(new StringReader(rawCsv));
        Assert.assertEquals(3, records.size(), "Should have header + 2 rows");

        List<String> row1 = records.get(1);
        Assert.assertEquals("PRD-1", row1.get(0), "ID match");
        Assert.assertEquals("Gaming Mouse, RGB Edition", row1.get(1), "Embedded comma match");
        Assert.assertEquals("Features: \"Ultra Precision\"\nNext line", row1.get(2), "Escaped quotes and newline match");

        // Verify CSV Writer correctly re-escapes
        String formatted = CsvWriter.formatRow(row1);
        Assert.assertTrue(formatted.contains("\"Gaming Mouse, RGB Edition\""), "Should re-quote field with comma");
        Assert.assertTrue(formatted.contains("\"\"Ultra Precision\"\""), "Should escape internal quotes as double quotes");
    }

    private static void testSaveAndReloadProductsAcrossRestarts() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_test_persist_");
        Path productsFile = tempDir.resolve("products.csv");

        IdGenerator gen1 = new IdGenerator("PRD-", 1000);
        CsvProductRepository repo1 = new CsvProductRepository(productsFile, gen1);

        Product p1 = new Product("PRD-1001", "Laptop Stand", "Accessories", "Aluminium",
                new BigDecimal("1200.00"), new BigDecimal("1800.00"), 15);
        Product p2 = new Product("PRD-1002", "HDMI Cable", "Cables", "Gold plated",
                new BigDecimal("200.00"), new BigDecimal("450.00"), 30);

        repo1.save(p1);
        repo1.save(p2);
        Assert.assertEquals(2, repo1.count(), "Repository 1 should have 2 products");

        // Simulate application restart: instantiate brand new repository reading same CSV file
        IdGenerator gen2 = new IdGenerator("PRD-", 1000);
        CsvProductRepository repo2 = new CsvProductRepository(productsFile, gen2);

        Assert.assertEquals(2, repo2.count(), "Repository 2 should reload 2 products from disk");
        Product loadedP1 = repo2.findById("PRD-1001").orElseThrow();
        Assert.assertEquals("Laptop Stand", loadedP1.getName(), "Product name preserved");
        Assert.assertEquals(new BigDecimal("1800.00"), loadedP1.getSellingPrice(), "Selling price preserved");
        Assert.assertEquals(15, loadedP1.getQuantity(), "Quantity preserved");
        Assert.assertEquals(ProductStatus.ACTIVE, loadedP1.getStatus(), "Status preserved");

        // Verify high-watermark ID generator observed existing IDs
        Assert.assertEquals("PRD-1003", gen2.nextId(), "Next generated ID should be PRD-1003");
    }

    private static void testSaveAndReloadPriceHistory() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_test_hist_");
        Path historyFile = tempDir.resolve("price_history.csv");

        IdGenerator gen1 = new IdGenerator("HST-", 1000);
        CsvPriceHistoryRepository repo1 = new CsvPriceHistoryRepository(historyFile, gen1);

        PriceHistory h1 = new PriceHistory("HST-1001", "PRD-1001", new BigDecimal("1500.00"),
                new BigDecimal("1800.00"), LocalDateTime.now(), "Supplier adjustment");
        repo1.save(h1);

        // Restart
        IdGenerator gen2 = new IdGenerator("HST-", 1000);
        CsvPriceHistoryRepository repo2 = new CsvPriceHistoryRepository(historyFile, gen2);

        Assert.assertEquals(1, repo2.count(), "Should reload 1 history record");
        PriceHistory loaded = repo2.findAll().get(0);
        Assert.assertEquals("HST-1001", loaded.getHistoryId(), "History ID match");
        Assert.assertEquals("PRD-1001", loaded.getProductId(), "Product ID match");
        Assert.assertEquals(new BigDecimal("1500.00"), loaded.getOldPrice(), "Old price match");
        Assert.assertEquals(new BigDecimal("1800.00"), loaded.getNewPrice(), "New price match");
        Assert.assertEquals("Supplier adjustment", loaded.getReason(), "Reason match");
    }

    private static void testMalformedRowResilience() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_test_malformed_");
        Path productsFile = tempDir.resolve("products.csv");

        // Write file with 1 valid row, 1 row missing price, 1 row invalid quantity
        String content = "productId,name,category,description,costPrice,sellingPrice,quantity,status,createdAt,updatedAt\r\n"
                + "PRD-1001,Valid Product,Tech,Good,100.00,150.00,10,ACTIVE,2026-01-01T10:00:00,2026-01-01T10:00:00\r\n"
                + "PRD-1002,Broken Product,Tech,Bad,NOT_A_NUMBER,150.00,10,ACTIVE,2026-01-01T10:00:00,2026-01-01T10:00:00\r\n"
                + "PRD-1003,Broken Product 2,Tech,Bad,100.00,150.00,NOT_AN_INT,ACTIVE,2026-01-01T10:00:00,2026-01-01T10:00:00\r\n";
        Files.writeString(productsFile, content);

        CsvProductRepository repo = new CsvProductRepository(productsFile, new IdGenerator("PRD-", 1000));
        // Only the valid row should be loaded into cache, no crash
        Assert.assertEquals(1, repo.count(), "Only valid record PRD-1001 should be loaded");
        Assert.assertTrue(repo.findById("PRD-1001").isPresent(), "PRD-1001 must be present");
    }

    private static void testBackupAndRestore() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_test_backup_");
        Path productsFile = tempDir.resolve("products.csv");
        Path historyFile = tempDir.resolve("price_history.csv");

        CsvProductRepository repo = new CsvProductRepository(productsFile, new IdGenerator("PRD-", 1000));
        CsvPriceHistoryRepository histRepo = new CsvPriceHistoryRepository(historyFile, new IdGenerator("HST-", 1000));

        repo.save(new Product("PRD-1001", "Item A", "Cat", "", new BigDecimal("10.00"), new BigDecimal("20.00"), 5));

        BackupService backupService = new BackupService(tempDir, repo, histRepo);
        Path backupDir = backupService.createBackup("Initial State");
        Assert.assertTrue(Files.exists(backupDir.resolve("products.csv")), "Backup must contain products.csv");

        // Mutate live product
        repo.save(new Product("PRD-1001", "Item A Mutated", "Cat", "", new BigDecimal("10.00"), new BigDecimal("99.00"), 5));
        Assert.assertEquals(new BigDecimal("99.00"), repo.findById("PRD-1001").orElseThrow().getSellingPrice(), "Live price mutated");

        // Restore from backup
        backupService.restoreBackup(backupDir);

        // Verify restoration reloaded original price
        Product restored = repo.findById("PRD-1001").orElseThrow();
        Assert.assertEquals(new BigDecimal("20.00"), restored.getSellingPrice(), "Restored product price must be 20.00");
    }
}
