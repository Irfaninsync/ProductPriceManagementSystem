// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Audit record describing the outcome of a batch CSV import operation.
 */
public class ImportReport {

    public static class RowRejection {
        private final int lineNumber;
        private final String rawRecord;
        private final String reason;

        public RowRejection(int lineNumber, String rawRecord, String reason) {
            this.lineNumber = lineNumber;
            this.rawRecord = rawRecord;
            this.reason = reason;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getRawRecord() {
            return rawRecord;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "Line " + lineNumber + ": " + reason + " [Data: " + rawRecord + "]";
        }
    }

    private int totalRowsProcessed = 0;
    private final List<Product> validProducts = new ArrayList<>();
    private final List<RowRejection> rejections = new ArrayList<>();

    public void recordValid(Product product) {
        if (product != null) {
            validProducts.add(product);
        }
    }

    public void recordRejection(int lineNumber, String rawRecord, String reason) {
        rejections.add(new RowRejection(lineNumber, rawRecord, reason));
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public int getAcceptedCount() {
        return validProducts.size();
    }

    public int getRejectedCount() {
        return rejections.size();
    }

    public List<Product> getValidProducts() {
        return Collections.unmodifiableList(validProducts);
    }

    public List<RowRejection> getRejections() {
        return Collections.unmodifiableList(rejections);
    }

    public boolean hasRejections() {
        return !rejections.isEmpty();
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

