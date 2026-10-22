// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.persistence;

import com.ppms.model.PriceHistory;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CSV-backed persistent implementation of PriceHistoryRepository.
 * Maintains historical price audit records and reliably flushes them to disk.
 */
public class CsvPriceHistoryRepository implements PriceHistoryRepository {

    public static final List<String> HEADERS = List.of(
            "historyId", "productId", "oldPrice", "newPrice", "changedAt", "reason"
    );

    private final Path filePath;
    private final IdGenerator idGenerator;
    private final List<PriceHistory> historyRecords = new ArrayList<>();

    public CsvPriceHistoryRepository(Path filePath, IdGenerator idGenerator) {
        this.filePath = filePath;
        this.idGenerator = idGenerator;
        reload();
    }

    @Override
    public synchronized PriceHistory save(PriceHistory history) {
        Objects.requireNonNull(history, "PriceHistory cannot be null");
        historyRecords.add(history);
        if (idGenerator != null) {
            idGenerator.observeId(history.getHistoryId());
        }
        flushToDisk();
        return history;
    }

    @Override
    public synchronized List<PriceHistory> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(historyRecords));
    }

    @Override
    public synchronized List<PriceHistory> findByProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanId = productId.trim();
        return historyRecords.stream()
                .filter(h -> h.getProductId().equalsIgnoreCase(cleanId))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public synchronized List<PriceHistory> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return historyRecords.stream()
                .filter(h -> {
                    boolean afterStart = (start == null) || !h.getChangedAt().isBefore(start);
                    boolean beforeEnd = (end == null) || !h.getChangedAt().isAfter(end);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public synchronized int count() {
        return historyRecords.size();
    }

    @Override
    public synchronized void reload() {
        historyRecords.clear();
        try {
            List<CsvRow> rows = CsvFileHandler.readRows(filePath, HEADERS);
            for (CsvRow row : rows) {
                try {
                    String historyId = row.getString("historyId");
                    String productId = row.getString("productId");
                    BigDecimal oldPrice = row.getBigDecimal("oldPrice");
                    BigDecimal newPrice = row.getBigDecimal("newPrice");
                    LocalDateTime changedAt = row.getLocalDateTime("changedAt");
                    String reason = row.getString("reason");

                    PriceHistory record = new PriceHistory(historyId, productId, oldPrice, newPrice, changedAt, reason);
                    historyRecords.add(record);
                    if (idGenerator != null) {
                        idGenerator.observeId(historyId);
                    }
                } catch (Exception e) {
                    System.err.println("[CsvPriceHistoryRepository] Warning: Skipping malformed history record at line "
                            + row.getLineNumber() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[CsvPriceHistoryRepository] Error loading price history file: " + e.getMessage());
        }
    }

    private void flushToDisk() {
        List<List<String>> records = new ArrayList<>();
        records.add(HEADERS);

        for (PriceHistory h : historyRecords) {
            records.add(List.of(
                    h.getHistoryId(),
                    h.getProductId(),
                    h.getOldPrice().toPlainString(),
                    h.getNewPrice().toPlainString(),
                    DateTimeUtil.formatIso(h.getChangedAt()),
                    h.getReason()
            ));
        }

        try {
            CsvFileHandler.writeRowsSafely(filePath, records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist price history to disk: " + e.getMessage(), e);
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

