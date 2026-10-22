package com.ppms.service;

import com.ppms.model.ImportReport;
import com.ppms.model.PriceHistory;
import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.persistence.CsvFileHandler;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.CsvRow;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import com.ppms.validation.ProductValidator;
import com.ppms.validation.ValidationResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service managing CSV export generation and robust, validated CSV imports.
 */
public class ImportExportService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IdGenerator idGenerator;

    public ImportExportService(ProductRepository productRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               IdGenerator idGenerator) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.priceHistoryRepository = priceHistoryRepository;
        this.idGenerator = Objects.requireNonNull(idGenerator, "IdGenerator cannot be null");
    }

    /**
     * Exports a list of products to the destination path.
     */
    public void exportProducts(List<Product> products, Path destinationPath) throws IOException {
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

        CsvFileHandler.writeRowsSafely(destinationPath, records);
    }

    /**
     * Exports price history to the destination path.
     */
    public void exportPriceHistory(List<PriceHistory> historyList, Path destinationPath) throws IOException {
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

        CsvFileHandler.writeRowsSafely(destinationPath, records);
    }

    /**
     * Analyzes and validates a CSV file for import without committing changes.
     */
    public ImportReport previewImport(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            throw new IOException("Import file does not exist: " + csvFile);
        }

        List<CsvRow> rows = CsvFileHandler.readRows(csvFile, null);
        ImportReport report = new ImportReport();
        report.setTotalRowsProcessed(rows.size());

        Set<String> seenIdsInBatch = new HashSet<>();

        for (CsvRow row : rows) {
            String rawLine = String.join(",", row.getRawValues());

            try {
                // Check mandatory column presence
                if (!row.hasColumn("name") || !row.hasColumn("category")
                        || !row.hasColumn("costprice") || !row.hasColumn("sellingprice")
                        || !row.hasColumn("quantity")) {
                    report.recordRejection(row.getLineNumber(), rawLine,
                            "Missing required columns. Header must include: name, category, costPrice, sellingPrice, quantity");
                    continue;
                }

                String id = row.getString("productId");
                if (id.isEmpty()) {
                    id = idGenerator.nextId();
                } else {
                    id = id.toUpperCase();
                }

                // Check duplicate in catalog
                if (productRepository.existsById(id)) {
                    report.recordRejection(row.getLineNumber(), rawLine,
                            "Duplicate product ID '" + id + "': already exists in catalog.");
                    continue;
                }

                // Check duplicate in current import batch
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
     * Persists valid products from an import report.
     */
    public synchronized int applyImport(ImportReport report) {
        Objects.requireNonNull(report, "ImportReport cannot be null");
        int savedCount = 0;
        for (Product product : report.getValidProducts()) {
            productRepository.save(product);
            savedCount++;
        }
        return savedCount;
    }
}
