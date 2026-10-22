package com.ppms.persistence;

import com.ppms.model.Product;
import com.ppms.model.ProductStatus;
import com.ppms.repository.ProductRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CSV-backed persistent implementation of ProductRepository.
 * Maintains an in-memory synchronized cache and safely flushes modifications to disk.
 */
public class CsvProductRepository implements ProductRepository {

    public static final List<String> HEADERS = List.of(
            "productId", "name", "category", "description",
            "costPrice", "sellingPrice", "quantity", "status",
            "createdAt", "updatedAt"
    );

    private final Path filePath;
    private final IdGenerator idGenerator;
    private final Map<String, Product> productCache = new LinkedHashMap<>();

    public CsvProductRepository(Path filePath, IdGenerator idGenerator) {
        this.filePath = filePath;
        this.idGenerator = idGenerator;
        reload();
    }

    @Override
    public synchronized Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Cannot save null product");
        }
        productCache.put(product.getProductId().toUpperCase(), new Product(product));
        if (idGenerator != null) {
            idGenerator.observeId(product.getProductId());
        }
        flushToDisk();
        return new Product(product);
    }

    @Override
    public synchronized Optional<Product> findById(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Optional.empty();
        }
        Product product = productCache.get(productId.trim().toUpperCase());
        return product != null ? Optional.of(new Product(product)) : Optional.empty();
    }

    @Override
    public synchronized boolean existsById(String productId) {
        if (productId == null) return false;
        return productCache.containsKey(productId.trim().toUpperCase());
    }

    @Override
    public synchronized List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        for (Product p : productCache.values()) {
            list.add(new Product(p));
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public synchronized List<Product> findActive() {
        List<Product> activeList = new ArrayList<>();
        for (Product p : productCache.values()) {
            if (p.isActive()) {
                activeList.add(new Product(p));
            }
        }
        return Collections.unmodifiableList(activeList);
    }

    @Override
    public synchronized List<Product> findArchived() {
        List<Product> archivedList = new ArrayList<>();
        for (Product p : productCache.values()) {
            if (p.isArchived()) {
                archivedList.add(new Product(p));
            }
        }
        return Collections.unmodifiableList(archivedList);
    }

    @Override
    public synchronized int count() {
        return productCache.size();
    }

    @Override
    public synchronized void reload() {
        productCache.clear();
        try {
            List<CsvRow> rows = CsvFileHandler.readRows(filePath, HEADERS);
            for (CsvRow row : rows) {
                try {
                    String id = row.getString("productId");
                    if (id.isEmpty()) {
                        System.err.println("[CsvProductRepository] Warning: Missing productId at line " + row.getLineNumber());
                        continue;
                    }

                    String name = row.getString("name");
                    String category = row.getString("category");
                    String description = row.getString("description");
                    BigDecimal costPrice = row.getBigDecimal("costPrice");
                    BigDecimal sellingPrice = row.getBigDecimal("sellingPrice");
                    int quantity = row.getInt("quantity");
                    ProductStatus status = ProductStatus.fromString(row.getString("status"));
                    LocalDateTime createdAt = row.getLocalDateTime("createdAt");
                    LocalDateTime updatedAt = row.getLocalDateTime("updatedAt");

                    Product product = new Product(id, name, category, description,
                            costPrice, sellingPrice, quantity, status, createdAt, updatedAt);

                    productCache.put(id.toUpperCase(), product);
                    if (idGenerator != null) {
                        idGenerator.observeId(id);
                    }
                } catch (Exception e) {
                    System.err.println("[CsvProductRepository] Warning: Skipping malformed product record at line "
                            + row.getLineNumber() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[CsvProductRepository] Error loading products file: " + e.getMessage());
        }
    }

    private void flushToDisk() {
        List<List<String>> records = new ArrayList<>();
        records.add(HEADERS);

        for (Product p : productCache.values()) {
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

        try {
            CsvFileHandler.writeRowsSafely(filePath, records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist products to disk: " + e.getMessage(), e);
        }
    }
}
