// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.app;

/**
 * Application entry point for the Product Price Management System (PPMS).
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Attempt to enable UTF-8 console output where supported
            try {
                System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8.name()));
            } catch (Exception ignored) {}

            Application app = new Application();
            app.run();
        } catch (Throwable t) {
            System.err.println("Fatal system initialization error: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

