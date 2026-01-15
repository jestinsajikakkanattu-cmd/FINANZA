# FINANZA - Personal Budget Tracker

FINANZA is a robust, offline-first Android application built with modern Android development practices (Jetpack Compose, Room, DataStore, and MVVM). It allows users to track their daily transactions, view running balances, and manage their financial data securely.

## 🚀 Key Features

### 1. Transaction Management
*   **Add/Edit Transactions:** Track Income and Expenses with fixed categories, date, and mandatory notes.
*   **Running Balance:** Every transaction row displays the cumulative balance up to that point.
*   **Grouping & Sorting:** Transactions are grouped by date. Latest transactions are displayed at the bottom for chronological reading.
*   **Currency:** Fully localized to use the Rupee symbol (₹).
*   **Floating Action Button:** Enhanced primary accent button with shadow and high contrast.

### 2. Validations & Constraints
*   **Amount Limit:** Maximum allowed transaction amount is restricted to **10 Lakh (₹1,000,000)**.
*   **Mandatory Notes:** Transactions cannot be saved without a description.
*   **Note Restriction:** The "Note" field is limited to **30 characters** for UI consistency.
*   **Fixed Categories:** Structured system including Food, Fuel, Bills, Shopping, Loan, Savings, Entertainment, and Other.

### 3. Advanced Financial Analytics
*   **Dual View Toggle:** Switch between **Expense Breakdown** and **Income vs Expense** views.
*   **Donut Charts:** Dynamic visualization using a consistent, category-specific color palette.
*   **Category Drill-Down:** Tap any category to view a detailed list of transactions grouped by specific notes/descriptions.
*   **Aggregation:** Automatically groups smaller expenses under "Other" to keep charts clean.

### 4. Financial Reports (PDF)
*   **Monthly Filtering:** Choose specific months to generate professional financial summaries.
*   **Styled PDF:** Includes summary tables and color-coded transaction logs.
*   **Dedicated Folder:** Files are saved to `Documents/Finanza/`.

### 5. User Profile & Data Management
*   **Persistent Profile:** Edit and save your professional details (Name, Job, Email, etc.) using `Jetpack DataStore`.
*   **Local Backup & Restore:** Export entire history to JSON and restore securely with built-in file validation and duplicate prevention.
*   **Danger Zone:** Secure "Clear All Data" feature with double-confirmation dialogs.

---

## 🛠 Tech Stack & Dependencies

*   **UI:** Jetpack Compose with Material 3.
*   **Navigation:** Compose Navigation with nested graphs for Tab Bar and Drill-Downs.
*   **Database:** Room Database for local transaction storage.
*   **Persistence:** Preferences DataStore for user profile settings.
*   **PDF Logic:** `com.itextpdf:itext7-core`.
*   **JSON Serialization:** `com.google.code.gson:gson`.

---

## 📂 Project Structure for Maintainers

*   **`com.example.budgettracker.data`**: Room entities, DAO, and the `TransactionCategory` color-mapping enum.
*   **`com.example.budgettracker.ui`**:
    *   **`screens/`**: Home, Analytics, CategoryBreakdown, Profile, Reports, Add/Edit.
    *   **`theme/`**: Typography (readability optimized), Colors, and Theme definitions.
    *   **`BudgetViewModel`**: The central logic hub handling state, DB, JSON, and DataStore.

---

## ⚠️ Important Developer Notes

1.  **Package Name:** The application ID is `com.jestApp.finanza`, but the source code package is `com.example.budgettracker`.
2.  **File Permissions:** Uses `MediaStore` for safe file handling in `Documents/Finanza` on modern Android versions.
3.  **Offline-First:** No cloud or Firebase used. All data is local.

## 🏁 How to Run
1.  Open the project in Android Studio.
2.  Sync Gradle.
3.  Run on an emulator or device (API 24+).
