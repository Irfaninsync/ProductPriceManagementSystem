package com.ppms;

/**
 * Master test runner executing all unit, integration, and persistence test suites.
 * Exits with status 0 on total success, or non-zero on failure.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("       PRODUCT PRICE MANAGEMENT SYSTEM (PPMS) - AUTOMATED TEST SUITE            ");
        System.out.println("================================================================================");

        int suitesRun = 0;
        int suitesPassed = 0;

        try {
            suitesRun++;
            ProductTest.runAll();
            suitesPassed++;

            suitesRun++;
            PricingTest.runAll();
            suitesPassed++;

            suitesRun++;
            PersistenceTest.runAll();
            suitesPassed++;

            suitesRun++;
            AnalyticsTest.runAll();
            suitesPassed++;

            suitesRun++;
            ImportExportTest.runAll();
            suitesPassed++;

            suitesRun++;
            EndToEndIntegrationTest.runAll();
            suitesPassed++;

            System.out.println("================================================================================");
            System.out.println(String.format("TEST EXECUTION COMPLETED: %d of %d test suites passed (100%% Success).",
                    suitesPassed, suitesRun));
            System.out.println("================================================================================");

        } catch (Throwable t) {
            System.err.println("\n[TEST RUNNER FAILURE]: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
