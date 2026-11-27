// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.app;

import com.ppms.db.SqliteActivityRepository;
import com.ppms.db.SqliteBackupService;
import com.ppms.db.SqliteCategoryRepository;
import com.ppms.db.SqliteDatabaseManager;
import com.ppms.db.SqlitePriceHistoryRepository;
import com.ppms.db.SqliteProductRepository;
import com.ppms.db.SqliteSettingsRepository;
import com.ppms.server.CategoryHandler;
import com.ppms.server.DashboardHandler;
import com.ppms.server.DataAndSettingsHandler;
import com.ppms.server.LocalHttpServer;
import com.ppms.server.PricingHandler;
import com.ppms.server.ProductHandler;
import com.ppms.server.ReportsHandler;
import com.ppms.service.AnalyticsService;
import com.ppms.service.CategoryService;
import com.ppms.service.ImportExportService;
import com.ppms.service.PricingService;
import com.ppms.service.ProductService;
import com.ppms.util.CurrencyFormatter;
import com.ppms.util.IdGenerator;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local-first desktop application host.
 * Enforces single-instance locking, initializes SQLite storage, starts the loopback
 * HTTP server, opens the default browser, and manages clean shutdown.
 */
public class DesktopApplication {

    private final AppPaths paths;
    private final boolean headless;
    private final int preferredPort;

    private FileChannel lockChannel;
    private FileLock instanceLock;
    private SqliteDatabaseManager databaseManager;
    private LocalHttpServer httpServer;
    private TrayIcon trayIcon;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public DesktopApplication() {
        this(AppPaths.resolveDefault(), false, LocalHttpServer.PREFERRED_PORT);
    }

    public DesktopApplication(AppPaths paths, boolean headless, int preferredPort) {
        this.paths = paths;
        this.headless = headless;
        this.preferredPort = preferredPort;
    }

    public void run() {
        try {
            paths.ensureDirectories();
            if (!acquireSingleInstanceLock()) {
                handleExistingInstance();
                return;
            }

            startServices();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "ppms-jvm-shutdown"));

            if (!headless) {
                setupSystemTray();
                openBrowser(httpServer.getBaseUrl());
            }

