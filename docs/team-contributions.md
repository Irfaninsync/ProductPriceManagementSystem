> **GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)

# Team Responsibilities & Contribution Matrix — PPMS

## 1. Academic Team Structure (6 Roles)

This project was conceived and structured for a six-member academic engineering team. The formal responsibility areas and actual implementation ownership are documented below with complete academic transparency.

| Role Number | Proposed Role Title | Assigned Scope & Responsibilities | Assigned Team Member (Placeholder for Student Name) | Actual Implementation Ownership |
| :---: | :--- | :--- | :--- | :--- |
| **Role 1** | **Project Lead & System Architect** | System architecture, package structure, dependency injection, integration, cross-cutting concerns, and final release validation. | *[Student Name 1 — Project Lead]* | Project Lead |
| **Role 2** | **Product Model & Validation Engineer** | Domain models (`Product`, `ProductStatus`), validation rules (`ProductValidator`, `ValidationResult`), financial precision (`BigDecimal`). | *[Student Name 2]* | Implemented by Lead; reviewed by Role 2 |
| **Role 3** | **Product Operations & Search Specialist** | Product catalog operations (`ProductService`), multi-attribute search, deterministic sorting, and filtering logic. | *[Student Name 3]* | Implemented by Lead; tested by Role 3 |
| **Role 4** | **Pricing & Historical Audit Specialist** | Pricing proposals, calculation formulas (`PricingService`), `PriceHistory`, and financial analytics (`AnalyticsService`). | *[Student Name 4]* | Implemented by Lead; verified by Role 4 |
| **Role 5** | **Persistence & Quality Assurance Engineer** | RFC-4180 CSV engine (`CsvParser`, `CsvWriter`, `CsvFileHandler`), atomic safe writes, backup/restore, and automated test suite (`TestRunner`). | *[Student Name 5]* | Implemented by Lead; QA verified by Role 5 |
| **Role 6** | **Documentation & UI/UX Specialist** | Terminal UI (`ConsoleUI`, `TableFormatter`, `MenuController`), user guide, academic report, and viva demonstration script. | *[Student Name 6]* | Implemented by Lead; presentation prep by Role 6 |

---

## 2. Transparent Contribution Declaration

As required by software engineering academic integrity standards:
- **Architecture, Implementation & Testing**: The unified Core Java codebase, persistence layer, calculation engine, test runner, and console user interface were architected and implemented by the Project Lead.
- **Role Collaboration & Verification**:
  - Role 2 participated in defining validation boundary conditions (e.g., handling zero selling price vs negative prices).
  - Role 3 verified the search and filtering behavior with sample catalogs.
  - Role 4 audited the pricing percentage change formulas and gross margin calculations against business accounting rules.
  - Role 5 verified CSV recovery edge cases and test executions.
  - Role 6 formatted documentation, reviewed terminal layout alignment, and prepared viva defense slides.

---

## 3. Team Member Details Table (For Submission)

| Roll No. / Student ID | Student Full Name | Assigned Module / Role | Signature / Confirmation |
| :--- | :--- | :--- | :--- |
| `[ID-001]` | `[Student 1 Name]` | Project Lead & System Architect | _____________________ |
| `[ID-002]` | `[Student 2 Name]` | Product Model & Validation | _____________________ |
| `[ID-003]` | `[Student 3 Name]` | Product Operations & Search | _____________________ |
| `[ID-004]` | `[Student 4 Name]` | Pricing & Historical Audit | _____________________ |
| `[ID-005]` | `[Student 5 Name]` | Persistence & Quality Assurance | _____________________ |
| `[ID-006]` | `[Student 6 Name]` | Documentation & Demonstration | _____________________ |


---
**GitHub Repository:** [https://github.com/Irfaninsync/ProductPriceManagementSystem](https://github.com/Irfaninsync/ProductPriceManagementSystem)
