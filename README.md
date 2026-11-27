> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Product Price Management System (PPMS)

> A Local-First, Enterprise-Grade Business Application for Product Catalogue Control, Pricing Strategy, Immutable Price History, Category Management, and Financial Analytics.

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/Database-Local%20SQLite%20(WAL)-blue.svg)]()
[![Mode](https://img.shields.io/badge/Mode-Local--First%20Single%20PC-green.svg)]()
[![Offline](https://img.shields.io/badge/Network-100%25%20Offline%20%26%20Private-teal.svg)]()
[![Tests](https://img.shields.io/badge/Tests-9%2F9%20Passed%20(100%25)-brightgreen.svg)]()

---

## 1. Why PPMS Is Built to Be Effortless

PPMS is designed for individual **shopkeepers, retailers, wholesalers, and small business owners** who want commercial-grade software on a single Windows computer—without cloud subscriptions, complex database setup, or technical commands.

- **Zero Setup & Zero Internet Required:** No MySQL/PostgreSQL installation, no Docker, no Maven/Gradle, and no external hosting. Everything runs locally on your own computer.
- **One-Click Desktop Experience:** Double-clicking `PPMS.exe` (or `packaging/launch-ppms.vbs`) silently starts the local Java engine and opens PPMS automatically in your regular web browser (Chrome, Edge, etc.).
- **Safe & Private on Your Machine:** The local server listens strictly on `127.0.0.1` (your computer only). Your prices, margins, and inventory never leave your PC.
- **Mistake-Proof Pricing & Automatic Audit Trail:**
  - Adding/editing a product previews your profit margin live and warns you immediately if selling price drops below cost.
  - Updating any selling price shows an **Old vs. New Price Comparison** (amount, `%` change, and resulting margin) *before* committing, then records a permanent audit entry.
- **Crash-Safe Local Storage & 1-Click Backups:** All records are stored in a persistent local SQLite database (`%LOCALAPPDATA%\PPMS\ppms.db`) outside temporary or installation folders, so data survives browser closes, app restarts, and Windows reboots.

---

## 2. How to Run PPMS Locally

### A. For Business Owners & Everyday Users (No Coding / No Terminal)
*(Once packaged into `dist\PPMS\`, the target Windows 10/11 PC does **not** even need Java installed.)*

1. Build or copy the standalone `dist\PPMS\` folder onto your computer (see Section 5 to build it once).
2. *(Optional)* Run `packaging\create-desktop-shortcut.ps1` once to place a **PPMS - Product Price Management** icon on your Desktop.
3. **Double-click `PPMS.exe`** (or the Desktop shortcut):
   - PPMS starts quietly in the background (no black terminal window).
   - A **P** icon appears in your Windows System Tray.
   - Your default browser opens straight to the PPMS interface.
4. **When finished:** Click **Exit Application** at the bottom-left of the sidebar (or right-click the **P** tray icon → **Quit PPMS**). Your database closes cleanly with every change saved.

> **Using directly from the repo without packaging?** Just double-click **`packaging\launch-ppms.vbs`** on Windows (requires JDK 17+) to launch silently without a terminal window.

---

### B. For Developers, Teammates & Evaluators (From GitHub)
**Prerequisites:** Java Development Kit (**JDK 17 or higher** — includes `javac`, `java`, `jar`, `jpackage`).

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Irfaninsync/ProductPriceManagementSystem.git
   cd ProductPriceManagementSystem
   ```
2. **Launch the desktop browser app:**
   - **Windows PowerShell:** `./run.ps1`
   - **Windows Command Prompt (`cmd`):** `run.bat`
   - **Silent Windows Launcher:** double-click `packaging/launch-ppms.vbs`
   - **Linux / macOS:** `bash run.sh`
3. **Run all 9 automated test suites:**
   - Windows CMD: `test.bat`
   - PowerShell / Cross-platform:
     ```powershell
     ./build.ps1
     java -cp "bin;lib/*" com.ppms.TestRunner
     ```
     *(On Linux/macOS replace `;` with `:` in `-cp "bin:lib/*"`.)*
4. **Original Terminal CLI Mode (Preserved):**
   ```bash
   java -cp "bin;lib/*" com.ppms.app.Main --cli
   ```

---

## 3. Everyday User Guide (How Easy It Is Day-to-Day)

| Everyday Task | How You Do It in PPMS |
| :--- | :--- |
| **First-Time Start** | Opens to a clean, empty store—click **Add First Product**, **Import from CSV**, migrate an existing `data/products.csv`, or optionally click **Load Sample Catalog** to explore with demo items. |
| **Add or Edit Products** | Click **+ Add Product** in the top bar (or open **Products**). Enter cost price, selling price, quantity, and category—unit profit margin calculates live as you type. |
| **Find Anything Instantly** | Press **`Ctrl + K`** anywhere (or use the top search bar) to jump straight to any product by name or ID, or filter by category. |
| **Search, Filter & Sort Catalog** | In **Products**, filter by category, Active/Archived status, min/max price, **Below Cost**, or **Low Stock**, and sort by name, price, stock, ID, or date updated. |
| **Inspect One Product** | Click any product name or `PRD-xxxx` ID to view unit economics, stock valuation, a price trend chart, and its full historical price log. |
| **Update Single or Bulk Prices** | Open **Price Management**: preview exact ₹/`%` changes and margin impact before confirming single updates, or apply a percentage increase/decrease across a category/catalog (with automatic pre-change safety backup). |
| **Manage Categories** | Open **Categories** to create, search, rename (updates all linked products automatically), or delete categories (with guided reassignment if products exist). |
| **Export Spreadsheets & PDFs** | Open **Reports & Analytics** for **Product Catalogue**, **Category Summary**, **Price Comparison**, **Price Change History**, and **Inventory Valuation & Risks**—each with **Export CSV** and **Print / Save as PDF**. |
| **Back Up & Restore Data** | Click **Quick Backup** in the sidebar anytime, or visit **Settings & Data** to restore any verified snapshot (PPMS creates a safety backup of your current data automatically before restoring). |
| **Customize Store Settings** | In **Settings & Data**, set your **Business Name**, **Currency Symbol** (`₹`, `Rs.`, `$`, `€`, `£`, or custom), **Low-Stock Threshold**, and daily startup backups. |

---

## 4. Architecture & Technology Stack

- **Core Business Engine & Local Server:** Pure Java SE 17 (`jdk.httpserver.HttpServer`, `java.sql`, `java.desktop`, `BigDecimal` with `RoundingMode.HALF_UP`).
- **Database:** Embedded **SQLite 3** (`lib/sqlite-jdbc-3.46.1.3.jar`) running in WAL mode (`PRAGMA foreign_keys = ON`, parameterized `PreparedStatement` queries, exact 2-decimal `INTEGER` minor-unit monetary columns).
- **Local Frontend:** HTML5 + CSS3 + modular Vanilla ES6+ JavaScript, locally bundled `Inter` & `IBM Plex Mono` WOFF2 fonts, and custom SVG charts—100% offline with zero external CDN requests.
- **Security & Reliability:** Loopback-only binding (`127.0.0.1`), `Host`/`Origin` validation, per-launch `X-PPMS-Token` CSRF protection, single-instance file lock (`ppms.lock`), atomic SQLite transactions for price updates + audit logs, and `VACUUM INTO` snapshot backups verified via `PRAGMA integrity_check`.

---

## 5. Standalone Windows Packaging & Storage Locations

### Build Standalone Windows Bundle (`PPMS.exe` + Bundled JRE)
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File packaging\package-win.ps1
```
This outputs `dist\PPMS\PPMS.exe`, which you can copy to any Windows 10/11 machine and run without installing Java.

### Create Desktop Shortcut
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File packaging\create-desktop-shortcut.ps1
```

### Persistent Local Data Locations
- **Database File:** `%LOCALAPPDATA%\PPMS\ppms.db`
- **Backup Snapshots:** `%LOCALAPPDATA%\PPMS\backups\`
- **Exports & Logs:** `%LOCALAPPDATA%\PPMS\exports\`, `%LOCALAPPDATA%\PPMS\logs\`
- *(Portable / sandbox override supported via `-Dppms.data.dir=...` or `PPMS_DATA_DIR`.)*

---

## 6. Automated Test Suite (9/9 Passing)

Run `java -cp "bin;lib/*" com.ppms.TestRunner` (or `test.bat`):

1. `ProductTest` — Domain validation, duplicate ID rejection, archive/restore, search & filter
2. `PricingTest` — Proposals, percentage change math, cancellation isolation, zero-price safety
3. `PersistenceTest` — RFC-4180 CSV parsing/escaping, malformed row resilience, CSV backups
4. `AnalyticsTest` — Inventory valuation, gross margin/markup %, median prices, risk alerts
5. `ImportExportTest` — CSV batch import rejections and round-trip fidelity
6. `EndToEndIntegrationTest` — Full CSV lifecycle across simulated restart
7. `CategoryServiceTest` — Category CRUD, duplicate guards, rename propagation, deletion reassignment
8. `SqlitePersistenceTest` — SQLite WAL CRUD, restart persistence, `VACUUM INTO` backup & corrupted backup safety, `JsonCodec`
9. `LocalApiIntegrationTest` — Loopback HTTP API, CSRF token security, static offline asset serving, restart & restore

---

## 7. Further Documentation
- [System Architecture](docs/architecture.md)
- [Operator User Guide](docs/user-guide.md)
- [Test Plan & Verification](docs/test-plan.md)
- [UML Class Diagram](docs/class-diagram.md)
- [Requirements Matrix](docs/requirements.md)

---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
