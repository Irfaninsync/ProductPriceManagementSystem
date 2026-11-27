// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.ppms.app.AppPaths;
import com.ppms.db.SqliteBackupService;
import com.ppms.db.SqliteDatabaseManager;
import com.ppms.model.BackupSnapshot;
import com.ppms.model.ImportReport;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.persistence.CsvFileHandler;
import com.ppms.persistence.CsvPriceHistoryRepository;
import com.ppms.persistence.CsvRow;
import com.ppms.repository.ActivityRepository;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.repository.SettingsRepository;
import com.ppms.service.ImportExportService;
import com.ppms.service.ProductService;
import com.ppms.util.CurrencyFormatter;
import com.ppms.util.IdGenerator;
import com.ppms.util.JsonCodec;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP handler for application settings, SQLite backup/restore, CSV import,
 * legacy CSV migration, optional demo seeding, and graceful shutdown.
 */
public class DataAndSettingsHandler {

    public static final String APP_VERSION = "2.0.0";

    private final AppPaths paths;
    private final SqliteDatabaseManager db;
    private final SettingsRepository settingsRepository;
    private final ActivityRepository activityRepository;
    private final SqliteBackupService backupService;
    private final ImportExportService importExportService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IdGenerator historyIdGenerator;
    private final Runnable shutdownHook;

    public DataAndSettingsHandler(AppPaths paths,
                                  SqliteDatabaseManager db,
                                  SettingsRepository settingsRepository,
                                  ActivityRepository activityRepository,
                                  SqliteBackupService backupService,
                                  ImportExportService importExportService,
                                  ProductService productService,
                                  ProductRepository productRepository,
                                  PriceHistoryRepository priceHistoryRepository,
                                  IdGenerator historyIdGenerator,
                                  Runnable shutdownHook) {
        this.paths = paths;
        this.db = db;
        this.settingsRepository = settingsRepository;
        this.activityRepository = activityRepository;
        this.backupService = backupService;
        this.importExportService = importExportService;
        this.productService = productService;
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.historyIdGenerator = historyIdGenerator;
        this.shutdownHook = shutdownHook;
    }

