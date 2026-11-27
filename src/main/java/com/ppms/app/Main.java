// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.app;

import java.nio.charset.StandardCharsets;

/**
 * Application entry point for the Product Price Management System (PPMS).
 * Launches the local-first browser application by default, or legacy CLI with --cli.
 */
public class Main {

    public static void main(String[] args) {
        try {
            try {
                System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
            } catch (Exception ignored) {}

            boolean cliMode = false;
            boolean headless = false;
            for (String arg : args) {
                if ("--cli".equalsIgnoreCase(arg)) {
                    cliMode = true;
                } else if ("--headless".equalsIgnoreCase(arg)) {
                    headless = true;
                }
            }

            if (cliMode) {
                Application cliApp = new Application();
                cliApp.run();
            } else {
                DesktopApplication desktopApp = new DesktopApplication(
                        AppPaths.resolveDefault(), headless, com.ppms.server.LocalHttpServer.PREFERRED_PORT);
                desktopApp.run();
            }
        } catch (Throwable t) {
            System.err.println("Fatal system initialization error: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
