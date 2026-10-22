> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Functional & Non-Functional Requirements — PPMS

## 1. Functional Requirements Matrix

| Module | Requirement Description | Implementation Status | Verification Method |
| :--- | :--- | :--- | :--- |
| **Module A** | Real-time Main Dashboard with live calculated statistics | Implemented | `AnalyticsTest`, Console Output |
| **Module A** | Zero hardcoded statistics; graceful empty catalog handling | Implemented | `AnalyticsTest.testEmptyCatalogAnalyticsSafety` |
| **Module B** | Register new product with custom or automated ID (`PRD-xxxx`) | Implemented | `ProductTest.testCreateValidProduct` |
| **Module B** | Reject duplicate IDs & prevent ID recycling across restarts | Implemented | `ProductTest.testRejectDuplicateId`, `PersistenceTest` |
| **Module B** | Multi-attribute search (ID, name, category, description) | Implemented | `ProductTest.testSearchAndFilter` |
| **Module B** | Deterministic multi-criteria sorting (Price, Cost, Name, Qty, Date) | Implemented | `ProductService.sort` |
| **Module B** | Multi-boundary filtering (Price range, Category, Status, Alerts) | Implemented | `ProductTest.testSearchAndFilter` |
| **Module B** | Soft product archive & restoration without history destruction | Implemented | `ProductTest.testArchiveAndRestore` |
| **Module C** | Selling price preview with old/new price, diff, and % change | Implemented | `PricingTest.testPriceIncreaseAndAuditTrail` |
| **Module C** | User confirmation required before modifying product or writing to disk | Implemented | `PricingTest.testProposalCancellationDoesNotModifyProduct` |
| **Module C** | Identical price modification rejection | Implemented | `PricingTest.testIdenticalPriceRejection` |
| **Module D** | Immutable historical audit record creation on every selling price update | Implemented | `PricingTest`, `PersistenceTest` |
| **Module D** | Historical query filters (product ID, date range, reason keyword) | Implemented | `EndToEndIntegrationTest` |
| **Module D** | Calculation of largest historical price increase and decrease | Implemented | `PricingService.getLargestPriceIncrease/Decrease` |
| **Module E** | Minimum, maximum, average, and median active selling price | Implemented | `AnalyticsTest.testAnalyticsCalculations` |
| **Module E** | Total inventory cost, retail valuation, gross margin % and markup % | Implemented | `AnalyticsTest.testAnalyticsCalculations` |
| **Module F** | Search, filter boundary validation (reject min > max with message) | Implemented | `ProductService.filter` |
| **Module G** | Below-cost product identification (Selling < Cost) | Implemented | `AnalyticsTest.testBelowCostAndLowStockAlerts` |
| **Module G** | Configurable low-stock threshold alert | Implemented | `AnalyticsTest.testBelowCostAndLowStockAlerts` |
| **Module H** | RFC-4180 CSV export for products and audit history | Implemented | `ImportExportTest.testExportAndRoundTripRead` |
| **Module H** | CSV import validation with line-by-line error reporting | Implemented | `ImportExportTest.testImportValidationAndCommit` |
| **Module I** | Bulk percentage price adjustments with pre-commit tabular preview | Implemented | `PricingService.previewBulkAdjustment`, Menu Option |
| **Module J** | Local timestamped snapshot backup creation (`data/backups/`) | Implemented | `PersistenceTest.testBackupAndRestore` |
| **Module J** | Backup integrity validation and safe restore with pre-rollback snapshot | Implemented | `PersistenceTest.testBackupAndRestore` |
| **Module K** | Configurable settings (Low stock threshold, currency symbol, ANSI, borders) | Implemented | `MetadataHandler`, `ConsoleUI` |

---

## 2. Non-Functional Requirements

### 2.1 Reliability & Data Integrity
- Financial calculations must use `java.math.BigDecimal` exclusively with explicit rounding (`RoundingMode.HALF_UP`). Floating-point primitive types (`double`, `float`) are prohibited for financial state.
- Safe-write file semantics: Files are staged to temporary files (`.tmp`), synced, backed up to `.bak`, and atomically replaced (`ATOMIC_MOVE` or `REPLACE_EXISTING`).

### 2.2 Usability & UI/UX
- Terminal presentation features a structured visual hierarchy: ASCII/Box table borders, formatted currency (`Rs. 1,499.00`), paginated views for large lists, clear status tags (`[SUCCESS]`, `[WARNING]`, `[ERROR]`).
- ANSI escape codes are optional and dynamically toggleable to ensure zero garbage output on non-ANSI consoles.

### 2.3 Portability & Technology Restrictions
- Pure Core Java SE 17 standard library.
- Zero external build tool or framework dependencies (no Maven/Gradle required, standard `javac` compilable).
- Compatible with Windows, Linux, and macOS.


---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
