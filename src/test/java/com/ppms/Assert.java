// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms;

import java.util.Objects;

/**
 * Lightweight, zero-dependency test assertion framework for Core Java verification.
 */
public class Assert {

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Assertion Failed (expected false, got true): " + message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(String.format("Assertion Failed: %s [Expected: <%s>, Actual: <%s>]",
                    message, expected, actual));
        }
    }

    public static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("Assertion Failed: Expected non-null object, got null: " + message);
        }
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, Executable executable, String message) {
        try {
            executable.execute();
            throw new AssertionError("Assertion Failed: Expected exception " + expectedType.getName() + " was not thrown: " + message);
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError(String.format("Assertion Failed: Expected %s but caught %s (%s)",
                        expectedType.getName(), t.getClass().getName(), t.getMessage()), t);
            }
        }
    }

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

