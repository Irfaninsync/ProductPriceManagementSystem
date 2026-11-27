// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.db;

import com.ppms.model.Product;
import com.ppms.repository.TransactionManager;
import com.ppms.util.DateTimeUtil;
import com.ppms.util.IdGenerator;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe SQLite connection, schema migration, transaction, and lifecycle manager.
 * Uses a single persistent WAL-mode connection guarded by a reentrant lock so multi-step
 * transactions remain atomic and Windows file handles release cleanly during restore/shutdown.
 */
public class SqliteDatabaseManager implements TransactionManager, AutoCloseable {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern NUMBER_EXTRACTOR = Pattern.compile("(\\d+)$");

    private final Path databaseFile;
    private final ReentrantLock lock = new ReentrantLock(true);

    private Connection connection;
    private int transactionDepth = 0;
    private boolean rollbackOnly = false;

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlVoidWork {
        void execute(Connection conn) throws SQLException;
    }

    public SqliteDatabaseManager(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath().normalize();
        open();
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    public void open() {
        lock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            if (databaseFile.getParent() != null) {
                Files.createDirectories(databaseFile.getParent());
            }
            String jdbcUrl = "jdbc:sqlite:" + databaseFile;
            Connection conn = DriverManager.getConnection(jdbcUrl);
            configurePragmas(conn);
            migrateSchema(conn);
            this.connection = conn;
            this.transactionDepth = 0;
            this.rollbackOnly = false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite database at " + databaseFile + ": " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void configurePragmas(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
    }

    private void migrateSchema(Connection conn) throws SQLException {
        boolean prevAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS schema_migrations (
                            version INTEGER PRIMARY KEY,
                            description TEXT NOT NULL,
                            applied_at TEXT NOT NULL
                        );
                        """);
            }

            int version = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
                if (rs.next()) {
                    version = rs.getInt(1);
                }
            }

            if (version < 1) {
                applyV1Schema(conn);
                recordMigration(conn, 1, "Initial PPMS enterprise schema (categories, products, price_history, activity_log, app_settings)");
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
            throw e;
        } finally {
            try {
                conn.setAutoCommit(prevAutoCommit);
            } catch (SQLException ignored) {}
        }
    }

    private void applyV1Schema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        category_id TEXT PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        description TEXT NOT NULL DEFAULT '',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    );
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        product_id TEXT PRIMARY KEY COLLATE NOCASE,
                        name TEXT NOT NULL,
                        category_id TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        cost_price_minor INTEGER NOT NULL CHECK (cost_price_minor >= 0),
                        selling_price_minor INTEGER NOT NULL CHECK (selling_price_minor >= 0),
                        quantity INTEGER NOT NULL CHECK (quantity >= 0),
                        status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (category_id) REFERENCES categories(category_id)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    );
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS price_history (
                        history_id TEXT PRIMARY KEY COLLATE NOCASE,
                        product_id TEXT NOT NULL,
                        old_price_minor INTEGER NOT NULL CHECK (old_price_minor >= 0),
                        new_price_minor INTEGER NOT NULL CHECK (new_price_minor >= 0),
                        changed_at TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        FOREIGN KEY (product_id) REFERENCES products(product_id)
                            ON DELETE CASCADE
                    );
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS activity_log (
                        activity_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_type TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    );
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        setting_key TEXT PRIMARY KEY,
                        setting_value TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    );
                    """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_price ON products(selling_price_minor);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_name ON products(name COLLATE NOCASE);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_product ON price_history(product_id, changed_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_changed_at ON price_history(changed_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_activity_created_at ON activity_log(created_at DESC);");
        }
    }

    private void recordMigration(Connection conn, int version, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO schema_migrations (version, description, applied_at) VALUES (?, ?, ?)")) {
            ps.setInt(1, version);
            ps.setString(2, description);
            ps.setString(3, DateTimeUtil.formatIso(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public <T> T query(SqlWork<T> work) {
        lock.lock();
        try {
            ensureOpen();
            return work.execute(connection);
        } catch (SQLException e) {
            if (transactionDepth > 0) {
                rollbackOnly = true;
            }
            throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public void execute(SqlVoidWork work) {
        query(conn -> {
            work.execute(conn);
            return null;
        });
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        lock.lock();
        boolean outermost = false;
        try {
            ensureOpen();
            if (transactionDepth == 0) {
                connection.setAutoCommit(false);
                rollbackOnly = false;
                outermost = true;
            }
            transactionDepth++;

            T result = action.get();

            if (outermost) {
                if (rollbackOnly) {
                    connection.rollback();
                    throw new RuntimeException("Transaction rolled back due to an internal error.");
                }
                connection.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (outermost) {
                rollbackQuietly();
            } else {
                rollbackOnly = true;
            }
            throw e;
        } catch (Exception e) {
            if (outermost) {
                rollbackQuietly();
            } else {
                rollbackOnly = true;
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        } finally {
            if (transactionDepth > 0) {
                transactionDepth--;
            }
            if (outermost) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException ignored) {}
                rollbackOnly = false;
            }
            lock.unlock();
        }
    }

    private void rollbackQuietly() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException ignored) {}
    }

    private void ensureOpen() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is closed.");
        }
    }

    /**
     * Synchronizes IdGenerator counters with existing database primary keys so IDs never collide.
     */
    public void syncIdGenerators(IdGenerator productGen, IdGenerator historyGen, IdGenerator categoryGen) {
        execute(conn -> {
            if (productGen != null) {
                observeColumnMax(conn, "SELECT product_id FROM products", productGen);
            }
            if (historyGen != null) {
                observeColumnMax(conn, "SELECT history_id FROM price_history", historyGen);
            }
            if (categoryGen != null) {
                observeColumnMax(conn, "SELECT category_id FROM categories", categoryGen);
            }
        });
    }

    private void observeColumnMax(Connection conn, String sql, IdGenerator generator) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString(1);
                if (id != null) {
                    Matcher m = NUMBER_EXTRACTOR.matcher(id.trim());
                    if (m.find()) {
                        generator.observeId(id);
                    }
                }
            }
        }
    }

