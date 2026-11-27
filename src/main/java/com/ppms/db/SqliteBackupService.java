// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.BackupSnapshot;
import com.ppms.repository.ActivityRepository;
import com.ppms.repository.SettingsRepository;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Manages SQLite snapshot backups (VACUUM INTO) and integrity-checked atomic restores.
 */
public class SqliteBackupService {

    private static final DateTimeFormatter BACKUP_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final SqliteDatabaseManager db;
    private final Path backupsDir;
    private final ActivityRepository activityRepository;
    private final SettingsRepository settingsRepository;
    private final IdGenerator productGen;
    private final IdGenerator historyGen;
    private final IdGenerator categoryGen;

    public SqliteBackupService(SqliteDatabaseManager db,
                               Path backupsDir,
                               ActivityRepository activityRepository,
                               SettingsRepository settingsRepository,
                               IdGenerator productGen,
                               IdGenerator historyGen,
                               IdGenerator categoryGen) {
        this.db = Objects.requireNonNull(db, "DatabaseManager cannot be null");
        this.backupsDir = Objects.requireNonNull(backupsDir, "Backups directory cannot be null").toAbsolutePath().normalize();
        this.activityRepository = activityRepository;
        this.settingsRepository = settingsRepository;
        this.productGen = productGen;
        this.historyGen = historyGen;
        this.categoryGen = categoryGen;
    }

    public synchronized BackupSnapshot createBackup(String reason) throws IOException {
        Files.createDirectories(backupsDir);
        String ts = LocalDateTime.now().format(BACKUP_TS_FORMAT);
        String baseName = "ppms_backup_" + ts;
        Path target = backupsDir.resolve(baseName + ".db");
        int suffix = 1;
        while (Files.exists(target)) {
            target = backupsDir.resolve(baseName + "_" + (suffix++) + ".db");
        }

        db.createSnapshotBackup(target);

        String noteText = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Manual backup";
        Path metaNote = target.resolveSibling(target.getFileName().toString() + ".note");
        Files.writeString(metaNote, noteText);

        String nowIso = DateTimeUtil.formatIso(LocalDateTime.now());
        if (settingsRepository != null) {
            settingsRepository.set("last_backup_at", nowIso);
        }
        if (activityRepository != null) {
            activityRepository.record("BACKUP_CREATED", "BACKUP", target.getFileName().toString(),
                    "Created database backup " + target.getFileName() + " (" + noteText + ")");
        }

        return toSnapshot(target);
    }

    /**
     * Automatically ensures a daily startup backup once per day if products exist.
     */
    public synchronized void ensureStartupBackupIfNeeded(boolean hasProducts) {
        if (!hasProducts) {
            return;
        }
        boolean autoEnabled = settingsRepository == null || settingsRepository.getBoolean("auto_backup_startup", true);
        if (!autoEnabled) {
            return;
        }
        List<BackupSnapshot> existing = listBackups();
        LocalDateTime now = LocalDateTime.now();
        boolean backedUpToday = existing.stream()
                .anyMatch(b -> b.getCreatedAt().toLocalDate().equals(now.toLocalDate()));
        if (!backedUpToday) {
            try {
                createBackup("Automatic daily startup snapshot");
            } catch (Exception e) {
                System.err.println("[SqliteBackupService] Notice: Automatic startup backup skipped: " + e.getMessage());
            }
        }
    }

    public List<BackupSnapshot> listBackups() {
        if (!Files.exists(backupsDir) || !Files.isDirectory(backupsDir)) {
            return Collections.emptyList();
        }
        List<BackupSnapshot> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "*.db")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    list.add(toSnapshot(entry));
                }
            }
        } catch (IOException e) {
            System.err.println("[SqliteBackupService] Warning listing backups: " + e.getMessage());
        }
        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    public synchronized void restoreBackupByFileName(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup file name is required.");
        }
        Path candidate = resolveSafeBackupPath(fileName.trim());
        restoreFromPath(candidate);
    }

    public synchronized void restoreFromPath(Path backupFile) throws IOException {
        Path normalized = backupFile.toAbsolutePath().normalize();
        db.verifyBackupFile(normalized);

        Files.createDirectories(backupsDir);
        String safetyName = "ppms_pre_restore_" + LocalDateTime.now().format(BACKUP_TS_FORMAT) + ".db";
        Path safetyPath = backupsDir.resolve(safetyName);

        db.restoreFromVerifiedBackup(normalized, safetyPath);
        if (Files.exists(safetyPath)) {
            Files.writeString(safetyPath.resolveSibling(safetyName + ".note"), "Automatic pre-restore safety snapshot");
        }
        db.syncIdGenerators(productGen, historyGen, categoryGen);

        if (activityRepository != null) {
            activityRepository.record("BACKUP_RESTORED", "BACKUP", normalized.getFileName().toString(),
                    "Restored database from backup " + normalized.getFileName());
        }
    }

    public Path resolveSafeBackupPath(String fileName) {
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid backup file name.");
        }
        Path resolved = backupsDir.resolve(fileName).toAbsolutePath().normalize();
        if (!resolved.startsWith(backupsDir)) {
            throw new IllegalArgumentException("Access outside backups directory is prohibited.");
        }
        return resolved;
    }

    public Path getBackupsDir() {
        return backupsDir;
    }

    private BackupSnapshot toSnapshot(Path file) throws IOException {
        String name = file.getFileName().toString();
        long size = Files.size(file);
        LocalDateTime time = parseTimestampFromFilename(name, file);
        String note = "";
        Path noteFile = file.resolveSibling(name + ".note");
        if (Files.isRegularFile(noteFile)) {
            try {
                note = Files.readString(noteFile).trim();
            } catch (IOException ignored) {}
        }
        return new BackupSnapshot(name, file, time, size, note);
    }

    private LocalDateTime parseTimestampFromFilename(String name, Path file) throws IOException {
        String[] prefixes = {"ppms_backup_", "ppms_pre_restore_"};
        for (String prefix : prefixes) {
            if (name.startsWith(prefix) && name.length() >= prefix.length() + 19) {
                try {
                    String part = name.substring(prefix.length(), prefix.length() + 19);
                    return LocalDateTime.parse(part, BACKUP_TS_FORMAT);
                } catch (Exception ignored) {}
            }
        }
        Instant mtime = Files.getLastModifiedTime(file).toInstant();
        return LocalDateTime.ofInstant(mtime, ZoneId.systemDefault());
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
