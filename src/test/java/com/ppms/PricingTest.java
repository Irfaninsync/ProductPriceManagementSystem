package com.ppms;

import com.ppms.model.PriceHistory;
import com.ppms.model.PriceProposal;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.service.PricingService;
import com.ppms.util.IdGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PricingTest {

    private static class InMemoryPriceHistoryRepository implements PriceHistoryRepository {
        private final List<PriceHistory> list = new ArrayList<>();

        @Override
        public PriceHistory save(PriceHistory h) {
            list.add(h);
            return h;
        }

        @Override
        public List<PriceHistory> findAll() { return new ArrayList<>(list); }

        @Override
        public List<PriceHistory> findByProductId(String pid) {
            return list.stream().filter(h -> h.getProductId().equalsIgnoreCase(pid)).toList();
        }

        @Override
        public List<PriceHistory> findByDateRange(LocalDateTime start, LocalDateTime end) {
            return list.stream().filter(h -> (start == null || !h.getChangedAt().isBefore(start))
                    && (end == null || !h.getChangedAt().isAfter(end))).toList();
        }

        @Override
        public int count() { return list.size(); }

        @Override
        public void reload() {}
    }

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
        public List<Product> findActive() { return list.stream().filter(Product::isActive).toList(); }

        @Override
        public List<Product> findArchived() { return list.stream().filter(Product::isArchived).toList(); }

        @Override
        public int count() { return list.size(); }

        @Override
        public void reload() {}
    }

    public static void runAll() {
        System.out.println("Running Pricing Tests...");
        testPriceIncreaseAndAuditTrail();
        testPriceDecreaseAndAuditTrail();
        testProposalCancellationDoesNotModifyProduct();
        testIdenticalPriceRejection();
        testPercentageChangeCalculationFormula();
        testZeroOldPriceHandling();
        System.out.println("  -> All Pricing Tests Passed!");
    }

    private static void testPriceIncreaseAndAuditTrail() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();
        IdGenerator histGen = new IdGenerator("HST-", 1000);

        Product p = new Product("PRD-2001", "Wireless Mouse", "Peripherals", "",
                new BigDecimal("400.00"), new BigDecimal("599.00"), 20);
        prodRepo.save(p);

        PricingService pricingService = new PricingService(prodRepo, histRepo, histGen);

        PriceProposal proposal = pricingService.createProposal("PRD-2001", new BigDecimal("649.00"), "Supplier price increase");
        Assert.assertTrue(proposal.isValid(), "Proposal should be valid");
        Assert.assertEquals(new BigDecimal("50.00"), proposal.getDifference(), "Price difference should be 50.00");
        Assert.assertEquals(new BigDecimal("8.35"), proposal.getPercentageChange().orElseThrow(), "Percentage change check (8.35%)");

        PriceHistory record = pricingService.applyPriceChange(proposal);
        Assert.assertEquals("PRD-2001", record.getProductId(), "History product ID match");
        Assert.assertEquals(new BigDecimal("599.00"), record.getOldPrice(), "Old price recorded");
        Assert.assertEquals(new BigDecimal("649.00"), record.getNewPrice(), "New price recorded");
        Assert.assertEquals("Supplier price increase", record.getReason(), "Reason recorded");

        // Verify product in repository actually reflects new price
        Product updatedProduct = prodRepo.findById("PRD-2001").orElseThrow();
        Assert.assertEquals(new BigDecimal("649.00"), updatedProduct.getSellingPrice(), "Product selling price must be updated");
    }

    private static void testPriceDecreaseAndAuditTrail() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();
        IdGenerator histGen = new IdGenerator("HST-", 1000);

        Product p = new Product("PRD-2002", "SSD 1TB", "Storage", "",
                new BigDecimal("5000.00"), new BigDecimal("8000.00"), 10);
        prodRepo.save(p);

        PricingService pricingService = new PricingService(prodRepo, histRepo, histGen);
        PriceHistory history = pricingService.changePrice("PRD-2002", new BigDecimal("7200.00"), "Competitor match");

        Assert.assertTrue(history.isPriceDecrease(), "Should be identified as price decrease");
        Assert.assertEquals(new BigDecimal("-800.00"), history.getPriceDifference(), "Price difference -800.00");
        Assert.assertEquals(new BigDecimal("-10.00"), history.getPercentageChange().orElseThrow(), "Percentage change -10.00%");
    }

    private static void testProposalCancellationDoesNotModifyProduct() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();
        IdGenerator histGen = new IdGenerator("HST-", 1000);

        Product p = new Product("PRD-2003", "Monitor", "Displays", "",
                new BigDecimal("10000.00"), new BigDecimal("15000.00"), 5);
        prodRepo.save(p);

        PricingService pricingService = new PricingService(prodRepo, histRepo, histGen);

        // Create proposal preview, but DO NOT call applyPriceChange
        PriceProposal proposal = pricingService.createProposal("PRD-2003", new BigDecimal("19000.00"), "Inflation");
        Assert.assertTrue(proposal.isValid(), "Proposal is valid");

        // Verify product in repository remains 15000.00
        Product unchanged = prodRepo.findById("PRD-2003").orElseThrow();
        Assert.assertEquals(new BigDecimal("15000.00"), unchanged.getSellingPrice(), "Price must not change on unconfirmed preview");
        Assert.assertEquals(0, histRepo.count(), "No history record should be generated on cancellation");
    }

    private static void testIdenticalPriceRejection() {
        ProductRepository prodRepo = new InMemoryProductRepository();
        PriceHistoryRepository histRepo = new InMemoryPriceHistoryRepository();
        IdGenerator histGen = new IdGenerator("HST-", 1000);

        Product p = new Product("PRD-2004", "Keyboard", "Peripherals", "",
                new BigDecimal("500.00"), new BigDecimal("1000.00"), 10);
        prodRepo.save(p);

        PricingService pricingService = new PricingService(prodRepo, histRepo, histGen);

        PriceProposal proposal = pricingService.createProposal("PRD-2004", new BigDecimal("1000.00"), "No reason");
        Assert.assertFalse(proposal.isValid(), "Identical price proposal must be marked invalid");

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            pricingService.applyPriceChange(proposal);
        }, "Applying identical price must throw IllegalArgumentException");
    }

    private static void testPercentageChangeCalculationFormula() {
        // (150 - 100) / 100 * 100 = 50.00%
        PriceHistory h1 = new PriceHistory("HST-1", "P1", new BigDecimal("100.00"), new BigDecimal("150.00"),
                LocalDateTime.now(), "Increase");
        Assert.assertEquals(new BigDecimal("50.00"), h1.getPercentageChange().orElseThrow(), "50% increase");

        // (50 - 100) / 100 * 100 = -50.00%
        PriceHistory h2 = new PriceHistory("HST-2", "P2", new BigDecimal("100.00"), new BigDecimal("50.00"),
                LocalDateTime.now(), "Decrease");
        Assert.assertEquals(new BigDecimal("-50.00"), h2.getPercentageChange().orElseThrow(), "50% decrease");
    }

    private static void testZeroOldPriceHandling() {
        // When old price is 0, percentage change is mathematically undefined
        PriceHistory hZero = new PriceHistory("HST-3", "P3", BigDecimal.ZERO, new BigDecimal("100.00"),
                LocalDateTime.now(), "First price set");
        Assert.assertTrue(hZero.getPercentageChange().isEmpty(), "Percentage change should be empty Optional when old price is 0");
    }
}
