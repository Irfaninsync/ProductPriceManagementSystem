package com.ppms.persistence;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * RFC-4180 compliant CSV Writer implemented purely with Core Java.
 * Properly quotes and escapes fields containing special characters.
 */
public class CsvWriter {

    /**
     * Escapes a single CSV cell value according to RFC-4180 rules.
     */
    public static String escapeField(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (needsQuotes) {
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }

    /**
     * Formats a row of fields into a CSV line.
     */
    public static String formatRow(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            sb.append(escapeField(fields.get(i)));
            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Writes all rows to the given Writer followed by standard CRLF line breaks.
     */
    public static void writeAll(Writer writer, List<List<String>> rows) throws IOException {
        for (List<String> row : rows) {
            writer.write(formatRow(row));
            writer.write("\r\n");
        }
        writer.flush();
    }
}
