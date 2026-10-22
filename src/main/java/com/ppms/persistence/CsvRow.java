// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.persistence;

import com.ppms.util.CurrencyFormatter;
import com.ppms.util.DateTimeUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed row with column header mapping, line number tracking, and safe type conversions.
 */
public class CsvRow {

    private final int lineNumber;
    private final Map<String, Integer> headerMap;
    private final List<String> rawValues;

    public CsvRow(int lineNumber, Map<String, Integer> headerMap, List<String> rawValues) {
        this.lineNumber = lineNumber;
        this.headerMap = headerMap;
        this.rawValues = rawValues;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<String> getRawValues() {
        return rawValues;
    }

    public boolean hasColumn(String columnName) {
        return headerMap.containsKey(columnName.trim().toLowerCase());
    }

    public String getString(String columnName) {
        Integer idx = headerMap.get(columnName.trim().toLowerCase());
        if (idx == null || idx >= rawValues.size()) {
            return "";
        }
        return rawValues.get(idx).trim();
    }

    public BigDecimal getBigDecimal(String columnName) throws NumberFormatException {
        String val = getString(columnName);
        if (val.isEmpty()) {
            throw new NumberFormatException("Empty value for column '" + columnName + "' at line " + lineNumber);
        }
        return CurrencyFormatter.parse(val);
    }

    public int getInt(String columnName) throws NumberFormatException {
        String val = getString(columnName);
        if (val.isEmpty()) {
            throw new NumberFormatException("Empty value for column '" + columnName + "' at line " + lineNumber);
        }
        return Integer.parseInt(val);
    }

    public LocalDateTime getLocalDateTime(String columnName) {
        String val = getString(columnName);
        if (val.isEmpty()) {
            return null;
        }
        return DateTimeUtil.parseIso(val);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

