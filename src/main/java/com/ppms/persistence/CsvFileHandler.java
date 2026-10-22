package com.ppms.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles persistent reading and atomic, safe writing of CSV files.
 * Protects against partial writes, unexpected power loss, or sudden termination.
 */
public class CsvFileHandler {

    /**
     * Reads all records from a CSV file. If the file does not exist, creates it with default headers.
     */
    public static List<CsvRow> readRows(Path filePath, List<String> defaultHeaders) throws IOException {
        if (!Files.exists(filePath)) {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
                writeRowsSafely(filePath, List.of(defaultHeaders));
            }
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            List<List<String>> rawRecords = CsvParser.parse(reader);
            if (rawRecords.isEmpty()) {
                return new ArrayList<>();
            }

            // Map header names (case-insensitive) to column indices
            List<String> headerLine = rawRecords.get(0);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerLine.size(); i++) {
                headerMap.put(headerLine.get(i).trim().toLowerCase(), i);
            }

            List<CsvRow> rows = new ArrayList<>();
            for (int i = 1; i < rawRecords.size(); i++) {
                List<String> rawRow = rawRecords.get(i);
                // Skip completely blank rows
                boolean allEmpty = true;
                for (String s : rawRow) {
                    if (s != null && !s.trim().isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (!allEmpty) {
                    rows.add(new CsvRow(i + 1, headerMap, rawRow));
                }
            }
            return rows;
        }
    }

    /**
     * Atomically writes CSV rows using a staging temporary file and atomic file replacement.
     */
    public static synchronized void writeRowsSafely(Path filePath, List<List<String>> records) throws IOException {
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        Path tempFile = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");
        Path backupFile = filePath.resolveSibling(filePath.getFileName().toString() + ".bak");

        // Step 1: Write to temporary file
        try (BufferedWriter writer = Files.newBufferedWriter(
                tempFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            CsvWriter.writeAll(writer, records);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            throw new IOException("Failed to write temporary CSV file: " + tempFile + " - " + e.getMessage(), e);
        }

        // Step 2: Backup current file if present
        if (Files.exists(filePath)) {
            try {
                Files.copy(filePath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Warning: backup copy failed, but we still proceed cautiously
                System.err.println("[CsvFileHandler] Notice: Could not create pre-write backup copy: " + e.getMessage());
            }
        }

        // Step 3: Atomic move/rename
        try {
            Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback for filesystems that do not support ATOMIC_MOVE across boundaries
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
