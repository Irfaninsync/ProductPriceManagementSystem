// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves persistent, OS-appropriate local storage directories outside temporary
 * and replaceable application installation folders.
 */
public final class AppPaths {

    private final Path dataDir;
    private final Path databaseFile;
    private final Path backupsDir;
    private final Path exportsDir;
    private final Path logsDir;
    private final Path lockFile;
    private final Path portFile;

    private AppPaths(Path dataDir) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.databaseFile = this.dataDir.resolve("ppms.db");
        this.backupsDir = this.dataDir.resolve("backups");
        this.exportsDir = this.dataDir.resolve("exports");
        this.logsDir = this.dataDir.resolve("logs");
        this.lockFile = this.dataDir.resolve("ppms.lock");
        this.portFile = this.dataDir.resolve("ppms.port");
    }

    /**
     * Resolves the persistent application data directory:
     * 1. Explicit JVM property (-Dppms.data.dir=...)
     * 2. Explicit environment variable (PPMS_DATA_DIR)
     * 3. Windows %LOCALAPPDATA%\PPMS (or ~/.local/share/PPMS on Unix/macOS)
     */
    public static AppPaths resolveDefault() {
        String prop = System.getProperty("ppms.data.dir");
        if (prop != null && !prop.trim().isEmpty()) {
            return new AppPaths(Paths.get(prop.trim()));
        }

        String envDir = System.getenv("PPMS_DATA_DIR");
        if (envDir != null && !envDir.trim().isEmpty()) {
            return new AppPaths(Paths.get(envDir.trim()));
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.trim().isEmpty()) {
                return new AppPaths(Paths.get(localAppData.trim(), "PPMS"));
            }
            return new AppPaths(Paths.get(System.getProperty("user.home"), "AppData", "Local", "PPMS"));
        }
        if (os.contains("mac")) {
            return new AppPaths(Paths.get(System.getProperty("user.home"), "Library", "Application Support", "PPMS"));
        }
        String xdgData = System.getenv("XDG_DATA_HOME");
        if (xdgData != null && !xdgData.trim().isEmpty()) {
            return new AppPaths(Paths.get(xdgData.trim(), "ppms"));
        }
        return new AppPaths(Paths.get(System.getProperty("user.home"), ".local", "share", "ppms"));
    }

    public static AppPaths forDirectory(Path directory) {
        return new AppPaths(directory);
    }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(dataDir);
        Files.createDirectories(backupsDir);
        Files.createDirectories(exportsDir);
        Files.createDirectories(logsDir);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public Path getBackupsDir() {
        return backupsDir;
    }

    public Path getExportsDir() {
        return exportsDir;
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public Path getLockFile() {
        return lockFile;
    }

    public Path getPortFile() {
        return portFile;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
