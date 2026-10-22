package com.ppms.repository;

import com.ppms.model.PriceHistory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface defining price history audit trail operations.
 */
public interface PriceHistoryRepository {

    /**
     * Persists a new price change history audit record.
     */
    PriceHistory save(PriceHistory history);

    /**
     * Retrieves all recorded price history records.
     */
    List<PriceHistory> findAll();

    /**
     * Retrieves all price changes recorded for a specific product.
     */
    List<PriceHistory> findByProductId(String productId);

    /**
     * Retrieves all price changes recorded within a specific timestamp window.
     */
    List<PriceHistory> findByDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Returns total number of price adjustments recorded.
     */
    int count();

    /**
     * Forces a reload from persistent storage.
     */
    void reload();
}
