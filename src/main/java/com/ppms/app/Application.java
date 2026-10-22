package com.ppms.app;

import com.ppms.model.Product;
import com.ppms.persistence.CsvPriceHistoryRepository;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.MetadataHandler;
import com.ppms.report.ReportGenerator;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.service.AnalyticsService;
import com.ppms.service.BackupService;
import com.ppms.service.ImportExportService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.ui.ConsoleUI;
import com.ppms.ui.MenuController;
import com.ppms.util.IdGenerator;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Bootstraps system dependency injection, persistent directories, and starts the console application.
 */
public class Application {

    private final Path dataDirectory;
    private final Path productsFile;
    private final Path historyFile;
    private final Path metadataFile;

    public Application() {
        this(Paths.get("data"));
    }

    public Application(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.productsFile = dataDirectory.resolve("products.csv");
        this.historyFile = dataDirectory.resolve("price_history.csv");
        this.metadataFile = dataDirectory.resolve("metadata.csv");
    }

    public void run() {
        // 1. Generators
        IdGenerator productIdGenerator = new IdGenerator("PRD-", 1000);
        IdGenerator historyIdGenerator = new IdGenerator("HST-", 1000);

        // 2. Repositories
        ProductRepository productRepository = new CsvProductRepository(productsFile, productIdGenerator);
        PriceHistoryRepository historyRepository = new CsvPriceHistoryRepository(historyFile, historyIdGenerator);
        MetadataHandler metadataHandler = new MetadataHandler(metadataFile);

        // 3. Seed demonstration data if brand new installation
        if (productRepository.count() == 0) {
            bootstrapSampleData(productRepository, historyRepository, productIdGenerator, historyIdGenerator);
        }

        // 4. Services
        ProductService productService = new ProductService(productRepository, productIdGenerator);
        PricingService pricingService = new PricingService(productRepository, historyRepository, historyIdGenerator);
        AnalyticsService analyticsService = new AnalyticsService(productRepository, historyRepository);
        ImportExportService importExportService = new ImportExportService(productRepository, historyRepository, productIdGenerator);
        BackupService backupService = new BackupService(dataDirectory, productRepository, historyRepository);
        ReportGenerator reportGenerator = new ReportGenerator(productService, pricingService, analyticsService);

        // 5. UI & Controller
        Scanner scanner = new Scanner(System.in);
        ConsoleUI ui = new ConsoleUI(scanner);
        MenuController controller = new MenuController(
                ui,
                productService,
                pricingService,
                analyticsService,
                importExportService,
                backupService,
                reportGenerator,
                metadataHandler
        );

        // 6. Launch main workflow
        controller.start();
    }

    private void bootstrapSampleData(ProductRepository productRepo,
                                     PriceHistoryRepository historyRepo,
                                     IdGenerator prodGen,
                                     IdGenerator histGen) {
        // Seed initial diverse catalog
        Product p1 = new Product(prodGen.nextId(), "Logitech MX Master 3S", "Peripherals",
                "Ergonomic wireless performance mouse", new BigDecimal("5500.00"), new BigDecimal("8995.00"), 45);

        Product p2 = new Product(prodGen.nextId(), "Dell UltraSharp 27 4K", "Displays",
                "IPS USB-C Hub Monitor (U2723QE)", new BigDecimal("42000.00"), new BigDecimal("54990.00"), 12);

        Product p3 = new Product(prodGen.nextId(), "Keychron K2 Mechanical Keyboard", "Peripherals",
                "Wireless 75% mechanical keyboard Gateron Brown", new BigDecimal("6200.00"), new BigDecimal("8499.00"), 28);

        Product p4 = new Product(prodGen.nextId(), "SanDisk Extreme SSD 1TB", "Storage",
                "Rugged portable NVMe USB 3.2 Gen 2", new BigDecimal("7800.00"), new BigDecimal("10499.00"), 8); // Low stock

        Product p5 = new Product(prodGen.nextId(), "USB-C to HDMI Adapter 4K", "Accessories",
                "Braided thunderbolt compatible display cable", new BigDecimal("950.00"), new BigDecimal("799.00"), 50); // Below Cost!

        Product p6 = new Product(prodGen.nextId(), "Anker 65W GaN Charger", "Accessories",
                "Compact 3-port fast wall charger", new BigDecimal("2400.00"), new BigDecimal("3499.00"), 35);

        Product p7 = new Product(prodGen.nextId(), "Sony WH-1000XM5 Headphones", "Audio",
                "Premium active noise cancelling over-ear headphones", new BigDecimal("21000.00"), new BigDecimal("29990.00"), 15);

        Product p8 = new Product(prodGen.nextId(), "Notebook Spiral A5 Pack of 3", "Office Supplies",
                "80 GSM ruled premium wirebound notebooks", new BigDecimal("180.00"), new BigDecimal("299.00"), 120);

        Product p9 = new Product(prodGen.nextId(), "Ergonomic Aluminium Laptop Stand", "Accessories",
                "Adjustable ventilated desktop riser", new BigDecimal("1100.00"), new BigDecimal("1799.00"), 5); // Low stock

        Product p10 = new Product(prodGen.nextId(), "Microfiber Screen Cleaning Cloth", "Accessories",
                "Streak-free reusable cleaning cloths pack of 5", new BigDecimal("120.00"), new BigDecimal("99.00"), 65); // Below Cost!

        productRepo.save(p1);
        productRepo.save(p2);
        productRepo.save(p3);
        productRepo.save(p4);
        productRepo.save(p5);
        productRepo.save(p6);
        productRepo.save(p7);
        productRepo.save(p8);
        productRepo.save(p9);
        productRepo.save(p10);

        // Seed initial price audit records to demonstrate history analytics
        com.ppms.model.PriceHistory h1 = new com.ppms.model.PriceHistory(
                histGen.nextId(), p1.getProductId(), new BigDecimal("8495.00"), new BigDecimal("8995.00"),
                java.time.LocalDateTime.now().minusDays(15), "Supplier component cost increase"
        );
        com.ppms.model.PriceHistory h2 = new com.ppms.model.PriceHistory(
                histGen.nextId(), p2.getProductId(), new BigDecimal("58990.00"), new BigDecimal("54990.00"),
                java.time.LocalDateTime.now().minusDays(7), "Festive seasonal promotional discount"
        );
        com.ppms.model.PriceHistory h3 = new com.ppms.model.PriceHistory(
                histGen.nextId(), p7.getProductId(), new BigDecimal("32990.00"), new BigDecimal("29990.00"),
                java.time.LocalDateTime.now().minusDays(3), "Competitor market price matching"
        );

        historyRepo.save(h1);
        historyRepo.save(h2);
        historyRepo.save(h3);
    }
}
