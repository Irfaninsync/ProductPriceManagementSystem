package com.ppms;

import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.ProductRepository;
import com.ppms.service.ProductService;
import com.ppms.util.IdGenerator;
import com.ppms.validation.ProductValidator;
import com.ppms.validation.ValidationResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductTest {

    // Simple in-memory mock repository for isolated unit testing
    private static class InMemoryProductRepository implements ProductRepository {
        private final List<Product> list = new ArrayList<>();

        @Override
        public Product save(Product p) {
            list.removeIf(item -> item.getProductId().equalsIgnoreCase(p.getProductId()));
            list.add(new Product(p));
            return new Product(p);
        }

        @Override
        public Optional<Product> findById(String id) {
            return list.stream().filter(p -> p.getProductId().equalsIgnoreCase(id)).findFirst().map(Product::new);
        }

        @Override
        public boolean existsById(String id) {
            return list.stream().anyMatch(p -> p.getProductId().equalsIgnoreCase(id));
        }

        @Override
        public List<Product> findAll() { return new ArrayList<>(list); }

        @Override
        public List<Product> findActive() {
            return list.stream().filter(Product::isActive).toList();
        }

        @Override
        public List<Product> findArchived() {
            return list.stream().filter(Product::isArchived).toList();
        }

        @Override
        public int count() { return list.size(); }

        @Override
        public void reload() {}
    }

    public static void runAll() {
        System.out.println("Running Product Tests...");
        testCreateValidProduct();
        testRejectDuplicateId();
        testRejectInvalidPrices();
        testRejectInvalidQuantities();
        testUpdateValidProductDetails();
        testRejectInvalidEditsWithoutCorruptingEntity();
        testArchiveAndRestore();
        testSearchAndFilter();
        System.out.println("  -> All Product Tests Passed!");
    }

    private static void testCreateValidProduct() {
        Product p = new Product("PRD-1001", "Keyboard", "Peripherals", "Wireless",
                new BigDecimal("1500.00"), new BigDecimal("2500.00"), 10);
        ValidationResult v = ProductValidator.validate(p);
        Assert.assertTrue(v.isValid(), "Product should be valid");
        Assert.assertEquals("PRD-1001", p.getProductId(), "Product ID match");
        Assert.assertEquals(new BigDecimal("1000.00"), p.getGrossMarginAmount(), "Gross margin amount check");
    }

    private static void testRejectDuplicateId() {
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 1000);
        ProductService svc = new ProductService(repo, gen);

        svc.addProductWithId("PRD-1001", "Item A", "Cat A", "", new BigDecimal("10.00"), new BigDecimal("20.00"), 5);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            svc.addProductWithId("PRD-1001", "Item B", "Cat B", "", new BigDecimal("15.00"), new BigDecimal("30.00"), 2);
        }, "Should reject duplicate ID PRD-1001");
    }

    private static void testRejectInvalidPrices() {
        Product pNegativeCost = new Product("PRD-1002", "Item", "Cat", "",
                new BigDecimal("-5.00"), new BigDecimal("20.00"), 5);
        ValidationResult v1 = ProductValidator.validate(pNegativeCost);
        Assert.assertFalse(v1.isValid(), "Negative cost price must be invalid");

        Product pNegativeSelling = new Product("PRD-1003", "Item", "Cat", "",
                new BigDecimal("5.00"), new BigDecimal("-20.00"), 5);
        ValidationResult v2 = ProductValidator.validate(pNegativeSelling);
        Assert.assertFalse(v2.isValid(), "Negative selling price must be invalid");
    }

    private static void testRejectInvalidQuantities() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Product p = new Product("PRD-1004", "Item", "Cat", "",
                    new BigDecimal("5.00"), new BigDecimal("20.00"), 5);
            p.setQuantity(-10);
        }, "Negative quantity must throw exception");
    }

    private static void testUpdateValidProductDetails() {
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 1000);
        ProductService svc = new ProductService(repo, gen);

        Product p = svc.addProductWithId("PRD-1005", "Original Name", "Cat", "Desc",
                new BigDecimal("50.00"), new BigDecimal("100.00"), 15);

        Product updated = svc.updateProductDetails("PRD-1005", "Updated Name", "New Cat", "New Desc");
        Assert.assertEquals("Updated Name", updated.getName(), "Name should update");
        Assert.assertEquals("New Cat", updated.getCategory(), "Category should update");
    }

    private static void testRejectInvalidEditsWithoutCorruptingEntity() {
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 1000);
        ProductService svc = new ProductService(repo, gen);

        svc.addProductWithId("PRD-1006", "Valid Name", "Cat", "Desc",
                new BigDecimal("50.00"), new BigDecimal("100.00"), 15);

        // Attempt invalid update with blank name
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            svc.updateProductDetails("PRD-1006", "   ", "Cat", "Desc");
        }, "Should reject blank name");

        // Verify entity in repository was NOT corrupted
        Product p = svc.getProductById("PRD-1006").orElseThrow();
        Assert.assertEquals("Valid Name", p.getName(), "Original name must remain untouched after failed update");
    }

    private static void testArchiveAndRestore() {
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 1000);
        ProductService svc = new ProductService(repo, gen);

        svc.addProductWithId("PRD-1007", "Item", "Cat", "Desc",
                new BigDecimal("50.00"), new BigDecimal("100.00"), 10);

        svc.archiveProduct("PRD-1007");
        Product archived = svc.getProductById("PRD-1007").orElseThrow();
        Assert.assertEquals(ProductStatus.ARCHIVED, archived.getStatus(), "Status should be ARCHIVED");
        Assert.assertEquals(0, svc.getActiveProducts().size(), "Active products should be 0");
        Assert.assertEquals(1, svc.getArchivedProducts().size(), "Archived products should be 1");

        svc.restoreProduct("PRD-1007");
        Product restored = svc.getProductById("PRD-1007").orElseThrow();
        Assert.assertEquals(ProductStatus.ACTIVE, restored.getStatus(), "Status should be ACTIVE");
        Assert.assertEquals(1, svc.getActiveProducts().size(), "Active products should be 1");
    }

    private static void testSearchAndFilter() {
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 1000);
        ProductService svc = new ProductService(repo, gen);

        svc.addProductWithId("PRD-1008", "Mechanical Keyboard RGB", "Electronics", "",
                new BigDecimal("2000.00"), new BigDecimal("3500.00"), 20);
        svc.addProductWithId("PRD-1009", "Wireless Mouse Office", "Peripherals", "",
                new BigDecimal("400.00"), new BigDecimal("600.00"), 5);

        List<Product> searchResults = svc.search("mechanical");
        Assert.assertEquals(1, searchResults.size(), "Should find 1 item for search 'mechanical'");
        Assert.assertEquals("PRD-1008", searchResults.get(0).getProductId(), "Match product ID");

        List<Product> filtered = svc.filter("Peripherals", ProductStatus.ACTIVE, null, null, null, null, false, false, 10);
        Assert.assertEquals(1, filtered.size(), "Filter by category Peripherals");
        Assert.assertEquals("PRD-1009", filtered.get(0).getProductId(), "Match filtered product");
    }
}
