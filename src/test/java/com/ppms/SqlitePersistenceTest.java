// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.db.SqliteActivityRepository;
import com.ppms.db.SqliteBackupService;
import com.ppms.db.SqliteCategoryRepository;
import com.ppms.db.SqliteDatabaseManager;
import com.ppms.db.SqlitePriceHistoryRepository;
import com.ppms.db.SqliteProductRepository;
import com.ppms.db.SqliteSettingsRepository;
import com.ppms.model.ActivityEntry;
import com.ppms.model.BackupSnapshot;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.IdGenerator;
import com.ppms.util.JsonCodec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SqlitePersistenceTest {

    public static void runAll() throws IOException {
        System.out.println("Running SQLite Persistence & Backup Tests...");
        testSqliteCrudRestartAndAtomicPricing();
        testSqliteBackupAndSafeRestore();
        testCorruptedBackupRejectedWithoutOverwritingLiveDb();
        testJsonCodecRoundTrip();
        System.out.println("  -> All SQLite Persistence & Backup Tests Passed!");
    }

    private static void testSqliteCrudRestartAndAtomicPricing() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_sqlite_crud_");
        Path dbFile = tempDir.resolve("ppms.db");

        // Session 1
        try (SqliteDatabaseManager db1 = new SqliteDatabaseManager(dbFile)) {
            IdGenerator prodGen1 = new IdGenerator("PRD-", 1000);
            IdGenerator histGen1 = new IdGenerator("HST-", 1000);
            IdGenerator catGen1 = new IdGenerator("CAT-", 1000);

            SqliteCategoryRepository catRepo1 = new SqliteCategoryRepository(db1, catGen1);
            SqliteProductRepository prodRepo1 = new SqliteProductRepository(db1, catRepo1, prodGen1);
            SqlitePriceHistoryRepository histRepo1 = new SqlitePriceHistoryRepository(db1, histGen1);
            SqliteActivityRepository actRepo1 = new SqliteActivityRepository(db1);
            SqliteSettingsRepository setRepo1 = new SqliteSettingsRepository(db1);
            db1.syncIdGenerators(prodGen1, histGen1, catGen1);

            ProductService prodSvc1 = new ProductService(prodRepo1, prodGen1, db1, actRepo1);
            PricingService priceSvc1 = new PricingService(prodRepo1, histRepo1, histGen1, db1, actRepo1);

            setRepo1.set("currency_symbol", "₹");
            setRepo1.set("low_stock_threshold", "7");

            Product p = prodSvc1.addProduct("NVMe Enclosure", "Storage", "USB 3.2 Gen 2 \"Aluminum\"",
                    new BigDecimal("1199.50"), new BigDecimal("1899.99"), 18);
            Assert.assertEquals("PRD-1001", p.getProductId(), "Auto ID PRD-1001");

            PriceHistory hist = priceSvc1.changePrice(p.getProductId(), new BigDecimal("1749.00"), "Seasonal Adjustment");
            Assert.assertEquals("HST-1001", hist.getHistoryId(), "Auto history ID HST-1001");

            prodSvc1.archiveProduct(p.getProductId());
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                priceSvc1.changePrice(p.getProductId(), new BigDecimal("1999.00"), "Should fail while archived");
            }, "Archived product must not allow selling price update");
            prodSvc1.restoreProduct(p.getProductId());

            List<ActivityEntry> activities = actRepo1.findRecent(10);
            Assert.assertTrue(activities.size() >= 4, "Activity log captured operations");
        }

        // Session 2 (restart simulation)
        try (SqliteDatabaseManager db2 = new SqliteDatabaseManager(dbFile)) {
            IdGenerator prodGen2 = new IdGenerator("PRD-", 1000);
            IdGenerator histGen2 = new IdGenerator("HST-", 1000);
            IdGenerator catGen2 = new IdGenerator("CAT-", 1000);

            SqliteCategoryRepository catRepo2 = new SqliteCategoryRepository(db2, catGen2);
            SqliteProductRepository prodRepo2 = new SqliteProductRepository(db2, catRepo2, prodGen2);
            SqlitePriceHistoryRepository histRepo2 = new SqlitePriceHistoryRepository(db2, histGen2);
            SqliteSettingsRepository setRepo2 = new SqliteSettingsRepository(db2);
            db2.syncIdGenerators(prodGen2, histGen2, catGen2);

            Product reloaded = prodRepo2.findById("PRD-1001").orElseThrow();
            Assert.assertEquals("NVMe Enclosure", reloaded.getName(), "Name survived restart");
            Assert.assertEquals("Storage", reloaded.getCategory(), "Category survived restart");
            Assert.assertEquals(new BigDecimal("1199.50"), reloaded.getCostPrice(), "Cost price minor units exact");
            Assert.assertEquals(new BigDecimal("1749.00"), reloaded.getSellingPrice(), "Updated price survived restart");
            Assert.assertEquals(ProductStatus.ACTIVE, reloaded.getStatus(), "Restored status survived");

            Assert.assertEquals(1, histRepo2.findByProductId("PRD-1001").size(), "History survived restart");
            Assert.assertEquals("PRD-1002", prodGen2.nextId(), "High watermark preserved");
            Assert.assertEquals("HST-1002", histGen2.nextId(), "History high watermark preserved");
            Assert.assertEquals(7, setRepo2.getInt("low_stock_threshold", 10), "Settings persisted");

            // Permanent delete test
            ProductService prodSvc2 = new ProductService(prodRepo2, prodGen2, db2, null);
            prodSvc2.deleteProduct("PRD-1001");
            Assert.assertEquals(0, prodRepo2.count(), "Product permanently deleted");
            Assert.assertEquals(0, histRepo2.count(), "Cascading delete removed orphan history");
        }
    }

    private static void testSqliteBackupAndSafeRestore() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_sqlite_backup_");
        Path dbFile = tempDir.resolve("ppms.db");
        Path backupsDir = tempDir.resolve("backups");

        try (SqliteDatabaseManager db = new SqliteDatabaseManager(dbFile)) {
            IdGenerator prodGen = new IdGenerator("PRD-", 1000);
            IdGenerator histGen = new IdGenerator("HST-", 1000);
            IdGenerator catGen = new IdGenerator("CAT-", 1000);

            SqliteCategoryRepository catRepo = new SqliteCategoryRepository(db, catGen);
            SqliteProductRepository prodRepo = new SqliteProductRepository(db, catRepo, prodGen);
            SqlitePriceHistoryRepository histRepo = new SqlitePriceHistoryRepository(db, histGen);
            SqliteActivityRepository actRepo = new SqliteActivityRepository(db);
            SqliteSettingsRepository setRepo = new SqliteSettingsRepository(db);
            db.syncIdGenerators(prodGen, histGen, catGen);

            ProductService prodSvc = new ProductService(prodRepo, prodGen, db, actRepo);
            PricingService priceSvc = new PricingService(prodRepo, histRepo, histGen, db, actRepo);
            SqliteBackupService backupSvc = new SqliteBackupService(db, backupsDir, actRepo, setRepo, prodGen, histGen, catGen);

            Product p = prodSvc.addProduct("Desk Lamp", "Lighting", "LED", new BigDecimal("500.00"), new BigDecimal("850.00"), 15);
            BackupSnapshot snapshot = backupSvc.createBackup("Before price increase");
            Assert.assertTrue(Files.exists(snapshot.getPath()), "Backup file exists");

            // Mutate live database
            priceSvc.changePrice(p.getProductId(), new BigDecimal("1200.00"), "Inflation");
            prodSvc.addProduct("Extra Item", "Lighting", "", new BigDecimal("100.00"), new BigDecimal("200.00"), 5);
            Assert.assertEquals(2, prodRepo.count(), "Live DB has 2 products before restore");

            // Restore snapshot
            backupSvc.restoreBackupByFileName(snapshot.getFileName());
            Assert.assertEquals(1, prodRepo.count(), "Restored DB has original 1 product");
            Assert.assertEquals(new BigDecimal("850.00"), prodRepo.findById(p.getProductId()).orElseThrow().getSellingPrice(),
                    "Original selling price restored");
            Assert.assertTrue(backupSvc.listBackups().size() >= 2, "Pre-restore safety backup created alongside manual backup");
        }
    }

    private static void testCorruptedBackupRejectedWithoutOverwritingLiveDb() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_sqlite_corrupt_");
        Path dbFile = tempDir.resolve("ppms.db");
        Path backupsDir = tempDir.resolve("backups");
        Files.createDirectories(backupsDir);

        try (SqliteDatabaseManager db = new SqliteDatabaseManager(dbFile)) {
            IdGenerator prodGen = new IdGenerator("PRD-", 1000);
            IdGenerator histGen = new IdGenerator("HST-", 1000);
            IdGenerator catGen = new IdGenerator("CAT-", 1000);
            SqliteCategoryRepository catRepo = new SqliteCategoryRepository(db, catGen);
            SqliteProductRepository prodRepo = new SqliteProductRepository(db, catRepo, prodGen);
            ProductService prodSvc = new ProductService(prodRepo, prodGen, db, null);
            SqliteBackupService backupSvc = new SqliteBackupService(db, backupsDir, null, null, prodGen, histGen, catGen);

            prodSvc.addProduct("Important SKU", "General", "", new BigDecimal("200.00"), new BigDecimal("350.00"), 5);
            Path corrupted = backupsDir.resolve("corrupted_backup.db");
            Files.writeString(corrupted, "NOT A VALID SQLITE DATABASE FILE");

            Assert.assertThrows(IllegalStateException.class, () -> {
                backupSvc.restoreBackupByFileName("corrupted_backup.db");
            }, "Corrupted backup must fail verification");

            Assert.assertEquals(1, prodRepo.count(), "Live DB must remain completely intact after rejected restore");
        }
    }

    private static void testJsonCodecRoundTrip() {
        Map<String, Object> payload = Map.of(
                "name", "Mouse \"Pro\"\nLine 2",
                "price", new BigDecimal("1499.50"),
                "active", true,
                "tags", List.of("Peripherals", "Wireless")
        );
        String json = JsonCodec.toJson(payload);
        Map<String, Object> decoded = JsonCodec.parseObject(json);
        Assert.assertEquals("Mouse \"Pro\"\nLine 2", JsonCodec.getString(decoded, "name"), "Escaped JSON string decoded");
        Assert.assertEquals(new BigDecimal("1499.50"), JsonCodec.getBigDecimal(decoded, "price"), "BigDecimal decoded");
        Assert.assertEquals(Boolean.TRUE, JsonCodec.getBoolean(decoded, "active"), "Boolean decoded");
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
