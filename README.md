> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Product Price Management System (PPMS)

> A Local-First, Enterprise-Grade Business Application for Product Catalogue Control, Pricing Strategy, Immutable Price History, Category Management, and Financial Analytics.

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/Database-Local%20SQLite%20(WAL)-blue.svg)]()
[![Mode](https://img.shields.io/badge/Mode-Local--First%20Single%20PC-green.svg)]()
[![Tests](https://img.shields.io/badge/Tests-9%2F9%20Passed%20(100%25)-brightgreen.svg)]()

---

## 1. Overview & Local-First Design

PPMS is designed for individual business owners, shopkeepers, retailers, and wholesalers who manage inventory and pricing on a **single Windows computer** without cloud subscriptions, external hosting, or internet dependency.

### How PPMS Works
1. **Launch Locally:** Double-clicking `PPMS.exe` (or `packaging/launch-ppms.vbs`) starts the local Java engine quietly on your computer.
2. **Loopback Interface (`127.0.0.1`):** Java serves the browser-based business interface exclusively on localhost (`127.0.0.1`)—never exposed to the internet or local network.
3. **Core Java Business Engine:** All product rules, `BigDecimal` financial arithmetic, validations, category integrity, pricing proposals, and CSV import/export logic execute inside Java.
4. **Persistent SQLite Database:** Business records are saved atomically in `%LOCALAPPDATA%\PPMS\ppms.db` (outside temporary or installation folders) so data survives app closes, Windows restarts, and application updates.

*(The original Core Java terminal interface remains preserved and accessible via `--cli`.)*

---

## 2. Technology Stack

- **Core Business Engine & Server:** Pure Java SE 17 (`jdk.httpserver.HttpServer`, `java.sql`, `java.desktop`, `BigDecimal` with `RoundingMode.HALF_UP`).
- **Database:** Embedded **SQLite 3** (`lib/sqlite-jdbc-3.46.1.3.jar`) with WAL journaling, foreign keys enabled, parameterized SQL queries, and fixed-point minor-unit monetary storage.
- **Local Frontend:** HTML5 + CSS3 + modular Vanilla ES6+ JavaScript, locally bundled `Inter` & `IBM Plex Mono` WOFF2 fonts, and crisp SVG charts—100% offline with zero external CDN calls.
- **Standalone Windows Packaging:** Built with JDK `jpackage` into `dist/PPMS/PPMS.exe` including a bundled Java runtime so end users do not need Java installed.

---

## 3. Application Modules

- **Dashboard:** Real-time KPIs (active products, categories, inventory retail & cost valuation, gross margin %, average/median price), below-cost & low-stock alerts, category valuation breakdown, price overview chart, recently added products, recently updated prices, and live activity ledger.
- **Product Management:** Add (auto `PRD-xxxx` or custom ID), edit details/cost/stock, search across ID/name/category/description, filter by category/status/price range/risk flags, sort deterministically, archive/restore, and confirmed permanent deletion.
- **Price Management:** Pre-commit single price adjustment workbench (old vs. new price, ₹/`%` delta, margin impact, below-cost warning, mandatory reason), bulk percentage revaluation with full preview + pre-change snapshot backup, and searchable price history audit trail.
- **Category Management:** Create, rename (propagates automatically to linked products), search, view category valuations, and safely delete categories (with guided product reassignment).
- **Product Details:** Complete SKU specification, unit economics, chronological SVG price trend chart, and audit trail.
- **Reports & Analytics:** Live reports for Product Catalogue, Category Summary, Price Comparison, Price Change History, and Inventory Valuation & Risks—each with RFC-4180 CSV export and Print / Save as PDF (`@media print`).
- **Settings & Data Management:** Business profile, currency symbol (`₹`, `Rs.`, `$`, `€`, `£`, custom), low-stock threshold, database path & health readout, one-click SQLite snapshot backups (`VACUUM INTO`), integrity-checked restore (with automatic pre-restore safety snapshot), validated CSV batch import, and optional legacy `data/products.csv` migration.

---

## 4. Quick Start (Developers & Evaluators)

### Prerequisites
- Java Development Kit (JDK) 17 or higher (includes `javac`, `java`, `jar`, `jpackage`).

### Compile & Run Automated Tests
**Windows PowerShell:**
```powershell
./build.ps1
java -cp "bin;lib/*" com.ppms.TestRunner
```

**Windows Command Prompt:**
```cmd
test.bat
```

**Linux / macOS:**
```bash
bash compile.sh
java -cp "bin:lib/*" com.ppms.TestRunner
```

### Launch Application
- **Desktop Browser Mode (Default):**
  - Windows PowerShell: `./run.ps1`
  - Windows CMD: `run.bat`
  - Silent Windows Launcher (no terminal window): double-click `packaging/launch-ppms.vbs`
- **Legacy Terminal CLI Mode:**
  - `java -cp "bin;lib/*" com.ppms.app.Main --cli`

---

## 5. Standalone Windows Packaging & Installation

To package PPMS as a self-contained Windows application with its own bundled Java runtime (no Java installation required on the owner's PC):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File packaging\package-win.ps1
```

This creates `dist\PPMS\PPMS.exe`. To create a Desktop shortcut:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File packaging\create-desktop-shortcut.ps1
```

### Data & Backup Locations
- **Database File:** `%LOCALAPPDATA%\PPMS\ppms.db`
- **Snapshots Directory:** `%LOCALAPPDATA%\PPMS\backups\`
- **Logs & Runtime Lock:** `%LOCALAPPDATA%\PPMS\logs\`, `ppms.lock`, `ppms.port`
- *(Portable / test override supported via `-Dppms.data.dir=...` or `PPMS_DATA_DIR`.)*

---

## 6. Automated Test Suite (9/9 Passing)
1. `ProductTest` — Domain validation, duplicate rejection, archive/restore, search & filter
2. `PricingTest` — Proposals, percentage change math, cancellation isolation, zero-price safety
3. `PersistenceTest` — RFC-4180 CSV parsing/escaping, malformed row resilience, CSV backups
4. `AnalyticsTest` — Inventory valuation, gross margin/markup %, median prices, risk alerts
5. `ImportExportTest` — CSV batch import rejections and round-trip fidelity
6. `EndToEndIntegrationTest` — Full CSV lifecycle across simulated restart
7. `CategoryServiceTest` — Category CRUD, duplicate guards, rename propagation, deletion reassignment
8. `SqlitePersistenceTest` — SQLite WAL CRUD, restart persistence, `VACUUM INTO` backup & corrupted backup safety, `JsonCodec`
9. `LocalApiIntegrationTest` — Loopback HTTP API, CSRF token security, static offline asset serving, restart & restore

---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
