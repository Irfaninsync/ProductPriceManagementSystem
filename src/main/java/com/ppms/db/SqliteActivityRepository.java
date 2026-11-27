// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.ActivityEntry;
import com.ppms.repository.ActivityRepository;
import com.ppms.util.DateTimeUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SQLite-backed repository for business activity feed events.
 */
public class SqliteActivityRepository implements ActivityRepository {

    private final SqliteDatabaseManager db;

    public SqliteActivityRepository(SqliteDatabaseManager db) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
    }

    @Override
    public void record(String eventType, String entityType, String entityId, String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return;
        }
        db.execute(conn -> {
            String sql = "INSERT INTO activity_log (event_type, entity_type, entity_id, summary, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, eventType != null ? eventType : "INFO");
                ps.setString(2, entityType != null ? entityType : "SYSTEM");
                ps.setString(3, entityId != null ? entityId : "");
                ps.setString(4, summary.trim());
                ps.setString(5, DateTimeUtil.formatIso(LocalDateTime.now()));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<ActivityEntry> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return db.query(conn -> {
            List<ActivityEntry> entries = new ArrayList<>();
            String sql = "SELECT activity_id, event_type, entity_type, entity_id, summary, created_at FROM activity_log ORDER BY activity_id DESC LIMIT ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, safeLimit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new ActivityEntry(
                                rs.getLong("activity_id"),
                                rs.getString("event_type"),
                                rs.getString("entity_type"),
                                rs.getString("entity_id"),
                                rs.getString("summary"),
                                DateTimeUtil.parseIso(rs.getString("created_at"))
                        ));
                    }
                }
            }
            return Collections.unmodifiableList(entries);
        });
    }

    @Override
    public int count() {
        return db.query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM activity_log")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
