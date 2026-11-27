// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.PriceHistory;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed implementation of PriceHistoryRepository.
 */
public class SqlitePriceHistoryRepository implements PriceHistoryRepository {

    private final SqliteDatabaseManager db;
    private final IdGenerator historyIdGenerator;

    public SqlitePriceHistoryRepository(SqliteDatabaseManager db, IdGenerator historyIdGenerator) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
        this.historyIdGenerator = Objects.requireNonNull(historyIdGenerator, "History IdGenerator cannot be null");
        reload();
    }

    @Override
    public PriceHistory save(PriceHistory history) {
        Objects.requireNonNull(history, "PriceHistory cannot be null");
        db.execute(conn -> {
            String sql = """
                    INSERT INTO price_history (
                        history_id, product_id, old_price_minor, new_price_minor, changed_at, reason
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, history.getHistoryId());
                ps.setString(2, history.getProductId());
                ps.setLong(3, SqliteDatabaseManager.toMinorUnits(history.getOldPrice()));
                ps.setLong(4, SqliteDatabaseManager.toMinorUnits(history.getNewPrice()));
                ps.setString(5, DateTimeUtil.formatIso(history.getChangedAt()));
                ps.setString(6, history.getReason());
                ps.executeUpdate();
            }
        });
        historyIdGenerator.observeId(history.getHistoryId());
        return history;
    }

    @Override
    public List<PriceHistory> findAll() {
        return db.query(conn -> {
            List<PriceHistory> list = new ArrayList<>();
            String sql = "SELECT history_id, product_id, old_price_minor, new_price_minor, changed_at, reason FROM price_history ORDER BY changed_at ASC, history_id ASC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return Collections.unmodifiableList(list);
        });
    }

    @Override
    public List<PriceHistory> findByProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return db.query(conn -> {
            List<PriceHistory> list = new ArrayList<>();
            String sql = "SELECT history_id, product_id, old_price_minor, new_price_minor, changed_at, reason FROM price_history WHERE LOWER(product_id) = LOWER(?) ORDER BY changed_at ASC, history_id ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productId.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        });
    }

    @Override
    public List<PriceHistory> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return db.query(conn -> {
            StringBuilder sql = new StringBuilder(
                    "SELECT history_id, product_id, old_price_minor, new_price_minor, changed_at, reason FROM price_history WHERE 1=1");
            List<String> params = new ArrayList<>();
            if (start != null) {
                sql.append(" AND changed_at >= ?");
                params.add(DateTimeUtil.formatIso(start));
            }
            if (end != null) {
                sql.append(" AND changed_at <= ?");
                params.add(DateTimeUtil.formatIso(end));
            }
            sql.append(" ORDER BY changed_at ASC, history_id ASC");

            List<PriceHistory> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        });
    }

    @Override
    public int count() {
        return db.query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM price_history")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    @Override
    public void reload() {
        db.syncIdGenerators(null, historyIdGenerator, null);
    }

    private PriceHistory mapRow(ResultSet rs) throws SQLException {
        return new PriceHistory(
                rs.getString("history_id"),
                rs.getString("product_id"),
                SqliteDatabaseManager.fromMinorUnits(rs.getLong("old_price_minor")),
                SqliteDatabaseManager.fromMinorUnits(rs.getLong("new_price_minor")),
                DateTimeUtil.parseIso(rs.getString("changed_at")),
                rs.getString("reason")
        );
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
