// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Formats structured tabular data into clean, aligned console tables.
 * Supports clean box borders with automatic column width calculation and plain-text fallback.
 */
public class TableFormatter {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();
    private final boolean useBoxDrawing;

    public TableFormatter(List<String> headers, boolean useBoxDrawing) {
        this.headers = new ArrayList<>(headers != null ? headers : List.of());
        this.useBoxDrawing = useBoxDrawing;
    }

    public TableFormatter(String[] headers, boolean useBoxDrawing) {
        this(Arrays.asList(headers), useBoxDrawing);
    }

    public void addRow(List<String> row) {
        if (row != null) {
            rows.add(new ArrayList<>(row));
        }
    }

    public void addRow(String... rowValues) {
        addRow(Arrays.asList(rowValues));
    }

    public String render() {
        if (headers.isEmpty() && rows.isEmpty()) {
            return "(Empty Table)";
        }

        int colCount = headers.size();
        for (List<String> r : rows) {
            colCount = Math.max(colCount, r.size());
        }

        int[] colWidths = new int[colCount];

        for (int i = 0; i < headers.size(); i++) {
            colWidths[i] = Math.max(colWidths[i], headers.get(i).length());
        }

        for (List<String> r : rows) {
            for (int i = 0; i < r.size(); i++) {
                String val = (r.get(i) != null) ? r.get(i) : "";
                colWidths[i] = Math.max(colWidths[i], val.length());
            }
        }

        // Add 2 padding spaces minimum per column
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i] += 2;
        }

        StringBuilder sb = new StringBuilder();

        // Border characters with explicit Unicode escape sequences
        char hLine = useBoxDrawing ? '\u2500' : '-';
        char vLine = useBoxDrawing ? '\u2502' : '|';
        char cross = useBoxDrawing ? '\u253C' : '+';
        char tDown = useBoxDrawing ? '\u252C' : '+';
        char tUp   = useBoxDrawing ? '\u2534' : '+';
        char topLeft = useBoxDrawing ? '\u250C' : '+';
        char topRight = useBoxDrawing ? '\u2510' : '+';
        char bottomLeft = useBoxDrawing ? '\u2514' : '+';
        char bottomRight = useBoxDrawing ? '\u2518' : '+';
        char leftCross = useBoxDrawing ? '\u251C' : '+';
        char rightCross = useBoxDrawing ? '\u2524' : '+';

        // Top Border
        sb.append(topLeft);
        for (int i = 0; i < colCount; i++) {
            sb.append(repeat(hLine, colWidths[i]));
            sb.append(i == colCount - 1 ? topRight : tDown);
        }
        sb.append("\n");

        // Headers
        if (!headers.isEmpty()) {
            sb.append(vLine);
            for (int i = 0; i < colCount; i++) {
                String h = i < headers.size() ? headers.get(i) : "";
                sb.append(padCenter(h, colWidths[i])).append(vLine);
            }
            sb.append("\n");

            // Header separator
            sb.append(leftCross);
            for (int i = 0; i < colCount; i++) {
                sb.append(repeat(hLine, colWidths[i]));
                sb.append(i == colCount - 1 ? rightCross : cross);
            }
            sb.append("\n");
        }

        // Rows
        if (rows.isEmpty()) {
            sb.append(vLine);
            int totalInnerWidth = 0;
            for (int w : colWidths) totalInnerWidth += w;
            totalInnerWidth += (colCount - 1);
            sb.append(padCenter("(No records to display)", totalInnerWidth)).append(vLine).append("\n");
        } else {
            for (List<String> r : rows) {
                sb.append(vLine);
                for (int i = 0; i < colCount; i++) {
                    String val = (i < r.size() && r.get(i) != null) ? r.get(i) : "";
                    sb.append(padLeftRight(val, colWidths[i])).append(vLine);
                }
                sb.append("\n");
            }
        }

        // Bottom border
        sb.append(bottomLeft);
        for (int i = 0; i < colCount; i++) {
            sb.append(repeat(hLine, colWidths[i]));
            sb.append(i == colCount - 1 ? bottomRight : tUp);
        }
        sb.append("\n");

        return sb.toString();
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    private static String padCenter(String s, int width) {
        if (s.length() >= width) return s;
        int left = (width - s.length()) / 2;
        int right = width - s.length() - left;
        return repeat(' ', left) + s + repeat(' ', right);
    }

    private static String padLeftRight(String s, int width) {
        if (s.length() >= width) return s;
        int right = width - s.length() - 1;
        return " " + s + repeat(' ', Math.max(0, right));
    }
}

// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
