> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# User Manual & Operator Guide — PPMS

## 1. Getting Started

### 1.1 Prerequisites & Git Clone
To clone and get started immediately:
```bash
git clone https://github.com/Irfaninsync/ProductPriceManagementSystem.git
cd ProductPriceManagementSystem
```

### 1.2 System Requirements
- Java Development Kit (JDK) 17 or higher.
- Standard terminal (Windows Command Prompt, Windows PowerShell, Terminal on macOS/Linux).

### 1.2 Compiling the System
Run the provided platform script from the project root:
- **Windows Command Prompt**:
  ```cmd
  compile.bat
  ```
- **Windows PowerShell**:
  ```powershell
  ./build.ps1
  ```
- **Linux / macOS**:
  ```bash
  bash compile.sh
  ```
- **Manual Plain Javac (any platform)**:
  ```bash
  javac -d bin -encoding UTF-8 src/main/java/com/ppms/*/*.java src/test/java/com/ppms/*.java
  ```

### 1.3 Running the Application
- **Windows Command Prompt**:
  ```cmd
  run.bat
  ```
- **Windows PowerShell**:
  ```powershell
  ./run.ps1
  ```
- **Linux / macOS**:
  ```bash
  bash run.sh
  ```
- **Manual Command**:
  ```bash
  java -cp bin com.ppms.app.Main
  ```

### 1.4 Running the Automated Test Suite
- **Windows Command Prompt**:
  ```cmd
  test.bat
  ```
- **Manual Command**:
  ```bash
  java -cp bin com.ppms.TestRunner
  ```

---

## 2. Main Navigation & Workflows

### 2.1 Main Dashboard
Upon launching, the interactive dashboard automatically calculates and presents:
- Total Registered Products, Active Count, Archived Count.
- Total Inventory Units in Stock.
- Lowest, Highest, Average, and Median Selling Price.
- Number of products priced below cost (Loss-Leader alert).
- Total Inventory Valuation.
- Low-stock item count.
- Recent 3 price change events with percentage differences.

### 2.2 Product Catalog Management (Menu 1)
- **Add Product**: Prompts for custom ID (or leave blank for automated sequential `PRD-xxxx`), Name, Category, Description, Cost Price, Selling Price, and Stock Quantity.
- **Inspect Product**: Displays all financial attributes, calculated gross margins, markup %, and price change history.
- **Search**: Case-insensitive substring search across ID, name, category, and description.
- **Filter**: Filter by category, status (Active/Archived), selling price boundaries, below-cost status, and low stock.
- **Sort**: Deterministic sorting by Name, ID, Category, Selling Price, Cost Price, Stock, or Last Updated.
- **Archive & Restore**: Safely soft-archive items without destroying their price history audit records.

### 2.3 Pricing Operations (Menu 2)
- **Single Price Adjustment**:
  1. Enter product ID.
  2. Enter proposed new selling price.
  3. Enter audit reason (mandatory).
  4. Review the generated **Price Change Preview**:
     * Current Price
     * Proposed Price
     * Absolute Difference (`+` or `-`)
     * Percentage Change (`+` or `-` %)
     * Cost Price Comparison Warning (if below cost)
  5. Enter `yes` to confirm and commit, or `no` to cancel.
- **Bulk Percentage Price Adjustment**:
  1. Select scope (by category or all active products).
  2. Enter percentage adjustment (e.g. `10` for +10%, or `-5` for -5% discount).
  3. Enter reason.
  4. Inspect tabular preview of all proposed prices before any changes are made.
  5. Confirm to execute.

### 2.4 Price History & Audit Trail (Menu 3)
- View complete chronological audit trail.
- Filter audit records by specific Product ID.
- Filter audit records by Date Range (`yyyy-MM-dd`).
- Filter audit records by reason keyword.
- Inspect the Largest Historical Price Increase and Largest Historical Price Decrease.

### 2.5 Analytical Reports (Menu 4)
- **Executive Pricing & Valuation Summary**: Enterprise-wide financial health, inventory valuation at cost vs retail, and margin potential.
- **Category Performance Breakdown**: Units, total inventory cost, retail valuation, and average profit margin per category.
- **Below-Cost Loss Leader Analysis**: Identifies products whose selling price is lower than cost price.
- **Low-Stock Alert**: Identifies products requiring restocking based on the configured threshold.

### 2.6 Data Management & Backups (Menu 5)
- **Export Products / Price History to CSV**: Generates RFC-4180 compliant CSV files with full escaping.
- **Import Products from CSV**: Validates external CSV files, checks column schemas, detects duplicates, displays row-by-row rejections with line numbers, and prompts before committing valid rows.
- **Create Snapshot Backup**: Creates a timestamped directory in `data/backups/backup_yyyyMMdd_HHmmss_SSS/` containing `products.csv`, `price_history.csv`, and `metadata.csv`.
- **Restore Snapshot**: Prompts to select an available backup snapshot, verifies header integrity, creates an automatic safety snapshot of current data, and restores files.

### 2.7 Application Settings & User Guide (Menu 6)
- Configure low-stock warning threshold (default: 10).
- Configure currency symbol (`Rs. `, `₹`, `$`, `EUR`, etc.).
- Toggle ANSI terminal colors.
- Toggle clean box-drawing characters vs plain ASCII borders.
- View embedded user guide.


---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
