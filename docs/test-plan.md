> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Test Plan and Verification Report — PPMS v2.0

## 1. Automated Verification Suites (`com.ppms.TestRunner`)

| Test Suite Class | Coverage | Verification Status |
| :--- | :--- | :--- |
| `ProductTest` | Product validation, duplicate ID rejection, archive/restore, search & filter | Executed via `TestRunner` |
| `PricingTest` | Price proposals, % math, cancellation safety, zero old-price handling | Executed via `TestRunner` |
| `PersistenceTest` | RFC-4180 CSV parser/writer, malformed row tolerance, CSV backups | Executed via `TestRunner` |
| `AnalyticsTest` | Inventory valuation, gross margin/markup %, median prices, risk alerts | Executed via `TestRunner` |
| `ImportExportTest` | Batch CSV import validation, row-level rejections, round-trip export | Executed via `TestRunner` |
| `EndToEndIntegrationTest` | Full 11-step CSV lifecycle across simulated restart | Executed via `TestRunner` |
| `CategoryServiceTest` | Category creation, uniqueness, rename FK propagation, delete & reassignment, summaries | Executed via `TestRunner` |
| `SqlitePersistenceTest` | SQLite WAL CRUD, restart persistence, archived price-edit guard, `VACUUM INTO` backup & corrupted backup safety, `JsonCodec` | Executed via `TestRunner` |
| `LocalApiIntegrationTest` | Loopback HTTP server (`127.0.0.1`), token security (`403`), offline static asset serving, CRUD/Pricing/Reports/CSV Import via HTTP, server restart & backup restore | Executed via `TestRunner` |

## 2. Running the Suite
```powershell
./build.ps1
java -cp "bin;lib/*" com.ppms.TestRunner
```

---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
