// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import com.ppms.model.ImportReport;
import com.ppms.model.Product;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.service.ImportExportService;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportExportTest {

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

    public static void runAll() throws IOException {
        System.out.println("Running Import/Export Tests...");
        testImportValidationAndCommit();
        testExportAndRoundTripRead();
        System.out.println("  -> All Import/Export Tests Passed!");
    }

    private static void testImportValidationAndCommit() throws IOException {
        Path tempFile = Files.createTempFile("ppms_import_", ".csv");
        // CSV with 2 valid rows, 1 duplicate ID, 1 negative price row
        String csvData = "productId,name,category,description,costPrice,sellingPrice,quantity\r\n"
                + "PRD-5001,Valid Mouse,Peripherals,Fast,300.00,500.00,20\r\n"
                + "PRD-5002,Valid Pad,Peripherals,Cloth,100.00,200.00,50\r\n"
                + "PRD-5001,Duplicate ID Item,Peripherals,Test,10.00,20.00,5\r\n"
                + "PRD-5003,Negative Price Item,Peripherals,Test,10.00,-20.00,5\r\n";
        Files.writeString(tempFile, csvData);

        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 5000);
        ImportExportService service = new ImportExportService(repo, null, gen);

        ImportReport report = service.previewImport(tempFile);
        Assert.assertEquals(4, report.getTotalRowsProcessed(), "Total processed rows");
        Assert.assertEquals(2, report.getAcceptedCount(), "Accepted rows should be 2");
        Assert.assertEquals(2, report.getRejectedCount(), "Rejected rows should be 2");

        // Verify rejection reasons
        Assert.assertTrue(report.getRejections().get(0).getReason().contains("Duplicate product ID 'PRD-5001'"),
                "Line 4 rejection should be duplicate ID");
        Assert.assertTrue(report.getRejections().get(1).getReason().contains("negative"),
                "Line 5 rejection should be negative selling price");

        // Commit import
        int saved = service.applyImport(report);
        Assert.assertEquals(2, saved, "Saved count matches accepted");
        Assert.assertEquals(2, repo.count(), "Repository size should be 2");
        Assert.assertTrue(repo.existsById("PRD-5001"), "PRD-5001 saved");
        Assert.assertTrue(repo.existsById("PRD-5002"), "PRD-5002 saved");
    }

    private static void testExportAndRoundTripRead() throws IOException {
        Path exportFile = Files.createTempFile("ppms_export_", ".csv");
        ProductRepository repo = new InMemoryProductRepository();
        IdGenerator gen = new IdGenerator("PRD-", 6000);
        ImportExportService service = new ImportExportService(repo, null, gen);

        Product p1 = new Product("PRD-6001", "USB Dongle, Type-C", "Adapters", "Supports 10Gbps",
                new BigDecimal("250.00"), new BigDecimal("499.00"), 40);
        Product p2 = new Product("PRD-6002", "Mechanical Switch Tester", "Tools", "Includes 9 switches",
                new BigDecimal("450.00"), new BigDecimal("799.00"), 12);

        service.exportProducts(List.of(p1, p2), exportFile);
        Assert.assertTrue(Files.exists(exportFile), "Export file must exist");

        // Read exported file back into a clean repository
        ProductRepository cleanRepo = new InMemoryProductRepository();
        ImportExportService cleanService = new ImportExportService(cleanRepo, null, new IdGenerator("PRD-", 7000));
        ImportReport report = cleanService.previewImport(exportFile);

        Assert.assertEquals(2, report.getAcceptedCount(), "All exported products should be valid on re-import");
        Assert.assertEquals(0, report.getRejectedCount(), "Zero rejections on clean export re-import");
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

