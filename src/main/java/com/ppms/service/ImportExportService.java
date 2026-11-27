// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.model.CategorySummary;
import com.ppms.model.ImportReport;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.persistence.CsvFileHandler;
import com.ppms.persistence.CsvParser;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.CsvRow;
import com.ppms.persistence.CsvWriter;
import com.ppms.repository.ActivityRepository;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.repository.TransactionManager;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import com.ppms.validation.ProductValidator;
import com.ppms.validation.ValidationResult;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service managing CSV export generation and robust, validated CSV imports.
 */
public class ImportExportService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IdGenerator idGenerator;
    private final TransactionManager transactionManager;
    private final ActivityRepository activityRepository;

    public ImportExportService(ProductRepository productRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               IdGenerator idGenerator) {
        this(productRepository, priceHistoryRepository, idGenerator, TransactionManager.direct(), null);
    }

    public ImportExportService(ProductRepository productRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               IdGenerator idGenerator,
                               TransactionManager transactionManager,
                               ActivityRepository activityRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.priceHistoryRepository = priceHistoryRepository;
        this.idGenerator = Objects.requireNonNull(idGenerator, "IdGenerator cannot be null");
        this.transactionManager = transactionManager != null ? transactionManager : TransactionManager.direct();
        this.activityRepository = activityRepository;
    }

    public void exportProducts(List<Product> products, Path destinationPath) throws IOException {
        CsvFileHandler.writeRowsSafely(destinationPath, buildProductCsvRows(products));
    }

    public String exportProductsToCsvString(List<Product> products) throws IOException {
        StringWriter sw = new StringWriter();
        CsvWriter.writeAll(sw, buildProductCsvRows(products));
        return sw.toString();
    }

    private List<List<String>> buildProductCsvRows(List<Product> products) {
        List<List<String>> records = new ArrayList<>();
        records.add(CsvProductRepository.HEADERS);
        for (Product p : products) {
            records.add(List.of(
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    p.getDescription() != null ? p.getDescription() : "",
                    p.getCostPrice().toPlainString(),
                    p.getSellingPrice().toPlainString(),
                    String.valueOf(p.getQuantity()),
                    p.getStatus().name(),
                    DateTimeUtil.formatIso(p.getCreatedAt()),
                    DateTimeUtil.formatIso(p.getUpdatedAt())
            ));
        }
        return records;
    }

    public void exportPriceHistory(List<PriceHistory> historyList, Path destinationPath) throws IOException {
        CsvFileHandler.writeRowsSafely(destinationPath, buildHistoryCsvRows(historyList));
    }

    public String exportPriceHistoryToCsvString(List<PriceHistory> historyList) throws IOException {
        StringWriter sw = new StringWriter();
        CsvWriter.writeAll(sw, buildHistoryCsvRows(historyList));
        return sw.toString();
    }

    private List<List<String>> buildHistoryCsvRows(List<PriceHistory> historyList) {
        List<List<String>> records = new ArrayList<>();
        records.add(List.of("historyId", "productId", "oldPrice", "newPrice", "changedAt", "reason"));
        for (PriceHistory h : historyList) {
            records.add(List.of(
                    h.getHistoryId(),
                    h.getProductId(),
                    h.getOldPrice().toPlainString(),
                    h.getNewPrice().toPlainString(),
                    DateTimeUtil.formatIso(h.getChangedAt()),
                    h.getReason()
            ));
        }
        return records;
    }

    public String exportCategorySummaryToCsvString(List<CategorySummary> summaries) throws IOException {
        List<List<String>> records = new ArrayList<>();
        records.add(List.of("categoryId", "name", "description", "totalProducts", "activeProducts",
                "totalUnits", "totalCostValue", "totalSellingValue", "totalMarginAmount", "marginPercentage", "avgSellingPrice"));
        for (CategorySummary cs : summaries) {
            records.add(List.of(
                    cs.getCategoryId(),
                    cs.getName(),
                    cs.getDescription(),
                    String.valueOf(cs.getTotalProducts()),
                    String.valueOf(cs.getActiveProducts()),
                    String.valueOf(cs.getTotalUnits()),
                    cs.getTotalCostValue().toPlainString(),
                    cs.getTotalSellingValue().toPlainString(),
                    cs.getTotalMarginAmount().toPlainString(),
                    cs.getMarginPercentage().map(BigDecimal::toPlainString).orElse("0.00"),
                    cs.getAvgSellingPrice().toPlainString()
            ));
        }
        StringWriter sw = new StringWriter();
        CsvWriter.writeAll(sw, records);
        return sw.toString();
    }

    /**
     * Analyzes and validates a CSV file for import without committing changes.
     */
    public ImportReport previewImport(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            throw new IOException("Import file does not exist: " + csvFile);
        }
        List<CsvRow> rows = CsvFileHandler.readRows(csvFile, null);
        return previewRows(rows);
    }

    /**
     * Analyzes and validates CSV text content (e.g. uploaded from the local UI) without committing changes.
     */
    public ImportReport previewImportFromString(String csvContent) throws IOException {
        List<List<String>> rawRecords = CsvParser.parseString(csvContent);
        if (rawRecords.isEmpty()) {
            return new ImportReport();
        }
        List<String> headerLine = rawRecords.get(0);
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerLine.size(); i++) {
            headerMap.put(headerLine.get(i).trim().toLowerCase(), i);
        }
        List<CsvRow> rows = new ArrayList<>();
        for (int i = 1; i < rawRecords.size(); i++) {
            List<String> rawRow = rawRecords.get(i);
            boolean allEmpty = rawRow.stream().allMatch(s -> s == null || s.trim().isEmpty());
            if (!allEmpty) {
                rows.add(new CsvRow(i + 1, headerMap, rawRow));
            }
        }
        return previewRows(rows);
    }

    private ImportReport previewRows(List<CsvRow> rows) {
        ImportReport report = new ImportReport();
        report.setTotalRowsProcessed(rows.size());

        Set<String> seenIdsInBatch = new HashSet<>();
        long syntheticSeq = idGenerator.getCurrentSequence();

        for (CsvRow row : rows) {
            String rawLine = String.join(",", row.getRawValues());

            try {
                if (!row.hasColumn("name") || !row.hasColumn("category")
                        || !row.hasColumn("costprice") || !row.hasColumn("sellingprice")
                        || !row.hasColumn("quantity")) {
                    report.recordRejection(row.getLineNumber(), rawLine,
                            "Missing required columns. Header must include: name, category, costPrice, sellingPrice, quantity");
                    continue;
                }

                String id = row.hasColumn("productid") ? row.getString("productId") : "";
                if (id.isEmpty()) {
                    do {
                        syntheticSeq++;
                        id = "PRD-" + syntheticSeq;
                    } while (productRepository.existsById(id) || seenIdsInBatch.contains(id));
                } else {
                    id = id.toUpperCase();
                }

                if (productRepository.existsById(id)) {
                    report.recordRejection(row.getLineNumber(), rawLine,
                            "Duplicate product ID '" + id + "': already exists in catalog.");
                    continue;
                }

                if (seenIdsInBatch.contains(id)) {
                    report.recordRejection(row.getLineNumber(), rawLine,
                            "Duplicate product ID '" + id + "' detected multiple times in import file.");
                    continue;
                }

                String name = row.getString("name");
                String category = row.getString("category");
                String description = row.hasColumn("description") ? row.getString("description") : "";
                BigDecimal costPrice = row.getBigDecimal("costprice");
                BigDecimal sellingPrice = row.getBigDecimal("sellingprice");
                int quantity = row.getInt("quantity");

                ProductStatus status = ProductStatus.ACTIVE;
                if (row.hasColumn("status") && !row.getString("status").isEmpty()) {
                    try {
                        status = ProductStatus.fromString(row.getString("status"));
                    } catch (IllegalArgumentException e) {
                        report.recordRejection(row.getLineNumber(), rawLine, "Invalid status: " + row.getString("status"));
                        continue;
                    }
                }

                LocalDateTime createdAt = row.hasColumn("createdat") ? row.getLocalDateTime("createdat") : LocalDateTime.now();
                LocalDateTime updatedAt = row.hasColumn("updatedat") ? row.getLocalDateTime("updatedat") : createdAt;

                Product candidate = new Product(id, name, category, description,
                        costPrice, sellingPrice, quantity, status, createdAt, updatedAt);

                ValidationResult validation = ProductValidator.validate(candidate);
                if (validation.hasErrors()) {
                    report.recordRejection(row.getLineNumber(), rawLine, validation.getErrors().get(0));
                    continue;
                }

                seenIdsInBatch.add(id);
                report.recordValid(candidate);

            } catch (NumberFormatException e) {
                report.recordRejection(row.getLineNumber(), rawLine, "Numeric parsing error: " + e.getMessage());
            } catch (Exception e) {
                report.recordRejection(row.getLineNumber(), rawLine, "Format error: " + e.getMessage());
            }
        }

        return report;
    }

    /**
     * Persists valid products from an import report atomically.
     */
    public synchronized int applyImport(ImportReport report) {
        Objects.requireNonNull(report, "ImportReport cannot be null");
        return transactionManager.inTransaction(() -> {
            int savedCount = 0;
            for (Product product : report.getValidProducts()) {
                productRepository.save(product);
                idGenerator.observeId(product.getProductId());
                savedCount++;
            }
            if (savedCount > 0 && activityRepository != null) {
                activityRepository.record("CSV_IMPORTED", "SYSTEM", "CSV",
                        "Imported " + savedCount + " product(s) from CSV batch");
            }
            return savedCount;
        });
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