            System.out.println("PPMS running locally at " + httpServer.getBaseUrl());
            System.out.println("Local database: " + paths.getDatabaseFile());
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Failed to start PPMS desktop application: " + e.getMessage());
            e.printStackTrace();
            shutdown();
            System.exit(1);
        } finally {
            shutdown();
        }
    }

    /**
     * Starts server and services (also directly usable by automated integration tests).
     */
    public void startServices() throws IOException {
        paths.ensureDirectories();

        IdGenerator productGen = new IdGenerator("PRD-", 1000);
        IdGenerator historyGen = new IdGenerator("HST-", 1000);
        IdGenerator categoryGen = new IdGenerator("CAT-", 1000);

        databaseManager = new SqliteDatabaseManager(paths.getDatabaseFile());
        SqliteCategoryRepository categoryRepo = new SqliteCategoryRepository(databaseManager, categoryGen);
        SqliteProductRepository productRepo = new SqliteProductRepository(databaseManager, categoryRepo, productGen);
        SqlitePriceHistoryRepository historyRepo = new SqlitePriceHistoryRepository(databaseManager, historyGen);
        SqliteActivityRepository activityRepo = new SqliteActivityRepository(databaseManager);
        SqliteSettingsRepository settingsRepo = new SqliteSettingsRepository(databaseManager);

        databaseManager.syncIdGenerators(productGen, historyGen, categoryGen);

        String savedCurrency = settingsRepo.get("currency_symbol", "\u20B9");
        CurrencyFormatter.setCurrencySymbol(savedCurrency);

        ProductService productService = new ProductService(productRepo, productGen, databaseManager, activityRepo);
        CategoryService categoryService = new CategoryService(categoryRepo, productRepo, categoryGen, databaseManager, activityRepo);
        PricingService pricingService = new PricingService(productRepo, historyRepo, historyGen, databaseManager, activityRepo);
        AnalyticsService analyticsService = new AnalyticsService(productRepo, historyRepo);
        ImportExportService importExportService = new ImportExportService(productRepo, historyRepo, productGen, databaseManager, activityRepo);
        SqliteBackupService backupService = new SqliteBackupService(
                databaseManager, paths.getBackupsDir(), activityRepo, settingsRepo, productGen, historyGen, categoryGen);

        backupService.ensureStartupBackupIfNeeded(productRepo.count() > 0);

        DashboardHandler dashboardHandler = new DashboardHandler(
                productService, categoryService, analyticsService, activityRepo, settingsRepo);
        ProductHandler productHandler = new ProductHandler(
                productService, pricingService, settingsRepo);
        CategoryHandler categoryHandler = new CategoryHandler(categoryService);
        PricingHandler pricingHandler = new PricingHandler(
                pricingService, productService, backupService, settingsRepo);
        ReportsHandler reportsHandler = new ReportsHandler(
                productService, categoryService, pricingService, analyticsService, importExportService, settingsRepo);
        DataAndSettingsHandler dataAndSettingsHandler = new DataAndSettingsHandler(
                paths, databaseManager, settingsRepo, activityRepo, backupService,
                importExportService, productService, productRepo, historyRepo, historyGen, this::shutdown);

        httpServer = new LocalHttpServer(
                preferredPort,
                dashboardHandler,
                productHandler,
                categoryHandler,
                pricingHandler,
                reportsHandler,
                dataAndSettingsHandler
        );
        httpServer.start();
        Files.writeString(paths.getPortFile(), String.valueOf(httpServer.getPort()));
    }

    public LocalHttpServer getHttpServer() {
        return httpServer;
    }

    public SqliteDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        try {
            if (trayIcon != null && SystemTray.isSupported()) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
        } catch (Exception ignored) {}

        try {
            if (httpServer != null) {
                httpServer.close();
            }
        } catch (Exception ignored) {}

        try {
            if (databaseManager != null) {
                databaseManager.close();
            }
        } catch (Exception ignored) {}

        try {
            Files.deleteIfExists(paths.getPortFile());
        } catch (Exception ignored) {}

        try {
            if (instanceLock != null && instanceLock.isValid()) {
                instanceLock.release();
            }
            if (lockChannel != null) {
                lockChannel.close();
            }
        } catch (Exception ignored) {}

        shutdownLatch.countDown();
    }

    private boolean acquireSingleInstanceLock() {
        try {
            lockChannel = FileChannel.open(paths.getLockFile(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            instanceLock = lockChannel.tryLock();
            return instanceLock != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleExistingInstance() {
        try {
            Path portFile = paths.getPortFile();
            if (Files.isRegularFile(portFile)) {
                String raw = Files.readString(portFile).trim();
                int existingPort = Integer.parseInt(raw);
                String url = "http://127.0.0.1:" + existingPort + "/";
                System.out.println("PPMS is already running at " + url);
                if (!headless) {
                    openBrowser(url);
                }
                return;
            }
        } catch (Exception ignored) {}
        System.out.println("Another instance of PPMS is already running.");
    }

    private void setupSystemTray() {
        try {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                return;
            }
            SystemTray tray = SystemTray.getSystemTray();
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(new java.awt.Color(0x71, 0x3B, 0x43));
            g.fillRoundRect(0, 0, 16, 16, 4, 4);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
            g.drawString("P", 4, 12);
            g.dispose();

            PopupMenu menu = new PopupMenu();
            MenuItem openItem = new MenuItem("Open PPMS Dashboard");
            openItem.addActionListener(e -> openBrowser(httpServer.getBaseUrl()));
            MenuItem exitItem = new MenuItem("Quit PPMS");
            exitItem.addActionListener(e -> shutdown());
            menu.add(openItem);
            menu.addSeparator();
            menu.add(exitItem);

            TrayIcon icon = new TrayIcon(img, "PPMS — Product Price Management System", menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(e -> openBrowser(httpServer.getBaseUrl()));
            tray.add(icon);
            this.trayIcon = icon;
        } catch (Exception ignored) {}
    }

    private void openBrowser(String url) {
        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {}

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ignored) {}
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
