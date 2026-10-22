package com.ppms.ui;

import com.ppms.util.CurrencyFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Handles terminal UI presentation, input parsing, ANSI color formatting, and user prompts.
 */
public class ConsoleUI {

    // ANSI Escape Codes
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";

    private final Scanner scanner;
    private boolean ansiEnabled = true;
    private boolean boxDrawingEnabled = false;

    public ConsoleUI(Scanner scanner) {
        this.scanner = scanner != null ? scanner : new Scanner(System.in);
    }

    public boolean isAnsiEnabled() {
        return ansiEnabled;
    }

    public void setAnsiEnabled(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
    }

    public boolean isBoxDrawingEnabled() {
        return boxDrawingEnabled;
    }

    public void setBoxDrawingEnabled(boolean boxDrawingEnabled) {
        this.boxDrawingEnabled = boxDrawingEnabled;
    }

    public void print(String message) {
        System.out.print(message);
    }

    public void println(String message) {
        System.out.println(message);
    }

    public void println() {
        System.out.println();
    }

    public void printSuccess(String message) {
        if (ansiEnabled) {
            System.out.println(GREEN + BOLD + "[SUCCESS] " + RESET + GREEN + message + RESET);
        } else {
            System.out.println("[SUCCESS] " + message);
        }
    }

    public void printWarning(String message) {
        if (ansiEnabled) {
            System.out.println(YELLOW + BOLD + "[WARNING] " + RESET + YELLOW + message + RESET);
        } else {
            System.out.println("[WARNING] " + message);
        }
    }

    public void printError(String message) {
        if (ansiEnabled) {
            System.out.println(RED + BOLD + "[ERROR] " + RESET + RED + message + RESET);
        } else {
            System.out.println("[ERROR] " + message);
        }
    }

    public void printInfo(String message) {
        if (ansiEnabled) {
            System.out.println(CYAN + "[INFO] " + RESET + message);
        } else {
            System.out.println("[INFO] " + message);
        }
    }

    public void renderBanner() {
        if (ansiEnabled) {
            System.out.println(CYAN + BOLD +
                    "================================================================================" + RESET);
            System.out.println(CYAN + BOLD +
                    "           PRODUCT PRICE MANAGEMENT SYSTEM (PPMS) - CORE JAVA                   " + RESET);
            System.out.println(WHITE +
                    "             Enterprise Pricing Governance, History & Analytics                 " + RESET);
            System.out.println(CYAN + BOLD +
                    "================================================================================" + RESET);
        } else {
            System.out.println("================================================================================");
            System.out.println("           PRODUCT PRICE MANAGEMENT SYSTEM (PPMS) - CORE JAVA                   ");
            System.out.println("             Enterprise Pricing Governance, History & Analytics                 ");
            System.out.println("================================================================================");
        }
    }

    public void renderSectionHeader(String sectionTitle) {
        println();
        if (ansiEnabled) {
            System.out.println(BLUE + BOLD + "--- " + sectionTitle.toUpperCase() + " ---" + RESET);
        } else {
            System.out.println("--- " + sectionTitle.toUpperCase() + " ---");
        }
    }

    public String readString(String prompt, boolean allowEmpty) {
        while (true) {
            print(prompt + (allowEmpty ? " (Optional, press Enter to skip): " : ": "));
            String input = scanner.nextLine();
            if (input == null) {
                return "";
            }
            String trimmed = input.trim();
            if (!allowEmpty && trimmed.isEmpty()) {
                printError("Input cannot be empty. Please enter a valid value.");
                continue;
            }
            return trimmed;
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            print(prompt + " [" + min + "-" + max + "]: ");
            String raw = scanner.nextLine();
            if (raw == null) continue;
            try {
                int value = Integer.parseInt(raw.trim());
                if (value < min || value > max) {
                    printError("Value out of range. Expected between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Invalid number format. Please enter an integer.");
            }
        }
    }

    public BigDecimal readBigDecimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            print(prompt + ": ");
            String raw = scanner.nextLine();
            if (raw == null) continue;
            try {
                BigDecimal value = CurrencyFormatter.parse(raw);
                if (min != null && value.compareTo(min) < 0) {
                    printError("Value cannot be less than " + CurrencyFormatter.format(min) + ".");
                    continue;
                }
                if (max != null && value.compareTo(max) > 0) {
                    printError("Value cannot exceed " + CurrencyFormatter.format(max) + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Invalid monetary amount. Example: 499.00 or 1,250.50");
            }
        }
    }

    public boolean readConfirmation(String prompt) {
        while (true) {
            print(prompt + " (yes/no): ");
            String input = scanner.nextLine();
            if (input == null) continue;
            String clean = input.trim().toLowerCase();
            if (clean.equals("y") || clean.equals("yes")) {
                return true;
            }
            if (clean.equals("n") || clean.equals("no")) {
                return false;
            }
            printError("Please respond with 'yes' or 'no'.");
        }
    }

    public void pause() {
        print("\nPress [Enter] to continue...");
        try {
            scanner.nextLine();
        } catch (Exception ignored) {}
    }

    /**
     * Renders a long list in paginated format.
     */
    public <T> void renderPaginated(List<T> items, int pageSize, TableRenderer<T> renderer) {
        if (items == null || items.isEmpty()) {
            println("\n(No records found)");
            pause();
            return;
        }

        int totalPages = (int) Math.ceil((double) items.size() / pageSize);
        int currentPage = 1;

        while (true) {
            int startIdx = (currentPage - 1) * pageSize;
            int endIdx = Math.min(startIdx + pageSize, items.size());
            List<T> pageItems = items.subList(startIdx, endIdx);

            println();
            println(renderer.renderPage(pageItems));
            println(String.format("Displaying records %d to %d of %d (Page %d of %d)",
                    startIdx + 1, endIdx, items.size(), currentPage, totalPages));

            if (totalPages <= 1) {
                pause();
                break;
            }

            print("[N]ext Page | [P]revious Page | [G]o to Page | [E]xit to Menu: ");
            String choice = scanner.nextLine();
            if (choice == null) break;
            String cmd = choice.trim().toUpperCase();

            if (cmd.equals("N")) {
                if (currentPage < totalPages) {
                    currentPage++;
                } else {
                    printWarning("You are already on the last page.");
                }
            } else if (cmd.equals("P")) {
                if (currentPage > 1) {
                    currentPage--;
                } else {
                    printWarning("You are already on the first page.");
                }
            } else if (cmd.equals("G")) {
                currentPage = readInt("Enter page number", 1, totalPages);
            } else if (cmd.equals("E") || cmd.isEmpty()) {
                break;
            } else {
                printError("Unknown navigation option: " + choice);
            }
        }
    }

    @FunctionalInterface
    public interface TableRenderer<T> {
        String renderPage(List<T> pageItems);
    }
}
