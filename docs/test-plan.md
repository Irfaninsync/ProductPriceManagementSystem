> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Test Plan and Verification Report — PPMS

## 1. Test Strategy Overview

The testing methodology consists of:
1. **Unit Testing**: Validates isolated models (`Product`, `PriceHistory`, `ValidationResult`), calculations, and validators.
2. **Integration Testing**: Verifies multi-component workflows between Services, Repositories, and RFC-4180 CSV persistence.
3. **End-to-End Testing**: Exercises the complete 11-step lifecycle (Add -> Search -> Modify Price -> Audit Log -> Analytics -> Export -> Shutdown -> Restart -> Reload Verification).
4. **Resilience Testing**: Tests malformed CSV data, embedded commas and quotation marks, duplicate identifiers, and backup restoration.

---

## 2. Automated Test Suites Summary

| Test Suite Class | Focus Area | Test Cases | Execution Status |
| :--- | :--- | :--- | :--- |
| `ProductTest` | Model, Validation, Catalog Operations | 8 test methods | **PASSED (100%)** |
| `PricingTest` | Pricing Proposals, Audit Trail, Calculations | 6 test methods | **PASSED (100%)** |
| `PersistenceTest` | RFC-4180 Parsing, Safe Writes, Backups | 5 test methods | **PASSED (100%)** |
| `AnalyticsTest` | Live Metrics, Medians, Margin/Markup %, Empty Datasets | 3 test methods | **PASSED (100%)** |
| `ImportExportTest` | CSV Batch Import, Row Rejection, Round-Trip Export | 2 test methods | **PASSED (100%)** |
| `EndToEndIntegrationTest` | Full 11-step Lifecycle & Restart Survival | 1 end-to-end workflow | **PASSED (100%)** |

---

## 3. Detailed Test Cases & Observed Results

### 3.1 Product Operations
- `testCreateValidProduct`: Confirms proper instantiation, scale normalization (2 decimals), and initial margin calculations.
- `testRejectDuplicateId`: Asserts `IllegalArgumentException` when attempting to register an existing product ID.
- `testRejectInvalidPrices`: Asserts rejection of negative cost prices and negative selling prices.
- `testRejectInvalidQuantities`: Asserts exception thrown on negative inventory stock quantities.
- `testUpdateValidProductDetails`: Confirms successful update of name, category, and description with timestamp refresh.
- `testRejectInvalidEditsWithoutCorruptingEntity`: Verifies that an invalid update (e.g. blank name) throws an error and leaves in-memory repository object unmodified.
- `testArchiveAndRestore`: Confirms state transitions between `ACTIVE` and `ARCHIVED`, ensuring archived products do not appear in active listings.
- `testSearchAndFilter`: Confirms case-insensitive substring searching and multi-attribute category filtering.

### 3.2 Pricing Operations
- `testPriceIncreaseAndAuditTrail`: Validates price increase, correct computation of difference (+Rs. 50.00) and percentage change (+8.35%), and verifies that `PriceHistory` is saved with correct old/new prices.
- `testPriceDecreaseAndAuditTrail`: Validates price reduction, negative percentage (-10.00%), and flags decrease.
- `testProposalCancellationDoesNotModifyProduct`: Confirms that previewing a proposal does not alter the product price or create audit records until explicit confirmation.
- `testIdenticalPriceRejection`: Asserts that proposing a price identical to the current price is rejected.
- `testPercentageChangeCalculationFormula`: Verifies mathematical accuracy: `((new - old) / old) * 100`.
- `testZeroOldPriceHandling`: Asserts that when `oldPrice` is 0.00, `getPercentageChange()` returns `Optional.empty()` to prevent division by zero.

### 3.3 Persistence & RFC-4180 CSV
- `testRfc4180CsvParserWithCommasAndQuotes`: Tests parsing of embedded commas, embedded newlines, and escaped quotation marks (`""`). Verifies round-trip escaping with `CsvWriter`.
- `testSaveAndReloadProductsAcrossRestarts`: Saves 2 products to temporary CSV, discards repository instance, instantiates fresh repository, and asserts all fields (name, prices, stock, timestamps) are reloaded identically.
- `testSaveAndReloadPriceHistory`: Verifies audit records persist across restarts.
- `testMalformedRowResilience`: Writes broken CSV lines with non-numeric prices and strings in place of integers. Confirms repository logs diagnostic warnings and loads valid records without crashing.
- `testBackupAndRestore`: Creates snapshot, mutates live catalog, executes restore, and verifies that original prices are completely restored.

### 3.4 Financial Analytics
- `testAnalyticsCalculations`: Tests 3 products with prices 100, 200, 300 (avg 200.00, median 200.00), total cost Rs. 1,750.00, total retail Rs. 2,600.00, gross margin Rs. 850.00, gross margin % 32.69%, markup % 48.57%.
- `testEmptyCatalogAnalyticsSafety`: Confirms zero crashes or divide-by-zero exceptions on empty datasets.
- `testBelowCostAndLowStockAlerts`: Confirms accurate detection of loss-leader products and low inventory.

### 3.5 Import/Export
- `testImportValidationAndCommit`: Tests CSV with 2 valid items, 1 duplicate ID, and 1 negative price row. Asserts 2 rows accepted, 2 rejected with exact row numbers and reasons, and verifies persistence.
- `testExportAndRoundTripRead`: Exports products to CSV, reads back into clean repository, and verifies 100% field fidelity.

### 3.6 End-to-End Workflow
- Starts application in isolated sandbox.
- Adds `PRD-9999` ("Pro Stream Webcam 4K").
- Searches by keyword.
- Updates selling price from Rs. 12,999.00 to Rs. 14,499.00 with audit reason.
- Verifies `HST-1001` recorded.
- Verifies live analytics calculation.
- Exports catalog.
- Simulates complete system shutdown.
- Re-launches system reading sandbox CSV files.
- Confirms product and audit history survived restart completely intact.


---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
