// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for formatting and parsing financial amounts using consistent currency standards.
 */
public class CurrencyFormatter {

    private static String currencySymbol = "Rs. ";
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public static synchronized void setCurrencySymbol(String symbol) {
        currencySymbol = (symbol != null) ? symbol.trim() : "";
    }

    public static synchronized String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Formats a BigDecimal as a formatted currency string (e.g. Rs. 1,499.00 or \u20B9 1,499.00).
     */
    public static synchronized String format(BigDecimal amount) {
        if (amount == null) {
            return currencySymbol + "0.00";
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return currencySymbol + DECIMAL_FORMAT.format(scaled);
    }

    /**
     * Formats a percentage value (e.g. "+8.35%" or "-4.50%").
     */
    public static String formatPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return "N/A";
        }
        BigDecimal scaled = percentage.setScale(2, RoundingMode.HALF_UP);
        String sign = (scaled.compareTo(BigDecimal.ZERO) > 0) ? "+" : "";
        return sign + scaled.toPlainString() + "%";
    }

    /**
     * Parses a raw text input into a standardized BigDecimal with 2 decimal places.
     * Strips currency symbols and comma separators.
     */
    public static BigDecimal parse(String raw) throws NumberFormatException {
        if (raw == null) {
            throw new NumberFormatException("Price string cannot be null");
        }
        String clean = raw.trim().replace(currencySymbol, "").replace("$", "").replace(",", "").trim();
        if (clean.isEmpty()) {
            throw new NumberFormatException("Price string cannot be empty");
        }
        BigDecimal value = new BigDecimal(clean);
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

