// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Metadata representing a local SQLite database backup file.
 */
public class BackupSnapshot implements Comparable<BackupSnapshot> {

    private final String fileName;
    private final Path path;
    private final LocalDateTime createdAt;
    private final long sizeBytes;
    private final String note;

    public BackupSnapshot(String fileName, Path path, LocalDateTime createdAt, long sizeBytes, String note) {
        this.fileName = fileName;
        this.path = path;
        this.createdAt = createdAt;
        this.sizeBytes = sizeBytes;
        this.note = note != null ? note : "";
    }

    public String getFileName() { return fileName; }
    public Path getPath() { return path; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getSizeBytes() { return sizeBytes; }
    public String getNote() { return note; }

    @Override
    public int compareTo(BackupSnapshot other) {
        if (other == null) return -1;
        return other.createdAt.compareTo(this.createdAt);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