    public void handleSettings(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(method)) {
            sendSettings(exchange);
        } else if ("PUT".equals(method)) {
            updateSettings(exchange);
        } else {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
        }
    }

    public void handleBackups(HttpExchange exchange, String subPath) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if (subPath.isEmpty() || "/".equals(subPath)) {
            if ("GET".equals(method)) {
                List<BackupSnapshot> list = backupService.listBackups();
                HttpContextHelper.sendJson(exchange, 200, Map.of(
                        "items", list.stream().map(HttpContextHelper::toMap).toList(),
                        "backupsDir", paths.getBackupsDir().toString()
                ));
            } else if ("POST".equals(method)) {
                Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
                String note = JsonCodec.getStringOrDefault(body, "note", "Manual backup");
                BackupSnapshot created = backupService.createBackup(note);
                HttpContextHelper.sendJson(exchange, 201, HttpContextHelper.toMap(created));
            } else {
                HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            }
            return;
        }

        if ("/restore".equals(subPath) && "POST".equals(method)) {
            Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
            String fileName = JsonCodec.getStringOrDefault(body, "fileName", "");
            backupService.restoreBackupByFileName(fileName);
            String restoredCurrency = settingsRepository.get("currency_symbol", "\u20B9");
            CurrencyFormatter.setCurrencySymbol(restoredCurrency);
            HttpContextHelper.sendJson(exchange, 200, Map.of(
                    "restored", true,
                    "fileName", fileName
            ));
            return;
        }

        HttpContextHelper.sendError(exchange, 404, "Backup endpoint not found");
    }

    public void handleImport(HttpExchange exchange, String subPath) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);
        String csvContent = JsonCodec.getStringOrDefault(body, "csvContent", "");
        if (csvContent.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV content cannot be empty.");
        }

        ImportReport report = importExportService.previewImportFromString(csvContent);
        if ("/preview".equals(subPath)) {
            List<Map<String, Object>> rejections = report.getRejections().stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("lineNumber", r.getLineNumber());
                m.put("rawRecord", r.getRawRecord());
                m.put("reason", r.getReason());
                return m;
            }).toList();

            HttpContextHelper.sendJson(exchange, 200, Map.of(
                    "totalRows", report.getTotalRowsProcessed(),
                    "acceptedCount", report.getAcceptedCount(),
                    "rejectedCount", report.getRejectedCount(),
                    "validProducts", report.getValidProducts().stream().map(HttpContextHelper::toMap).toList(),
                    "rejections", rejections
            ));
        } else if ("/commit".equals(subPath)) {
            if (report.getAcceptedCount() == 0) {
                throw new IllegalArgumentException("No valid product rows found to import.");
            }
            int importedCount = importExportService.applyImport(report);
            HttpContextHelper.sendJson(exchange, 200, Map.of(
                    "importedCount", importedCount,
                    "rejectedCount", report.getRejectedCount()
            ));
        } else {
            HttpContextHelper.sendError(exchange, 404, "Import endpoint not found");
        }
    }

    public void handleDataActions(HttpExchange exchange, String subPath) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpContextHelper.sendError(exchange, 405, "Method not allowed");
            return;
        }
        if ("/import-legacy-csv".equals(subPath)) {
            importLegacyCsv(exchange);
        } else if ("/load-demo".equals(subPath)) {
            loadDemoCatalog(exchange);
        } else {
            HttpContextHelper.sendError(exchange, 404, "Data action not found");
        }
    }

    public void handleSystem(HttpExchange exchange, String subPath) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        if ("/health".equals(subPath) && "GET".equals(method)) {
            HttpContextHelper.sendJson(exchange, 200, Map.of(
                    "status", db.isHealthy() ? "OK" : "ERROR",
                    "appVersion", APP_VERSION,
                    "schemaVersion", db.getSchemaVersion()
            ));
        } else if ("/shutdown".equals(subPath) && "POST".equals(method)) {
            HttpContextHelper.sendJson(exchange, 200, Map.of("shuttingDown", true));
            if (shutdownHook != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {}
                    shutdownHook.run();
                }, "ppms-shutdown-trigger").start();
            }
        } else {
            HttpContextHelper.sendError(exchange, 404, "System endpoint not found");
        }
    }

    private void sendSettings(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("businessName", settingsRepository.get("business_name", "My Business"));
        payload.put("currencySymbol", settingsRepository.get("currency_symbol", "\u20B9"));
        payload.put("lowStockThreshold", settingsRepository.getInt("low_stock_threshold", 10));
        payload.put("autoBackupStartup", settingsRepository.getBoolean("auto_backup_startup", true));
        payload.put("lastBackupAt", settingsRepository.get("last_backup_at", ""));
        payload.put("appName", "Product Price Management System (PPMS)");
        payload.put("appVersion", APP_VERSION);
        payload.put("javaVersion", System.getProperty("java.version"));
        payload.put("dataDir", paths.getDataDir().toString());
        payload.put("databaseFile", paths.getDatabaseFile().toString());
        payload.put("backupsDir", paths.getBackupsDir().toString());
        payload.put("exportsDir", paths.getExportsDir().toString());
        payload.put("databaseSizeBytes", db.getDatabaseSizeBytes());
        payload.put("schemaVersion", db.getSchemaVersion());
        payload.put("databaseHealthy", db.isHealthy());
        payload.put("totalProducts", productRepository.count());
        payload.put("totalPriceHistory", priceHistoryRepository.count());
        payload.put("hasLegacyCsv", Files.isRegularFile(Paths.get("data", "products.csv")));
        HttpContextHelper.sendJson(exchange, 200, payload);
    }

    private void updateSettings(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpContextHelper.readJsonObject(exchange);

        String businessName = JsonCodec.getString(body, "businessName");
        if (businessName != null) {
            String cleanName = businessName.trim();
            if (cleanName.length() < 2 || cleanName.length() > 80) {
                throw new IllegalArgumentException("Business name must be between 2 and 80 characters.");
            }
            settingsRepository.set("business_name", cleanName);
        }

        String currency = JsonCodec.getString(body, "currencySymbol");
        if (currency != null) {
            String cleanCurrency = currency.trim();
            if (cleanCurrency.isEmpty() || cleanCurrency.length() > 8) {
                throw new IllegalArgumentException("Currency symbol must be 1 to 8 characters.");
            }
            settingsRepository.set("currency_symbol", cleanCurrency);
            CurrencyFormatter.setCurrencySymbol(cleanCurrency);
        }

        Integer threshold = JsonCodec.getInt(body, "lowStockThreshold");
        if (threshold != null) {
            if (threshold < 1 || threshold > 10000) {
                throw new IllegalArgumentException("Low-stock threshold must be between 1 and 10,000.");
            }
            settingsRepository.set("low_stock_threshold", String.valueOf(threshold));
        }

        Boolean autoBackup = JsonCodec.getBoolean(body, "autoBackupStartup");
        if (autoBackup != null) {
            settingsRepository.set("auto_backup_startup", String.valueOf(autoBackup));
        }

        if (activityRepository != null) {
            activityRepository.record("SETTINGS_UPDATED", "SYSTEM", "SETTINGS", "Updated application settings");
        }
        sendSettings(exchange);
    }

    private void importLegacyCsv(HttpExchange exchange) throws IOException {
        Path legacyProducts = Paths.get("data", "products.csv");
        Path legacyHistory = Paths.get("data", "price_history.csv");
        if (!Files.isRegularFile(legacyProducts)) {
            throw new IllegalStateException("No legacy data/products.csv file found in the application directory.");
        }

        ImportReport report = importExportService.previewImport(legacyProducts);
        int importedProducts = importExportService.applyImport(report);

        int importedHistory = 0;
        if (Files.isRegularFile(legacyHistory)) {
            List<CsvRow> rows = CsvFileHandler.readRows(legacyHistory, CsvPriceHistoryRepository.HEADERS);
            List<String> existingHistoryIds = priceHistoryRepository.findAll().stream()
                    .map(h -> h.getHistoryId().toUpperCase(Locale.ROOT)).toList();

            List<PriceHistory> toInsert = new ArrayList<>();
            for (CsvRow row : rows) {
                try {
                    String hid = row.getString("historyId").toUpperCase(Locale.ROOT);
                    String pid = row.getString("productId").toUpperCase(Locale.ROOT);
                    if (hid.isEmpty() || existingHistoryIds.contains(hid) || !productRepository.existsById(pid)) {
                        continue;
                    }
                    toInsert.add(new PriceHistory(
                            hid,
                            pid,
                            row.getBigDecimal("oldPrice"),
                            row.getBigDecimal("newPrice"),
                            row.getLocalDateTime("changedAt"),
                            row.getString("reason")
                    ));
                } catch (Exception ignored) {}
            }

            if (!toInsert.isEmpty()) {
                importedHistory = db.inTransaction(() -> {
                    int count = 0;
                    for (PriceHistory h : toInsert) {
                        priceHistoryRepository.save(h);
                        historyIdGenerator.observeId(h.getHistoryId());
                        count++;
                    }
                    return count;
                });
            }
        }

        if (activityRepository != null) {
            activityRepository.record("CSV_IMPORTED", "SYSTEM", "LEGACY_CSV",
                    "Migrated " + importedProducts + " product(s) and " + importedHistory + " history record(s) from CSV");
        }

        HttpContextHelper.sendJson(exchange, 200, Map.of(
                "importedProducts", importedProducts,
                "importedHistory", importedHistory,
                "rejectedRows", report.getRejectedCount()
        ));
    }

    private void loadDemoCatalog(HttpExchange exchange) throws IOException {
        if (productRepository.count() > 0) {
            throw new IllegalStateException("Sample catalog can only be loaded into an empty database.");
        }

        int createdCount = db.inTransaction(() -> {
            List<Product> seeded = new ArrayList<>();
            seeded.add(productService.addProduct("Logitech MX Master 3S", "Peripherals",
                    "Ergonomic wireless performance mouse", new BigDecimal("5500.00"), new BigDecimal("8995.00"), 45));
            seeded.add(productService.addProduct("Dell UltraSharp 27 4K", "Displays",
                    "IPS USB-C Hub Monitor (U2723QE)", new BigDecimal("42000.00"), new BigDecimal("54990.00"), 12));
            seeded.add(productService.addProduct("Keychron K2 Mechanical Keyboard", "Peripherals",
                    "Wireless 75% mechanical keyboard", new BigDecimal("6200.00"), new BigDecimal("8499.00"), 28));
            seeded.add(productService.addProduct("SanDisk Extreme SSD 1TB", "Storage",
                    "Rugged portable NVMe USB 3.2 Gen 2", new BigDecimal("7800.00"), new BigDecimal("10499.00"), 8));
            seeded.add(productService.addProduct("USB-C to HDMI Adapter 4K", "Accessories",
                    "Braided display adapter cable", new BigDecimal("950.00"), new BigDecimal("799.00"), 50));
            seeded.add(productService.addProduct("Anker 65W GaN Charger", "Accessories",
                    "Compact 3-port fast wall charger", new BigDecimal("2400.00"), new BigDecimal("3499.00"), 35));
            seeded.add(productService.addProduct("Sony WH-1000XM5 Headphones", "Audio",
                    "Active noise cancelling over-ear headphones", new BigDecimal("21000.00"), new BigDecimal("29990.00"), 15));
            seeded.add(productService.addProduct("A5 Wirebound Notebooks (Pack of 3)", "Office Supplies",
                    "80 GSM ruled notebooks", new BigDecimal("180.00"), new BigDecimal("299.00"), 120));

            Product p1 = seeded.get(0);
            Product p2 = seeded.get(1);
            Product p7 = seeded.get(6);
            priceHistoryRepository.save(new PriceHistory(historyIdGenerator.nextId(), p1.getProductId(),
                    new BigDecimal("8495.00"), new BigDecimal("8995.00"),
                    LocalDateTime.now().minusDays(14), "Supplier Price Increase"));
            priceHistoryRepository.save(new PriceHistory(historyIdGenerator.nextId(), p2.getProductId(),
                    new BigDecimal("58990.00"), new BigDecimal("54990.00"),
                    LocalDateTime.now().minusDays(7), "Seasonal Adjustment"));
            priceHistoryRepository.save(new PriceHistory(historyIdGenerator.nextId(), p7.getProductId(),
                    new BigDecimal("32990.00"), new BigDecimal("29990.00"),
                    LocalDateTime.now().minusDays(3), "Competitor Price Matching"));

            if (activityRepository != null) {
                activityRepository.record("DEMO_LOADED", "SYSTEM", "DEMO",
                        "Loaded sample demonstration catalog (" + seeded.size() + " products)");
            }
            return seeded.size();
        });

        HttpContextHelper.sendJson(exchange, 200, Map.of("loadedProducts", createdCount));
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
