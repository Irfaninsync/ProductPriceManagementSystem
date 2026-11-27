> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# User Manual & Operator Guide — PPMS v2.0

## 1. Installing & Launching PPMS on Windows

### Option A: Packaged Standalone App (Recommended for Business Owners)
1. Build or copy the `dist/PPMS/` folder to any location on your Windows 10/11 PC (e.g., `C:\Program Files\PPMS` or `C:\PPMS`).
2. Double-click **`PPMS.exe`** (or run `packaging\create-desktop-shortcut.ps1` once to put an icon on your Desktop).
3. PPMS starts silently in the background, initializes your local SQLite ledger in `%LOCALAPPDATA%\PPMS\ppms.db`, adds a **P** icon to your Windows System Tray, and opens PPMS in your default browser.
4. No Java installation, terminal window, or internet connection is required.

### Option B: Running from Source (Developers)
- Compile & launch: `./run.ps1` (PowerShell) or `run.bat` (Command Prompt).
- Silent desktop launch without terminal: double-click `packaging/launch-ppms.vbs`.
- Legacy terminal CLI mode: `java -cp "bin;lib/*" com.ppms.app.Main --cli`.

---

## 2. Managing Your Business Data

### Dashboard
- Displays real-time totals for active products, categories, inventory cost & retail value, average/median price, risk alerts (below-cost or low-stock), category valuations, price band overview, recently updated prices, and recent activity.
- On a brand-new install, the database starts clean with zero fake business records. You can immediately **Add First Product**, **Import from CSV**, migrate a legacy `data/products.csv`, or optionally click **Load Sample Catalog** for evaluation.

### Products & Product Details
- Use **+ Add Product** in the top bar or **Products** view to add items (Cost Price, Selling Price, Stock Quantity, Category, Description). A live Unit Margin preview highlights if a selling price is below cost.
- Filter by category, Active/Archived status, price bounds, below-cost items, or low-stock items; sort by name, price, stock, ID, or last updated date.
- Click any product name or ID to open **Product Details** showing financial ratios, chronological price trend chart, and full price history.
- Use **Archive** to retire a seasonal/discontinued item while preserving its history, or **Delete** (with confirmation) to permanently remove an item.

### Price Management
- **Single Price Update:** Select a product, enter the new selling price and business reason, click **Preview Price Comparison** to inspect the exact price delta and margin impact, then click **Confirm & Apply Change**.
- **Bulk Percentage Revaluation:** Apply a percentage increase/decrease across all active products or a single category, review the tabular preview, and confirm. PPMS automatically creates a safety backup before applying bulk price updates.
- **Price Change History:** Filter audit logs by Product ID, date range, or reason keyword, and inspect the largest historical price increase and decrease.

### Categories
- Create, rename, and delete categories. Renaming a category automatically updates all assigned products. Deleting a non-empty category prompts you to safely reassign its products to another category.

### Reports, CSV Export & Print / PDF
- Open **Reports & Analytics** to view **Product Catalogue**, **Category Summary**, **Price Comparison**, **Price Change History**, and **Inventory Valuation & Risks**.
- Click **Export CSV** to download spreadsheets, or **Print / Save as PDF** to save formatted reports using Windows Print-to-PDF.

### Backups, Restore & Closing PPMS
- Open **Settings & Data** to configure Business Name, Currency Symbol (`₹`, `Rs.`, `$`, `€`, `£`), and Low-Stock Threshold.
- Click **Create Backup Now** (or **Quick Backup** in the sidebar) to save a snapshot `.db` file inside `%LOCALAPPDATA%\PPMS\backups\`.
- To restore a backup, click **Restore** next to any snapshot—PPMS verifies file integrity first and automatically creates a pre-restore safety snapshot of your current data before switching.
- Click **Exit Application** in the sidebar (or right-click the **P** tray icon -> **Quit PPMS**) to shut down safely.

---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
