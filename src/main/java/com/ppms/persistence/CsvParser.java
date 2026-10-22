// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC-4180 compliant CSV Parser implemented purely using standard Java.
 * Properly handles embedded commas, double quotes, escaped quotes (""), and multiline fields.
 */
public class CsvParser {

    /**
     * Parses the entire input reader into a list of string arrays (rows).
     *
     * @param reader incoming text stream
     * @return list of parsed rows
     * @throws IOException on I/O error or malformed CSV structure
     */
    public static List<List<String>> parse(Reader reader) throws IOException {
        List<List<String>> records = new ArrayList<>();
        BufferedReader bufferedReader = (reader instanceof BufferedReader)
                ? (BufferedReader) reader
                : new BufferedReader(reader);

        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int ch;
        int lineNum = 1;

        while ((ch = bufferedReader.read()) != -1) {
            char c = (char) ch;

            if (inQuotes) {
                if (c == '"') {
                    // Look ahead for escaped quote ("")
                    bufferedReader.mark(1);
                    int next = bufferedReader.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            bufferedReader.reset();
                        }
                    }
                } else {
                    currentField.append(c);
                    if (c == '\n') {
                        lineNum++;
                    }
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    // Check for trailing \n
                    bufferedReader.mark(1);
                    int next = bufferedReader.read();
                    if (next != '\n' && next != -1) {
                        bufferedReader.reset();
                    }
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                    lineNum++;
                } else if (c == '\n') {
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    records.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                    lineNum++;
                } else {
                    currentField.append(c);
                }
            }
        }

        if (inQuotes) {
            throw new IOException("Malformed CSV: Unclosed quote detected at line " + lineNum);
        }

        // Add any remaining field/row if stream ends without newline
        if (currentField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentField.toString());
            records.add(currentRow);
        }

        return records;
    }

    public static List<List<String>> parseString(String csvContent) throws IOException {
        return parse(new StringReader(csvContent != null ? csvContent : ""));
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

