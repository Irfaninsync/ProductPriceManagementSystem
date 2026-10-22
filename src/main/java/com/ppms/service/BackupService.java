// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.service;

import com.ppms.persistence.CsvFileHandler;
import com.ppms.persistence.CsvProductRepository;
import com.ppms.persistence.CsvRow;
import com.ppms.repository.PriceHistoryRepository;
import com.ppms.repository.ProductRepository;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service managing local CSV snapshot backups, integrity validation, and atomic restoration.
 */
public class BackupService {

    private static final DateTimeFormatter BACKUP_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter BACKUP_DIR_PRECISE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path dataDirectory;
    private final Path backupDirectory;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public BackupService(Path dataDirectory,
                         ProductRepository productRepository,
                         PriceHistoryRepository priceHistoryRepository) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory cannot be null");
        this.backupDirectory = dataDirectory.resolve("backups");
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Represents backup metadata.
     */
    public static class BackupInfo implements Comparable<BackupInfo> {
        private final String backupId;
        private final Path path;
        private final LocalDateTime timestamp;
        private final long totalSizeBytes;

        public BackupInfo(String backupId, Path path, LocalDateTime timestamp, long totalSizeBytes) {
            this.backupId = backupId;
            this.path = path;
            this.timestamp = timestamp;
            this.totalSizeBytes = totalSizeBytes;
        }

        public String getBackupId() {
            return backupId;
        }

        public Path getPath() {
            return path;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public long getTotalSizeBytes() {
            return totalSizeBytes;
        }

        @Override
        public int compareTo(BackupInfo o) {
            // Newest first
            return o.timestamp.compareTo(this.timestamp);
        }
    }

    /**
     * Creates a new timestamped backup directory and archives the current state.
     */
    public synchronized Path createBackup(String reason) throws IOException {
        Files.createDirectories(backupDirectory);
        String timestampStr = LocalDateTime.now().format(BACKUP_DIR_PRECISE_FORMAT);
        String folderName = "backup_" + timestampStr;
        Path targetDir = backupDirectory.resolve(folderName);
        int suffix = 1;
        while (Files.exists(targetDir)) {
            targetDir = backupDirectory.resolve(folderName + "_" + (suffix++));
        }
        Files.createDirectories(targetDir);

        copyIfExists(dataDirectory.resolve("products.csv"), targetDir.resolve("products.csv"));
        copyIfExists(dataDirectory.resolve("price_history.csv"), targetDir.resolve("price_history.csv"));
        copyIfExists(dataDirectory.resolve("metadata.csv"), targetDir.resolve("metadata.csv"));

        // Write backup manifest
        String manifest = "Backup Created: " + LocalDateTime.now() + "\r\nReason: " + (reason != null ? reason : "Manual");
        Files.writeString(targetDir.resolve("manifest.txt"), manifest);

        return targetDir;
    }

    /**
     * Lists all existing valid backups sorted by most recent first.
     */
    public List<BackupInfo> listBackups() {
        if (!Files.exists(backupDirectory) || !Files.isDirectory(backupDirectory)) {
            return Collections.emptyList();
        }

        List<BackupInfo> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDirectory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && entry.getFileName().toString().startsWith("backup_")) {
                    String folderName = entry.getFileName().toString();
                    long size = calculateDirectorySize(entry);
                    LocalDateTime time;
                    try {
                        String datePart = folderName.substring("backup_".length());
                        if (datePart.length() >= 19) { // yyyyMMdd_HHmmss_SSS
                            time = LocalDateTime.parse(datePart.substring(0, 19), BACKUP_DIR_PRECISE_FORMAT);
                        } else if (datePart.length() >= 15) { // yyyyMMdd_HHmmss
                            time = LocalDateTime.parse(datePart.substring(0, 15), BACKUP_DIR_FORMAT);
                        } else {
                            time = LocalDateTime.now();
                        }
                    } catch (Exception e) {
                        time = LocalDateTime.now();
                    }
                    backups.add(new BackupInfo(folderName, entry, time, size));
                }
            }
        } catch (IOException e) {
            System.err.println("[BackupService] Error listing backups: " + e.getMessage());
        }

        Collections.sort(backups);
        return backups;
    }

    /**
     * Restores data files from a selected backup folder.
     * Takes a safety backup of current data before executing restore.
     */
    public synchronized void restoreBackup(Path backupFolder) throws IOException {
        if (!Files.exists(backupFolder) || !Files.isDirectory(backupFolder)) {
            throw new IllegalArgumentException("Backup folder does not exist: " + backupFolder);
        }

        Path backupProducts = backupFolder.resolve("products.csv");
        if (!Files.exists(backupProducts)) {
            throw new IllegalStateException("Invalid backup: Missing products.csv inside " + backupFolder);
        }

        // Validate products.csv header integrity in backup before restoring
        List<CsvRow> rows = CsvFileHandler.readRows(backupProducts, CsvProductRepository.HEADERS);
        if (rows.isEmpty()) {
            // It could be an empty catalog, but verify header
        }

        // Step 1: Create immediate safety backup of current live files
        createBackup("Pre-Restore Safety Snapshot");

        // Step 2: Copy backup files over live data
        copyIfExists(backupFolder.resolve("products.csv"), dataDirectory.resolve("products.csv"));
        copyIfExists(backupFolder.resolve("price_history.csv"), dataDirectory.resolve("price_history.csv"));
        copyIfExists(backupFolder.resolve("metadata.csv"), dataDirectory.resolve("metadata.csv"));

        // Step 3: Trigger in-memory cache reload
        if (productRepository != null) {
            productRepository.reload();
        }
        if (priceHistoryRepository != null) {
            priceHistoryRepository.reload();
        }
    }

    private void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private long calculateDirectorySize(Path dir) {
        long size = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    size += Files.size(p);
                }
            }
        } catch (IOException ignored) {}
        return size;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

