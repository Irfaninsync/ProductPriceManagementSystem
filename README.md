> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Product Price Management System (PPMS)

> A Professional-Grade Core Java Enterprise Console Application for Product Governance, Pricing Strategy, Historical Audit Trails, and Financial Analytics.

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Academic-blue.svg)]()
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Tests](https://img.shields.io/badge/Tests-6%2F6%20Passed%20(100%25)-brightgreen.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-Modular%20Layered-purple.svg)]()
[![Persistence](https://img.shields.io/badge/Persistence-Crash--Safe%20CSV-green.svg)]()

---

## Academic Project Credentials

- **Project Title:** Product Price Management System (PPMS)
- **Domain:** Advanced Object-Oriented Software Engineering & Enterprise Core Java Architecture
- **Operating Environment:** Standard Java Runtime Environment (JRE 17+), Cross-Platform (Windows, Linux, macOS)
- **Persistence Mechanism:** Custom RFC-4180 Compliant CSV Engine with Atomic Safe-Write Semantics
- **Team Registration / Roll Numbers:**
  1. `2025DCSBT10650` — Project Lead & System Architect
  2. `2025DCSBT10651` — Domain Model & Validation Specialist
  3. `2025DCSBT10652` — Persistence Layer & File I/O Engineer
  4. `2025DCSBT10653` — Business Logic & Pricing Engine Developer
  5. `2025DCSBT10654` — Console User Interface & Experience Designer
  6. `2025DCSBT10655` — Quality Assurance & Test Automation Specialist

---

## Table of Contents

1. [Project Overview & Problem Statement](#1-project-overview--problem-statement)
2. [Technology Stack & Architectural Constraints](#2-technology-stack--architectural-constraints)
3. [System Architecture & Layered Design](#3-system-architecture--layered-design)
4. [Project Directory Layout](#4-project-directory-layout)
5. [How the System Works (Under the Hood)](#5-how-the-system-works-under-the-hood)
6. [How to Clone, Compile, and Run (Git Quickstart)](#6-how-to-clone-compile-and-run-git-quickstart)
7. [Comprehensive User Guide (How to Use PPMS)](#7-comprehensive-user-guide-how-to-use-ppms)
8. [CSV Data Schemas & Crash-Resilient Persistence](#8-csv-data-schemas--crash-resilient-persistence)
9. [Financial & Analytical Calculation Rules](#9-financial--analytical-calculation-rules)
10. [Automated Verification & Test Suite](#10-automated-verification--test-suite)
11. [Known Limitations & Future Scope](#11-known-limitations--future-scope)

---

## 1. Project Overview & Problem Statement

### 1.1 The Real-World Challenge
In small-to-medium retail, wholesale, and distributed commercial operations, product pricing is volatile. Costs fluctuate continuously due to supplier rate revisions, inflationary pressures, promotional campaigns, inventory liquidation, and competitor market adjustments. 

Most standalone catalogue management systems suffer from severe structural deficiencies:
1. **Loss of Historical Context**: Modifying a selling price simply overwrites the database cell, permanently erasing the previous price, the change magnitude, and the date of modification.
2. **Absence of Governance & Audit Trails**: Critical commercial adjustments happen without mandatory business justification, authorization records, or pre-commit impact analysis (such as warnings when selling below procurement cost).
3. **Flawed Financial Analytics**: Over-reliance on IEEE 754 floating-point primitives (`double`, `float`) introduces binary rounding drift into financial reports, margin computations, and inventory valuations.
4. **Data Vulnerability**: Unsynchronized local file persistence leads to partial, corrupted files when terminal sessions terminate unexpectedly during I/O operations.

### 1.2 The PPMS Solution
The **Product Price Management System (PPMS)** is an advanced, standalone Core Java enterprise terminal application designed to resolve these challenges without external database servers or third-party web frameworks. PPMS combines rigorous software engineering design patterns, high-precision monetary arithmetic (`BigDecimal`), full RFC-4180 CSV parsing, and crash-resilient atomic file replacement.

---

## 2. Technology Stack & Architectural Constraints

PPMS is deliberately built to showcase software engineering excellence using **100% pure standard Core Java**.

### 2.1 Permitted Core Java Technologies
- **Language Level**: Core Java SE 17+ (Class OOP, Generics, Enums, Records, Interfaces, Lambdas, Streams).
- **Financial Arithmetic**: `java.math.BigDecimal` configured with `RoundingMode.HALF_UP` to prevent monetary rounding discrepancies.
- **Date & Time API**: Modern immutable `java.time.LocalDateTime`, `LocalDate`, and `DateTimeFormatter`.
- **Collections Framework**: `ArrayList`, `LinkedHashMap`, `HashSet`, `Comparator`, and `Optional<T>`.
- **Persistence & File I/O**: `java.nio.file.Files`, `java.nio.file.Path`, `StandardCopyOption`, `BufferedReader`, `BufferedWriter`, and UTF-8 charset.
- **Console Interface**: Interactive ANSI-color terminal interface with automatic ASCII fallback (`+---`, `|`) for universal console compatibility.

### 2.2 Prohibited Technologies (100% Strict Compliance)
- **NO** Spring Boot, Spring Framework, or Jakarta EE.
- **NO** external database engines (No MySQL, PostgreSQL, SQLite, or MongoDB).
- **NO** Object-Relational Mapping (No Hibernate, JPA, or MyBatis).
- **NO** web frontend or HTTP frameworks (No React, Angular, Vue, or REST controllers).
- **NO** third-party CSV utilities (No Apache Commons CSV or OpenCSV).
- **NO** external testing libraries (Self-contained zero-dependency test assertion framework).

---

## 3. System Architecture & Layered Design

PPMS follows an enterprise **Layered (N-Tier) Architectural Pattern**, strictly isolating responsibilities across decoupled components:

```
+-------------------------------------------------------------------------+
|                           PRESENTATION LAYER                            |
|             com.ppms.ui (ConsoleUI, MenuController, TableFormatter)     |
+-------------------------------------------------------------------------+
                                    | calls
                                    v
+-------------------------------------------------------------------------+
|                             SERVICE LAYER                               |
|   com.ppms.service (ProductService, PricingService, AnalyticsService,   |
|                     ImportExportService, BackupService)                 |
+-------------------------------------------------------------------------+
                    |                               | validates with
                    v                               v
+---------------------------------------+   +-----------------------------+
|           REPOSITORY LAYER            |   |      VALIDATION LAYER       |
| com.ppms.repository (ProductRepository|   | com.ppms.validation         |
|             PriceHistoryRepository)   |   | (ProductValidator)          |
+---------------------------------------+   +-----------------------------+
                    | implements
                    v
+-------------------------------------------------------------------------+
|                          PERSISTENCE LAYER                              |
|   com.ppms.persistence (CsvProductRepository, CsvPriceHistoryRepository,|
|            CsvFileHandler, CsvParser, CsvWriter, MetadataHandler)       |
+-------------------------------------------------------------------------+
                                    | file I/O
                                    v
+-------------------------------------------------------------------------+
|                         STORAGE / LOCAL DISK                            |
|        data/products.csv | data/price_history.csv | data/backups/       |
+-------------------------------------------------------------------------+
```

### Architectural Highlights
- **Repository Abstraction**: Service layers interact exclusively with `ProductRepository` and `PriceHistoryRepository` interfaces. The persistence implementation (`CsvProductRepository`) can be swapped without modifying a single line of business logic.
- **Single Source of Truth**: Atomic file writes prevent partial corruption. An in-memory cache maintains instantaneous $O(1)$ indexed access, synchronized atomically with disk on every mutating transaction.
- **Pure Dependency Injection**: All service and repository dependencies are instantiated and wired via constructor injection during bootstrap in `Application.java`.

---

## 4. Project Directory Layout

```
ProductPriceManagementSystem/
|-- src/
|   |-- main/java/com/ppms/
|   |   |-- app/
|   |   |   |-- Application.java          # Dependency injection container & bootstrap
|   |   |   `-- Main.java                 # Main CLI entry point
|   |   |-- ui/
|   |   |   |-- ConsoleUI.java            # Terminal I/O, ANSI colors, pagination
|   |   |   |-- MenuController.java       # Interactive menu navigation router
|   |   |   `-- TableFormatter.java       # Tabular border rendering engine
|   |   |-- model/
|   |   |   |-- Product.java              # Product domain entity with financial methods
|   |   |   |-- ProductStatus.java        # Enum: ACTIVE, ARCHIVED
|   |   |   |-- PriceHistory.java         # Immutable price audit log record
|   |   |   |-- PriceChangeReason.java    # Enum: Predefined business reasons
|   |   |   |-- PriceProposal.java        # Pre-commit price change DTO
|   |   |   |-- AnalyticsSummary.java     # Financial analytics container DTO
|   |   |   `-- ImportReport.java         # CSV batch import audit summary
|   |   |-- service/
|   |   |   |-- ProductService.java       # Catalog CRUD, search, filter, and sort
|   |   |   |-- PricingService.java       # Pricing workflow, proposals, and history
|   |   |   |-- AnalyticsService.java     # Financial metrics, margins, and distributions
|   |   |   |-- ImportExportService.java  # RFC-4180 CSV export and batch import engine
|   |   |   `-- BackupService.java        # Snapshot backup and recovery manager
|   |   |-- repository/
|   |   |   |-- ProductRepository.java    # Product storage interface
|   |   |   `-- PriceHistoryRepository.java # History storage interface
|   |   |-- persistence/
|   |   |   |-- CsvProductRepository.java # CSV product repository with atomic flush
|   |   |   |-- CsvPriceHistoryRepository.java # CSV history repository
|   |   |   |-- CsvFileHandler.java       # Atomic file write mechanics (.tmp -> .bak -> atomic move)
|   |   |   |-- CsvParser.java            # RFC-4180 compliant finite state machine parser
|   |   |   |-- CsvWriter.java            # RFC-4180 field escaper and stream writer
|   |   |   |-- CsvRow.java               # Type-safe indexed CSV row wrapper
|   |   |   `-- MetadataHandler.java      # Key-value system configuration store
|   |   |-- validation/
|   |   |   |-- ProductValidator.java     # Central domain validation rules
|   |   |   `-- ValidationResult.java     # Validation error aggregator
|   |   |-- report/
|   |   |   `-- ReportGenerator.java      # Plain-text and tabular operational reports
|   |   `-- util/
|   |       |-- CurrencyFormatter.java    # Localized monetary formatters (Rs. / ₹ / $)
|   |       |-- DateTimeUtil.java         # ISO-8601 parsing and user-friendly formatting
|   |       `-- IdGenerator.java          # Permanent monotonic ID generator (PRD-xxxx, HST-xxxx)
|   `-- test/java/com/ppms/
|       |-- Assert.java                   # Zero-dependency test assertion framework
|       |-- ProductTest.java              # Unit tests for product domain & catalog
|       |-- PricingTest.java              # Unit tests for pricing logic & proposals
|       |-- PersistenceTest.java          # Unit tests for RFC-4180 CSV & atomic writes
|       |-- AnalyticsTest.java            # Unit tests for margins, median, and valuation
|       |-- ImportExportTest.java         # Unit tests for CSV import, export, and malformed rows
|       |-- EndToEndIntegrationTest.java  # 11-step end-to-end integration scenario
|       `-- TestRunner.java               # Master test suite executor
|-- data/
|   |-- products.csv                      # Persistent product catalog
|   |-- price_history.csv                 # Persistent immutable price audit trail
|   |-- metadata.csv                      # System configuration and ID sequence markers
|   `-- backups/                          # Timestamped snapshot backup archives
|-- docs/
|   |-- architecture.md                   # In-depth architectural documentation
|   |-- class-diagram.md                  # Comprehensive UML class diagram
|   |-- requirements.md                   # Complete functional & non-functional requirements
|   |-- test-plan.md                      # Test cases, verification matrices, and coverage
|   |-- user-guide.md                     # Operator and user manual
|   `-- team-contributions.md             # Academic team responsibility breakdown
|-- compile.bat                           # Windows CMD compilation script
|-- run.bat                               # Windows CMD execution script
|-- test.bat                              # Windows CMD automated test runner
|-- build.ps1                             # PowerShell build script
|-- run.ps1                               # PowerShell execution script
|-- compile.sh                            # Linux / macOS compilation script
|-- run.sh                                # Linux / macOS execution script
|-- PROJECT_REPORT.md                     # Comprehensive academic engineering report
|-- README.md                             # Repository master documentation
`-- .gitignore                            # Clean Git repository ignore definitions
```

---

## 5. How the System Works (Under the Hood)

### 5.1 Bootstrapping and Inversion of Control
When `Main.java` executes:
1. `Application.bootstrap()` initializes the file system directories (`data/`, `data/backups/`).
2. `MetadataHandler` loads persistent application settings (currency symbol, low-stock threshold, next ID sequences).
3. Repositories read `data/products.csv` and `data/price_history.csv` into high-performance `LinkedHashMap` memory stores.
4. `IdGenerator` inspects existing records to ensure IDs continue monotonically (`PRD-1001`, `HST-1001`) without reuse.
5. `MenuController` launches the interactive console loop.

### 5.2 Atomic Safe-Write File Mechanism
Traditional file rewriting is susceptible to corruption if the operating system terminates mid-write. PPMS implements a three-stage atomic write pattern in `CsvFileHandler`:
1. **Stage 1 (Staging)**: Serializes memory state to a temporary file (`data/products.csv.tmp`). All buffers are flushed and forced to disk via `FileChannel.force(true)`.
2. **Stage 2 (Backup)**: Copies the existing live file to `data/products.csv.bak` as a recovery fallback.
3. **Stage 3 (Atomic Replacement)**: Uses `Files.move()` with `StandardCopyOption.ATOMIC_MOVE` to atomically swap the `.tmp` file into place. If the file system does not support atomic moves, it falls back safely to `REPLACE_EXISTING`.
4. The live in-memory repository cache is only updated after disk synchronization succeeds.

### 5.3 RFC-4180 State Machine Parser
Unlike primitive implementations using `String.split(",")` (which break when product names contain commas or quotes), `CsvParser` implements a strict character-by-character deterministic finite state machine (FSM):
- Correctly parses fields containing commas, line breaks, and escaped quotes (`""`).
- Handles trailing empty columns and whitespace trimming.
- Provides line-number error reporting for malformed CSV rows.

### 5.4 Controlled Pricing Governance Workflow
Prices cannot be overwritten arbitrarily. The pricing workflow enforces strict commercial rules:
1. User selects a product ID.
2. System computes and displays current cost, selling price, current margin, and markup.
3. Operator enters proposed new price.
4. System calculates a `PriceProposal`:
   - Absolute difference: $\Delta P = P_{\text{new}} - P_{\text{old}}$
   - Percentage change: $\% \Delta = \left( \frac{P_{\text{new}} - P_{\text{old}}}{P_{\text{old}}} \right) \times 100$
   - New gross margin amount and percentage.
   - Warning triggered if proposed price is below cost price ($P_{\text{new}} < P_{\text{cost}}$).
5. Operator selects a mandatory business reason from `PriceChangeReason` or enters a custom reason.
6. System requires explicit confirmation (`[y/N]`).
7. Upon approval:
   - Current price is updated in `Product`.
   - Immutable `PriceHistory` audit record is generated and persisted.
   - All changes are flushed atomically to disk.

---

## 6. How to Clone, Compile, and Run (Git Quickstart)

> **Can I use this project by `git clone`ing it in my terminal or PowerShell?**  
> **Yes, absolutely!** PPMS is designed with **zero third-party dependencies**. It requires no external database setup (no MySQL/PostgreSQL), no build tools (no Maven or Gradle required), and no Docker containers. Anyone with standard Java (JDK 17+) installed can clone the repository and launch the application in less than 30 seconds.

---

### 6.1 Step-by-Step Quickstart via Git Clone

#### Step 1: Open Terminal or PowerShell
- **Windows**: Press `Win + X` and select **Terminal** or **PowerShell** (or search for `cmd` or `PowerShell`).
- **macOS / Linux**: Open your system **Terminal**.

#### Step 2: Clone the GitHub Repository
Run the standard `git clone` command to download the complete repository onto your machine:
```bash
git clone https://github.com/Irfaninsync/ProductPriceManagementSystem.git
```

#### Step 3: Change Directory into the Cloned Project
```bash
cd ProductPriceManagementSystem
```

#### Step 4: Verify Java Development Kit (JDK 17+)
Ensure Java 17 or higher is installed and registered in your system environment path:
```bash
java -version
javac -version
```
*(If you do not have Java 17+ installed, you can download OpenJDK freely from [https://adoptium.net/](https://adoptium.net/)).*

---

### 6.2 Running the Application by Operating System

#### Method 1: Windows PowerShell
Once inside the cloned folder, run:
```powershell
# 1. Compile all Core Java classes into bin/
./build.ps1

# 2. Run the automated test suite (verifies 6/6 test suites pass)
java -cp bin com.ppms.TestRunner

# 3. Launch the interactive application
./run.ps1
```
*(Note: If PowerShell displays an execution policy notice, you can run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`, or simply run via Command Prompt or Direct Java CLI below).*

#### Method 2: Windows Command Prompt (`cmd.exe`)
Double-click the batch files or execute them in `cmd`:
```cmd
:: 1. Compile all Core Java source files into bin/
compile.bat

:: 2. Execute automated test suite
test.bat

:: 3. Launch the PPMS interactive console interface
run.bat
```

#### Method 3: Linux / macOS (Bash / Zsh)
```bash
# 1. Grant execution permissions to shell scripts
chmod +x compile.sh run.sh

# 2. Compile all source code into bin/
./compile.sh

# 3. Run automated test suite
java -cp bin com.ppms.TestRunner

# 4. Launch the application
./run.sh
```

#### Method 4: Universal Direct Java CLI (Works Everywhere)
If you prefer not using any shell script, standard Java commands work across any operating system:
```bash
# Compile everything directly into bin/
javac -d bin -encoding UTF-8 src/main/java/com/ppms/*/*.java src/test/java/com/ppms/*.java

# Run test suite
java -cp bin com.ppms.TestRunner

# Run interactive application
java -cp bin com.ppms.app.Main
```

---

## 7. Comprehensive User Guide (How to Use PPMS)

When launched, PPMS displays the master navigation menu:

```
================================================================================
                    PRODUCT PRICE MANAGEMENT SYSTEM (PPMS)                      
                     Core Java Desktop Terminal Application                     
================================================================================
 [1]  Product Catalog Management   (Add, View, List, Update, Archive, Restore)
 [2]  Price Management & History   (Propose Price Change, History Audit Trail)
 [3]  Historical Price Analytics   (Spike Analysis, Price Drops, Trends)
 [4]  Financial & Margin Analytics (Valuation, Margins, Below-Cost Alerts)
 [5]  Advanced Search & Filtering  (Multi-criteria Search, Range Filter)
 [6]  Operational Reports          (Catalog, Margins, Audit Trail Reports)
 [7]  Data Import & Export (CSV)   (RFC-4180 Batch Import / Export Engine)
 [8]  Backup & Snapshot Recovery   (Snapshot Backups, Restore Points)
 [9]  System Settings & Metadata   (Thresholds, Currency Symbol, ANSI Theme)
 [0]  Exit Application
================================================================================
Select Option [0-9]:
```

### Module 1: Product Catalog Management
- **Add Product**: Prompts for Name, Category, Description, Cost Price, Selling Price, and Initial Stock. Validates that selling price is non-negative and names are not blank. Automatically assigns a permanent ID (e.g., `PRD-1001`).
- **View Product Details**: Displays complete operational details: Cost, Selling Price, Margin, Markup %, Stock Status, Creation Timestamp, and Last Update Timestamp.
- **List All Active Products**: Renders tabular listing with pagination support and dynamic column sorting (by ID, Name, Price, Margin, or Stock).
- **Update Product Details**: Allows selective in-place editing of Name, Category, Description, or Stock Quantity without altering pricing history.
- **Archive Product (Soft-Delete)**: Safeguards historical audit integrity by marking products as `ARCHIVED` rather than deleting rows, preserving foreign-key references in price history.
- **Restore Archived Product**: Reactivates an archived product back into the active catalog.

### Module 2: Price Management & History
- **Propose Price Change**: Interactive pricing governance workflow described in Section 5.4. Displays before-and-after comparison, profit margin impact, below-cost warnings, mandatory reason selection, and confirmation prompt.
- **View Product Price History**: Displays complete chronological price evolution for any product, showing old price, new price, absolute change, percentage change, timestamp, and justification reason.
- **View All Recent Price Changes**: Global audit trail showing latest price movements across all products.

### Module 3: Historical Price Analytics
- **Outlier & Spike Analysis**: Identifies products with the highest single price increases and steepest price drops.
- **Date-Range Price Filter**: Audits all price changes enacted between two user-defined dates (`YYYY-MM-DD`).
- **Reason-Based Analytics**: Filters price changes by commercial trigger (e.g., all changes caused by `Supplier price increase` or `Promotional discount`).

### Module 4: Financial & Margin Analytics
- **Live Inventory Valuation**: Computes total stock valuation at Cost Price vs. total valuation at Current Selling Price.
- **Gross Profit Margin Distribution**: Calculates aggregate potential profit and overall portfolio gross margin %.
- **Below-Cost Price Warning**: Automatically scans catalog and alerts administrators to products currently selling below cost price ($P_{\text{selling}} < P_{\text{cost}}$).
- **Margin Champions & Laggards**: Lists the top 5 highest-margin products and bottom 5 lowest-margin products.
- **Statistical Benchmarks**: Computes minimum, maximum, average, and true mathematical median price across active products.

### Module 5: Advanced Search & Filtering
- **Keyword Search**: Case-insensitive substring search matching across Product Name, Category, Description, and Product ID.
- **Price Range Filter**: Filters products within user-specified minimum and maximum price boundaries.
- **Stock Status Filter**: Isolates in-stock items vs. out-of-stock items or low-stock items.

### Module 6: Operational Reports
- **Catalog Summary Report**: Formatted operational report of all active products with financial totals.
- **Margin & Profitability Report**: Detailed breakdown of profit margin amount and percentage for every product.
- **Price Audit Log Report**: Complete printable audit trail of historical price adjustments.
- **Low-Stock Alert Report**: Targets inventory items below the configured safety threshold.

### Module 7: Data Import & Export (CSV)
- **Export Catalog to CSV**: Generates an RFC-4180 compliant CSV export in `data/exports/` with timestamped filename.
- **Batch Import from CSV**: Imports products from external CSV files. Performs strict line-by-line validation, reports malformed lines with exact row numbers and error reasons, skips invalid rows, and persists all valid products atomically.

### Module 8: Backup & Snapshot Recovery
- **Create Snapshot Backup**: Creates a complete timestamped copy of all system data files (`products.csv`, `price_history.csv`, `metadata.csv`) in `data/backups/backup_YYYYMMDD_HHMMSS/`.
- **Restore from Backup**: Lists available recovery snapshots, creates an automatic safety backup of current state, and restores the selected snapshot cleanly into `data/`.

### Module 9: System Settings & Metadata
- **Low-Stock Threshold**: Configurable safety stock alert level (default: 10 units).
- **Currency Symbol**: Choose between `Rs. `, `₹`, `$`, `EUR`, or any custom currency prefix.
- **ANSI Color Theme**: Toggle ANSI terminal colors ON or OFF for maximum readability across various terminal emulators.

---

## 8. CSV Data Schemas & Crash-Resilient Persistence

### 8.1 Product Records (`data/products.csv`)
| Column | Java Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `productId` | `String` | `PRD-1001` | Permanent unique identifier |
| `name` | `String` | `Ergonomic Wireless Mouse` | Product name (quoted if contains comma) |
| `category` | `String` | `Peripherals` | Product category classification |
| `description` | `String` | `2.4GHz optical mouse` | Optional product description |
| `costPrice` | `BigDecimal` | `450.00` | Normalized procurement cost |
| `sellingPrice` | `BigDecimal` | `699.00` | Current active selling price |
| `quantity` | `int` | `35` | Physical units in inventory |
| `status` | `ProductStatus` | `ACTIVE` | Lifecycle state (`ACTIVE` / `ARCHIVED`) |
| `createdAt` | `LocalDateTime` | `2026-10-22T10:00:00` | ISO-8601 initial creation timestamp |
| `updatedAt` | `LocalDateTime` | `2026-10-22T11:30:00` | ISO-8601 last modification timestamp |

### 8.2 Price Audit History (`data/price_history.csv`)
| Column | Java Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `historyId` | `String` | `HST-1001` | Permanent unique audit entry ID |
| `productId` | `String` | `PRD-1001` | Foreign key referencing product record |
| `oldPrice` | `BigDecimal` | `699.00` | Selling price immediately prior to change |
| `newPrice` | `BigDecimal` | `749.00` | Approved new selling price |
| `changedAt` | `LocalDateTime` | `2026-10-22T11:30:00` | Exact ISO-8601 timestamp of modification |
| `reason` | `String` | `Supplier price increase` | Mandatory business justification |

### 8.3 System Metadata (`data/metadata.csv`)
Stores key-value system settings and sequence markers:
```csv
key,value
currency_symbol,Rs.
low_stock_threshold,10
next_product_id,1007
next_history_id,1012
```

---

## 9. Financial & Analytical Calculation Rules

All monetary calculations use `java.math.BigDecimal` with explicit scale and `RoundingMode.HALF_UP`. Floating-point primitives are prohibited.

### 9.1 Gross Profit Margin Amount
$$\text{Margin} = P_{\text{selling}} - P_{\text{cost}}$$

### 9.2 Gross Profit Margin Percentage
$$\text{Margin \%} = \begin{cases} 
\left( \frac{P_{\text{selling}} - P_{\text{cost}}}{P_{\text{selling}}} \right) \times 100 & \text{if } P_{\text{selling}} > 0 \\ 
0.00\% & \text{if } P_{\text{selling}} = 0 
\end{cases}$$

### 9.3 Markup Percentage
$$\text{Markup \%} = \begin{cases} 
\left( \frac{P_{\text{selling}} - P_{\text{cost}}}{P_{\text{cost}}} \right) \times 100 & \text{if } P_{\text{cost}} > 0 \\ 
0.00\% & \text{if } P_{\text{cost}} = 0 
\end{cases}$$

### 9.4 Price Change Difference and Percentage
$$\Delta P = P_{\text{new}} - P_{\text{old}}$$

$$\% \Delta P = \begin{cases} 
\left( \frac{P_{\text{new}} - P_{\text{old}}}{P_{\text{old}}} \right) \times 100 & \text{if } P_{\text{old}} > 0 \\ 
\text{Optional.empty() (Rendered as "N/A")} & \text{if } P_{\text{old}} = 0 
\end{cases}$$

### 9.5 Median Price Calculation
For an ordered set of active selling prices $S = \{p_1, p_2, \dots, p_n\}$:
$$\text{Median} = \begin{cases} 
p_{\frac{n+1}{2}} & \text{if } n \text{ is odd} \\ 
\frac{p_{\frac{n}{2}} + p_{\frac{n}{2} + 1}}{2} & \text{if } n \text{ is even} 
\end{cases}$$

---

## 10. Automated Verification & Test Suite

PPMS includes a fully automated, zero-dependency test suite located in `src/test/java/com/ppms/`. It executes all test cases without external libraries:

```cmd
test.bat
```

### Test Suite Execution Summary
```
================================================================================
       PRODUCT PRICE MANAGEMENT SYSTEM (PPMS) - AUTOMATED TEST SUITE            
================================================================================
Running Product Tests...
  -> All Product Tests Passed!
Running Pricing Tests...
  -> All Pricing Tests Passed!
Running Persistence Tests...
  -> All Persistence Tests Passed!
Running Analytics Tests...
  -> All Analytics Tests Passed!
Running Import/Export Tests...
  -> All Import/Export Tests Passed!
Running End-to-End Integration Workflow Test...
  -> End-to-End Workflow Verification Successfully Passed!
================================================================================
TEST EXECUTION COMPLETED: 6 of 6 test suites passed (100% Success).
================================================================================
```

### Coverage Matrix
1. **ProductTest**: Validates entity creation, boundary values, zero price handling, and field updates.
2. **PricingTest**: Verifies price change proposals, percentage calculations, division-by-zero safeguards, and reason auditing.
3. **PersistenceTest**: Verifies atomic safe-write mechanics, recovery from `.bak`, RFC-4180 quotes/commas, and malformed row recovery.
4. **AnalyticsTest**: Verifies inventory valuation, gross margins, markup %, below-cost alerts, and median calculation on both odd and even dataset lengths.
5. **ImportExportTest**: Tests CSV export format and batch import with invalid row skipping.
6. **EndToEndIntegrationTest**: Executes a complete 11-step integration sequence simulating a real-world operator workflow from product registration to price revision, backup, and restore.

---

## 11. Known Limitations & Future Scope

### 11.1 Known Architectural Limitations
- **Single-Process File Locking**: Local flat CSV files lack distributed ACID locks; multiple independent terminal processes concurrently modifying the same CSV file simultaneously is not supported.
- **Non-Distributed Scale**: CSV persistence is optimized for local small-to-medium business catalogs (up to ~50,000 products).

### 11.2 Future Enhancements (Academic Roadmap)
- Implementation of multi-user role-based authentication (Admin, Manager, Auditor).
- Introduction of an embedded relational database engine (e.g., H2 or SQLite via JDBC) as an alternative repository implementation.
- Export capabilities for formatted PDF reports and JSON data interchange.

---

**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
