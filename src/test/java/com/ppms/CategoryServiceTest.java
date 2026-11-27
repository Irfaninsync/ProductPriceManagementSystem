// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.db.SqliteActivityRepository;
import com.ppms.db.SqliteCategoryRepository;
import com.ppms.db.SqliteDatabaseManager;
import com.ppms.db.SqliteProductRepository;
import com.ppms.model.Category;
import com.ppms.model.CategorySummary;
import com.ppms.model.Product;
import com.ppms.service.CategoryService;
import com.ppms.service.ProductService;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CategoryServiceTest {

    public static void runAll() throws IOException {
        System.out.println("Running Category Service Tests...");
        testCreateRenameAndDeleteProtection();
        testDeleteWithReassignment();
        testCategorySummaryMetrics();
        System.out.println("  -> All Category Service Tests Passed!");
    }

    private static void testCreateRenameAndDeleteProtection() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_cat_test_");
        try (SqliteDatabaseManager db = new SqliteDatabaseManager(tempDir.resolve("ppms.db"))) {
            IdGenerator prodGen = new IdGenerator("PRD-", 1000);
            IdGenerator catGen = new IdGenerator("CAT-", 1000);
            SqliteCategoryRepository catRepo = new SqliteCategoryRepository(db, catGen);
            SqliteProductRepository prodRepo = new SqliteProductRepository(db, catRepo, prodGen);
            SqliteActivityRepository actRepo = new SqliteActivityRepository(db);

            CategoryService catSvc = new CategoryService(catRepo, prodRepo, catGen, db, actRepo);
            ProductService prodSvc = new ProductService(prodRepo, prodGen, db, actRepo);

            Category created = catSvc.createCategory("Networking", "Routers and switches");
            Assert.assertEquals("CAT-1001", created.getCategoryId(), "First category ID");
            Assert.assertEquals("Networking", created.getName(), "Category name");

            Assert.assertThrows(IllegalArgumentException.class, () -> {
                catSvc.createCategory("networking", "Duplicate case-insensitive");
            }, "Duplicate category name must be rejected");

            Product p = prodSvc.addProduct("WiFi 6 Router", "Networking", "Dual band",
                    new BigDecimal("2500.00"), new BigDecimal("3999.00"), 12);

            // Rename category and verify product reflects new name via foreign key join
            catSvc.updateCategory(created.getCategoryId(), "Network Gear", "Updated desc");
            Product updatedProduct = prodSvc.getProductById(p.getProductId()).orElseThrow();
            Assert.assertEquals("Network Gear", updatedProduct.getCategory(), "Product category must reflect renamed category");

            // Attempting to delete non-empty category without reassignment must fail
            Assert.assertThrows(IllegalStateException.class, () -> {
                catSvc.deleteCategory(created.getCategoryId(), null);
            }, "Deleting non-empty category without reassignment must throw");
        }
    }

    private static void testDeleteWithReassignment() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_cat_reassign_");
        try (SqliteDatabaseManager db = new SqliteDatabaseManager(tempDir.resolve("ppms.db"))) {
            IdGenerator prodGen = new IdGenerator("PRD-", 1000);
            IdGenerator catGen = new IdGenerator("CAT-", 1000);
            SqliteCategoryRepository catRepo = new SqliteCategoryRepository(db, catGen);
            SqliteProductRepository prodRepo = new SqliteProductRepository(db, catRepo, prodGen);
            SqliteActivityRepository actRepo = new SqliteActivityRepository(db);

            CategoryService catSvc = new CategoryService(catRepo, prodRepo, catGen, db, actRepo);
            ProductService prodSvc = new ProductService(prodRepo, prodGen, db, actRepo);

            Category c1 = catSvc.createCategory("Old Cat", "");
            Category c2 = catSvc.createCategory("New Cat", "");
            Product p = prodSvc.addProduct("Cable", "Old Cat", "", new BigDecimal("50.00"), new BigDecimal("120.00"), 10);

            catSvc.deleteCategory(c1.getCategoryId(), c2.getCategoryId());
            Assert.assertTrue(catSvc.getCategoryById(c1.getCategoryId()).isEmpty(), "Source category deleted");
            Product reassigned = prodSvc.getProductById(p.getProductId()).orElseThrow();
            Assert.assertEquals("New Cat", reassigned.getCategory(), "Product reassigned to target category");
        }
    }

    private static void testCategorySummaryMetrics() throws IOException {
        Path tempDir = Files.createTempDirectory("ppms_cat_summary_");
        try (SqliteDatabaseManager db = new SqliteDatabaseManager(tempDir.resolve("ppms.db"))) {
            IdGenerator prodGen = new IdGenerator("PRD-", 1000);
            IdGenerator catGen = new IdGenerator("CAT-", 1000);
            SqliteCategoryRepository catRepo = new SqliteCategoryRepository(db, catGen);
            SqliteProductRepository prodRepo = new SqliteProductRepository(db, catRepo, prodGen);
            SqliteActivityRepository actRepo = new SqliteActivityRepository(db);

            CategoryService catSvc = new CategoryService(catRepo, prodRepo, catGen, db, actRepo);
            ProductService prodSvc = new ProductService(prodRepo, prodGen, db, actRepo);

            prodSvc.addProduct("Item 1", "Audio", "", new BigDecimal("100.00"), new BigDecimal("200.00"), 4);
            prodSvc.addProduct("Item 2", "Audio", "", new BigDecimal("50.00"), new BigDecimal("100.00"), 6);

            List<CategorySummary> summaries = catSvc.getCategorySummaries(null);
            Assert.assertEquals(1, summaries.size(), "One summary entry");
            CategorySummary audio = summaries.get(0);
            Assert.assertEquals(2, audio.getActiveProducts(), "Active count 2");
            Assert.assertEquals(10, audio.getTotalUnits(), "Total units 10");
            // Cost: 400 + 300 = 700, Selling: 800 + 600 = 1400, Margin: 700, Avg price: 150
            Assert.assertEquals(new BigDecimal("700.00"), audio.getTotalCostValue(), "Total cost valuation");
            Assert.assertEquals(new BigDecimal("1400.00"), audio.getTotalSellingValue(), "Total selling valuation");
            Assert.assertEquals(new BigDecimal("150.00"), audio.getAvgSellingPrice(), "Avg selling price");
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