    public void createSnapshotBackup(Path destinationFile) {
        lock.lock();
        try {
            ensureOpen();
            if (destinationFile.getParent() != null) {
                Files.createDirectories(destinationFile.getParent());
            }
            Files.deleteIfExists(destinationFile);
            try (PreparedStatement ps = connection.prepareStatement("VACUUM INTO ?")) {
                ps.setString(1, destinationFile.toAbsolutePath().normalize().toString());
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SQLite snapshot backup: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validates a candidate SQLite backup file in read-only mode before restoring.
     */
    public void verifyBackupFile(Path backupFile) {
        if (backupFile == null || !Files.isRegularFile(backupFile)) {
            throw new IllegalArgumentException("Backup file does not exist.");
        }
        String url = "jdbc:sqlite:" + backupFile.toAbsolutePath().normalize();
        try (Connection checkConn = DriverManager.getConnection(url);
             Statement stmt = checkConn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("PRAGMA integrity_check;")) {
                if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) {
                    throw new IllegalStateException("Backup failed SQLite integrity verification.");
                }
            }
            Set<String> tables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }
            if (!tables.contains("categories") || !tables.contains("products") || !tables.contains("price_history")) {
                throw new IllegalStateException("Selected file is not a valid PPMS database backup (missing core tables).");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Invalid or corrupted backup database: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically replaces the live database with a verified backup while holding the DB lock.
     */
    public void restoreFromVerifiedBackup(Path verifiedBackupFile, Path safetyBackupTarget) {
        lock.lock();
        try {
            verifyBackupFile(verifiedBackupFile);

            // Create safety backup of current live state before touching live files
            if (connection != null && !connection.isClosed() && Files.exists(databaseFile)) {
                createSnapshotBackup(safetyBackupTarget);
            }

            closeConnectionInternal();
            deleteWalSidecars();

            Path staged = databaseFile.resolveSibling(databaseFile.getFileName().toString() + ".restore.tmp");
            Files.copy(verifiedBackupFile, staged, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(staged, databaseFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(staged, databaseFile, StandardCopyOption.REPLACE_EXISTING);
            }

            open();
        } catch (RuntimeException e) {
            reopenSafe();
            throw e;
        } catch (Exception e) {
            reopenSafe();
            throw new RuntimeException("Failed to restore database from backup: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void reopenSafe() {
        try {
            if (connection == null || connection.isClosed()) {
                open();
            }
        } catch (Exception ignored) {}
    }

    private void deleteWalSidecars() {
        try {
            Files.deleteIfExists(databaseFile.resolveSibling(databaseFile.getFileName().toString() + "-wal"));
            Files.deleteIfExists(databaseFile.resolveSibling(databaseFile.getFileName().toString() + "-shm"));
        } catch (Exception ignored) {}
    }

    private void closeConnectionInternal() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);");
                    } catch (SQLException ignored) {}
                    connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }

    public boolean isHealthy() {
        lock.lock();
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    public int getSchemaVersion() {
        return query(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public long getDatabaseSizeBytes() {
        try {
            return Files.exists(databaseFile) ? Files.size(databaseFile) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static long toMinorUnits(BigDecimal amount) {
        BigDecimal normalized = Product.normalizePrice(amount);
        try {
            return normalized.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Monetary amount exceeds supported precision range: " + normalized, e);
        }
    }

    public static BigDecimal fromMinorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2);
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closeConnectionInternal();
        } finally {
            lock.unlock();
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
