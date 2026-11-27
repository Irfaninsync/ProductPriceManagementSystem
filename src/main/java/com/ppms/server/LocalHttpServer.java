// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.server;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight embedded JDK HTTP server strictly bound to 127.0.0.1 loopback interface.
 */
public class LocalHttpServer implements AutoCloseable {

    public static final int PREFERRED_PORT = 18420;

    private final HttpServer server;
    private final ExecutorService executor;
    private final int port;
    private final String sessionToken;

    public LocalHttpServer(int preferredPort,
                           DashboardHandler dashboardHandler,
                           ProductHandler productHandler,
                           CategoryHandler categoryHandler,
                           PricingHandler pricingHandler,
                           ReportsHandler reportsHandler,
                           DataAndSettingsHandler dataAndSettingsHandler) throws IOException {
        this.sessionToken = generateSessionToken();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");

        HttpServer created;
        try {
            created = HttpServer.create(new InetSocketAddress(loopback, preferredPort), 0);
        } catch (IOException busyEx) {
            if (preferredPort == 0) {
                throw busyEx;
            }
            created = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        }
        this.server = created;
        this.port = server.getAddress().getPort();
        this.executor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "ppms-http-worker");
            t.setDaemon(true);
            return t;
        });

        ApiRouter apiRouter = new ApiRouter(
                sessionToken,
                dashboardHandler,
                productHandler,
                categoryHandler,
                pricingHandler,
                reportsHandler,
                dataAndSettingsHandler
        );
        StaticAssetHandler staticHandler = new StaticAssetHandler(sessionToken);

        server.createContext("/api/", apiRouter);
        server.createContext("/", staticHandler);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    public String getSessionToken() {
        return sessionToken;
    }

    @Override
    public void close() {
        server.stop(1);
        executor.shutdownNow();
    }

    private static String generateSessionToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
