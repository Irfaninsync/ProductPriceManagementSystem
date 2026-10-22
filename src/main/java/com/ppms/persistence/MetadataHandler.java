package com.ppms.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages system metadata and operational configuration stored in metadata.csv.
 */
public class MetadataHandler {

    private static final List<String> HEADERS = List.of("key", "value");
    private final Path filePath;
    private final Map<String, String> properties = new HashMap<>();

    public MetadataHandler(Path filePath) {
        this.filePath = filePath;
        load();
    }

    public synchronized void load() {
        properties.clear();
        try {
            List<CsvRow> rows = CsvFileHandler.readRows(filePath, HEADERS);
            for (CsvRow row : rows) {
                String key = row.getString("key");
                String value = row.getString("value");
                if (!key.isEmpty()) {
                    properties.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[MetadataHandler] Notice: Could not read metadata file: " + e.getMessage());
        }
    }

    public synchronized String get(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    public synchronized void set(String key, String value) {
        properties.put(key, value);
        save();
    }

    public synchronized int getInt(String key, int defaultValue) {
        String val = properties.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public synchronized void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    private void save() {
        List<List<String>> records = new ArrayList<>();
        records.add(HEADERS);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            records.add(List.of(entry.getKey(), entry.getValue()));
        }
        try {
            CsvFileHandler.writeRowsSafely(filePath, records);
        } catch (IOException e) {
            System.err.println("[MetadataHandler] Warning: Failed to persist metadata: " + e.getMessage());
        }
    }
}
