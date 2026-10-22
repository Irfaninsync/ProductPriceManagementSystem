# Product Price Management System (PPMS)

> A Professional-Grade Core Java Enterprise Console Application for Product Governance, Pricing Strategy, Historical Audit Trails, and Financial Analytics.

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Academic-blue.svg)]()
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Tests](https://img.shields.io/badge/Tests-6%2F6%20Passed%20(100%25)-brightgreen.svg)]()

---

## 1. Project Overview

Basic e-commerce and retail product catalogues typically store only current product prices. When prices fluctuate due to supplier price increases, inflation, discounts, or competitor matching, traditional simple systems lose historical context. Shop managers and business administrators cannot inspect *why* a price changed, *who* authorized it, or *how* prices evolved over time.

The **Product Price Management System (PPMS)** solves this problem. It is a standalone, robust desktop terminal software application built purely in **Core Java (SE 17)** with local, crash-resilient **CSV persistence**. PPMS provides:
- Complete product lifecycle and inventory management.
- Formal pricing governance: propose, preview difference and percentage change, enter business reasons, require confirmation, and record immutable audit records.
- Comprehensive price history: filter by product, date range, or reason; identify largest price increases and decreases.
- Financial analytics: live calculations of minimum, maximum, average, and median prices, inventory valuation (cost vs retail), gross margins, markup %, and below-cost alerts.
- Data management: RFC-4180 CSV export, batch import with line-by-line validation, and timestamped snapshot backup/restore.

---

## 2. Technology Constraints & Architecture

### 2.1 Permitted Core Java Technologies
- **Core Language**: Java classes, interfaces, records/enums, generics, encapsulation, and composition.
- **Collections Framework**: `ArrayList`, `LinkedHashMap`, `HashSet`, `Comparator`, and standard Java Streams.
- **Financial Arithmetic**: `java.math.BigDecimal` with `RoundingMode.HALF_UP` (zero primitive `double`/`float` for currency).
- **Date & Time API**: `java.time.LocalDateTime`, `LocalDate`, and `DateTimeFormatter`.
- **File I/O**: `java.nio.file.Files`, `Path`, `BufferedReader`, `BufferedWriter`, and UTF-8 charset.
- **Persistence**: Custom RFC-4180 compliant CSV parser and atomic file replacement (`.tmp` -> `.bak` -> atomic rename).
- **Console Interface**: Interactive console with clean ASCII borders (`+---`, `|`), optional Unicode box characters, and toggleable ANSI colors.

### 2.2 Prohibited Technologies (Preserved 100%)
- **NO** Spring Boot, Spring Framework, or Jakarta EE.
- **NO** external database engines (No MySQL, PostgreSQL, SQLite, MongoDB).
- **NO** ORM frameworks (No Hibernate).
- **NO** web frontends (No React, Angular, Vue, HTML/JS).
- **NO** REST APIs or external cloud services.
- **NO** third-party CSV libraries (e.g., Apache Commons CSV, OpenCSV).
- **NO** third-party testing dependencies (fully self-contained assertion and test execution runner).

---

## 3. Directory Layout

```
ProductPriceManagementSystem/
├── src/
│   ├── main/java/com/ppms/
│   │   ├── app/
│   │   │   ├── Application.java        # Bootstrap & dependency injection
│   │   │   └── Main.java               # Entry point
│   │   ├── ui/
│   │   │   ├── ConsoleUI.java          # Console I/O, ANSI colors, pagination
│   │   │   ├── MenuController.java     # Menu navigation & routing
│   │   │   └── TableFormatter.java     # Tabular border rendering
│   │   ├── model/
│   │   │   ├── Product.java            # Product entity with financial methods
│   │   │   ├── ProductStatus.java      # ACTIVE / ARCHIVED enum
│   │   │   ├── PriceHistory.java       # Immutable audit history record
│   │   │   ├── PriceChangeReason.java  # Predefined business reasons
│   │   │   ├── PriceProposal.java      # Pre-commit price change DTO
│   │   │   ├── AnalyticsSummary.java   # Financial & inventory metrics DTO
│   │   │   └── ImportReport.java       # CSV batch import audit report
│   │   ├── service/
│   │   │   ├── ProductService.java     # Catalog CRUD, search, filter, sort
│   │   │   ├── PricingService.java     # Pricing workflow & audit trail
│   │   │   ├── AnalyticsService.java   # Live metrics computation
│   │   │   ├── ImportExportService.java# CSV import/export engine
│   │   │   └── BackupService.java      # Snapshot backup & restore
│   │   ├── repository/
│   │   │   ├── ProductRepository.java  # Repository abstraction
│   │   │   └── PriceHistoryRepository.java
│   │   ├── persistence/
│   │   │   ├── CsvProductRepository.java
│   │   │   ├── CsvPriceHistoryRepository.java
│   │   │   ├── CsvFileHandler.java     # Atomic safe-write mechanics
│   │   │   ├── CsvParser.java          # RFC-4180 state-machine parser
│   │   │   ├── CsvWriter.java          # RFC-4180 field escaper
│   │   │   ├── CsvRow.java             # Typed row accessor
│   │   │   └── MetadataHandler.java    # Schema & configuration persistence
│   │   ├── validation/
│   │   │   ├── ProductValidator.java   # Centralized domain validation
│   │   │   └── ValidationResult.java   # Error container
│   │   ├── report/
│   │   │   └── ReportGenerator.java    # Formatted operational reports
│   │   └── util/
│   │       ├── CurrencyFormatter.java  # Monetary formatting (Rs. / ₹)
│   │       ├── DateTimeUtil.java       # ISO-8601 & display formatting
│   │       └── IdGenerator.java        # Permanent atomic ID generator
│   └── test/java/com/ppms/
│       ├── Assert.java                 # Zero-dependency test assertions
│       ├── ProductTest.java            # Unit tests for Product & Catalog
│       ├── PricingTest.java            # Unit tests for Pricing & Auditing
│       ├── PersistenceTest.java        # Unit tests for CSV & Atomic Writes
│       ├── AnalyticsTest.java          # Unit tests for Metrics & Margins
│       ├── ImportExportTest.java       # Unit tests for CSV Batch Import
│       ├── EndToEndIntegrationTest.java# Full 11-step lifecycle verification
│       └── TestRunner.java             # Master test execution suite
├── data/
│   ├── products.csv                    # Persistent product records
│   ├── price_history.csv               # Persistent price audit trail
│   ├── metadata.csv                    # System configuration metadata
│   └── backups/                        # Snapshot backup directories
├── docs/
│   ├── architecture.md                 # Layered architecture & diagrams
│   ├── class-diagram.md                # UML class diagram
│   ├── requirements.md                 # Functional & non-functional matrix
│   ├── test-plan.md                    # Detailed test cases & results
│   ├── user-guide.md                   # Complete operator manual
│   └── team-contributions.md           # 6-role academic contribution record
├── compile.bat                         # Windows CMD compiler
├── run.bat                             # Windows CMD runner
├── test.bat                            # Windows CMD test runner
├── build.ps1                           # PowerShell compiler
├── run.ps1                             # PowerShell runner
├── compile.sh                          # Linux / macOS compiler
├── run.sh                              # Linux / macOS runner
├── PROJECT_REPORT.md                   # Academic project report
└── README.md                           # This document
```

---

## 4. Build, Run, and Test Instructions

### 4.1 Compiling the Codebase
```powershell
# Windows PowerShell
./build.ps1

# Windows Command Prompt
compile.bat

# Linux / macOS
bash compile.sh

# Plain javac command (any environment)
javac -d bin -encoding UTF-8 src/main/java/com/ppms/*/*.java src/test/java/com/ppms/*.java
```

### 4.2 Running the Application
```powershell
# Windows PowerShell
./run.ps1

# Windows Command Prompt
run.bat

# Linux / macOS
bash run.sh

# Plain java command
java -cp bin com.ppms.app.Main
```

### 4.3 Running Automated Tests
```powershell
# Windows Command Prompt
test.bat

# Plain java command
java -cp bin com.ppms.TestRunner
```
*Output: 6 of 6 test suites passed with 100% success.*

---

## 5. CSV Schemas & Data Integrity

### 5.1 `data/products.csv`
| Column | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `productId` | String | `PRD-1001` | Permanent unique identifier |
| `name` | String | `Wireless Mouse` | Product name (quoted if contains comma) |
| `category` | String | `Peripherals` | Product classification |
| `description` | String | `Ergonomic mouse` | Optional description |
| `costPrice` | BigDecimal | `400.00` | Normalized procurement cost |
| `sellingPrice` | BigDecimal | `599.00` | Current active retail price |
| `quantity` | int | `25` | Units in physical stock |
| `status` | Enum | `ACTIVE` | `ACTIVE` or `ARCHIVED` |
| `createdAt` | ISO-8601 | `2026-10-22T10:00:00` | Timestamp of initial registration |
| `updatedAt` | ISO-8601 | `2026-10-22T12:00:00` | Timestamp of last modification |

### 5.2 `data/price_history.csv`
| Column | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `historyId` | String | `HST-1001` | Permanent unique audit record ID |
| `productId` | String | `PRD-1001` | Foreign key referencing product |
| `oldPrice` | BigDecimal | `599.00` | Selling price immediately prior to change |
| `newPrice` | BigDecimal | `649.00` | Approved new selling price |
| `changedAt` | ISO-8601 | `2026-10-22T12:15:00` | Exact timestamp of price modification |
| `reason` | String | `Supplier price increase`| Mandatory business reason for change |

### 5.3 Crash-Safe File Write Semantics
Because flat CSV files lack native database ACID transactions, PPMS implements a safe write workflow:
1. All changes are written to a temporary staging file (`products.csv.tmp`).
2. Staging file is flushed and synced to disk.
3. The existing live file is copied to `products.csv.bak`.
4. The staging file atomically replaces `products.csv` using `Files.move` with `StandardCopyOption.ATOMIC_MOVE` (falling back to `REPLACE_EXISTING`).
5. In-memory data is updated only after disk synchronization succeeds.

---

## 6. Financial Calculation Rules

- **Gross Margin Amount** $= \text{Selling Price} - \text{Cost Price}$
- **Gross Margin Percentage** $= \left( \frac{\text{Selling Price} - \text{Cost Price}}{\text{Selling Price}} \right) \times 100$
  *(Handled gracefully when Selling Price is zero)*
- **Markup Percentage** $= \left( \frac{\text{Selling Price} - \text{Cost Price}}{\text{Cost Price}} \right) \times 100$
  *(Handled gracefully when Cost Price is zero)*
- **Price Difference** $= \text{New Selling Price} - \text{Old Selling Price}$
- **Price Percentage Change** $= \left( \frac{\text{New Price} - \text{Old Price}}{\text{Old Price}} \right) \times 100$
  *(When Old Price is zero, returns `Optional.empty()` rather than dividing by zero)*

---

## 7. Known Limitations
1. **Concurrent Writers**: Because persistence uses flat local files rather than a database lock manager, running multiple independent OS processes writing to the same CSV file simultaneously is not supported.
2. **Transaction Rollbacks Across Multiple Files**: While individual file writes are atomic, simultaneous failure during multi-file writes is mitigated by snapshot backups and `.bak` files rather than two-phase commit (2PC) write-ahead logging.
