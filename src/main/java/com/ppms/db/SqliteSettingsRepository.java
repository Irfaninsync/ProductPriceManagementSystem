// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.repository.SettingsRepository;
import com.ppms.util.DateTimeUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SQLite-backed repository for persistent application configuration and preferences.
 */
public class SqliteSettingsRepository implements SettingsRepository {

    private final SqliteDatabaseManager db;

    public SqliteSettingsRepository(SqliteDatabaseManager db) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
    }

    @Override
    public String get(String key, String defaultValue) {
        if (key == null || key.trim().isEmpty()) {
            return defaultValue;
        }
        return db.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key = ?")) {
                ps.setString(1, key.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String val = rs.getString(1);
                        return val != null ? val : defaultValue;
                    }
                    return defaultValue;
                }
            }
        });
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String raw = get(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String raw = get(key, null);
        if (raw == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim());
    }

    @Override
    public void set(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting key cannot be empty.");
        }
        db.execute(conn -> {
            String sql = """
                    INSERT INTO app_settings (setting_key, setting_value, updated_at)
                    VALUES (?, ?, ?)
                    ON CONFLICT(setting_key) DO UPDATE SET
                        setting_value = excluded.setting_value,
                        updated_at = excluded.updated_at
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key.trim());
                ps.setString(2, value != null ? value : "");
                ps.setString(3, DateTimeUtil.formatIso(LocalDateTime.now()));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public Map<String, String> getAll() {
        return db.query(conn -> {
            Map<String, String> map = new LinkedHashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT setting_key, setting_value FROM app_settings ORDER BY setting_key ASC")) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getString(2));
                }
            }
            return Collections.unmodifiableMap(map);
        });
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
