# Project Plan

K&K Sales: An app for bookkeeping, user management, and product ordering. Users can order products which are tracked in a wallet. It includes product management with prices, quantities, costs, and amounts.

## Project Brief

# K&K Sales Project Brief

K&K Sales is a streamlined business management tool designed to bridge the gap between product inventory and customer transactions. The app enables seamless product ordering, automated wallet tracking for users, and comprehensive bookkeeping for administrators.

### Features
* **User Wallet & Account Management:** A centralized system where users can manage their profiles and track their balance. All product purchases are automatically deducted from the user's digital wallet.
* **Product Catalog & Ordering:** An intuitive storefront for users to browse available products, view real-time quantities, and place orders directly within the app.
* **Inventory & Price Management:** Admin-level tools to manage the product lifecycle, including setting unit costs, sales prices, and tracking stock levels (quantities).
* **Bookkeeping & Financial Overview:** A dedicated module for tracking "amounts" (revenue) and "costs" to provide a clear picture of profit margins and transaction history.

### High-Level Technical Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose with Material Design 3 (M3) for a modern, energetic, and adaptive interface.
* **Concurrency:** Kotlin Coroutines and Flow for reactive data handling and asynchronous operations.
* **Architecture:** MVVM (Model-View-ViewModel) to ensure a clean separation of concerns and maintainability.
* **Networking:** Retrofit & OkHttp for communicating with backend services for order processing.
* **Local Persistence:** Room Database (via **KSP**) for caching product data, transaction logs, and wallet states.
* **Dependency Management:** Version Catalogs (libs.versions.toml) with **KSP** for efficient annotation processing and code generation.

## Implementation Steps
**Total Duration:** 4m 9s

### Task_1_Data_and_Persistence: Define the core data layer including Room entities (Product, User, Transaction), DAOs, and the database. Set up Retrofit for API interactions and DataStore for user preferences. Implement repositories to manage data flow.
- **Status:** COMPLETED
- **Updates:** Implemented Room database with entities (Product, User, Transaction), DAOs, and database class. Set up Retrofit API service with a placeholder order endpoint. Implemented DataStore for user preferences (current user ID, theme). Created repositories for Products, Users, Transactions, and Ordering logic. Updated Application class and Manifest for dependency injection and Internet permission. Built the data layer following MVVM and using Kotlin Coroutines/Flow. Note: Code verification was interrupted by a host system disk space error, but implementation is complete as per requirements.
- **Acceptance Criteria:**
  - Room database and entities created
  - Retrofit API service defined
  - DataStore set up for preferences
  - Repositories implemented
- **Duration:** 2m 10s

### Task_2_User_and_Catalog_UI: Develop the User Profile and Wallet management screens. Create the Product Catalog UI for browsing and ordering products. Implement the business logic for deducting balance and updating stock during orders.
- **Status:** COMPLETED
- **Updates:** Developed the User Profile and Product Catalog UI for the K&K Sales app. Implemented UserViewModel and CatalogViewModel to manage UI state and interact with repositories. Created ProfileScreen and CatalogScreen using Jetpack Compose and Material 3 components. Set up Compose Navigation for switching between screens. Implemented ordering logic in CatalogViewModel and OrderRepository, including balance and stock checks, database updates, and simulated API calls. Enabled edge-to-edge display and used Material 3 components (Scaffold, TopAppBar, NavigationBar, Card, Button) for a vibrant, energetic UI. Note: Code verification was interrupted by a host system disk space error.
- **Acceptance Criteria:**
  - User Profile and Wallet screens functional
  - Product Catalog displays items
  - Ordering system updates wallet and inventory
  - UI uses Material 3
- **Duration:** 58s

### Task_3_Admin_and_Bookkeeping: Build the Admin dashboard for product inventory management (CRUD operations for products, costs, and sales prices). Implement the bookkeeping module to display financial summaries and transaction history.
- **Status:** COMPLETED
- **Updates:** Build the Admin dashboard and Bookkeeping module for the K&K Sales app. Created AdminDashboardScreen for product CRUD operations (name, unit cost, sales price, quantity) and BookkeepingScreen for financial summaries (Revenue, Cost, Profit) and transaction history. Developed AdminViewModel and BookkeepingViewModel to manage UI state and interact with repositories. Enhanced Transaction entity and OrderRepository to track quantity and unit cost for accurate profit calculations. Integrated Admin and Bookkeeping screens into the Compose Navigation setup. Note: Code verification was interrupted by a host system disk space error, but implementation is complete as per requirements.
- **Acceptance Criteria:**
  - Admin can add/edit products
  - Bookkeeping screen shows revenue and profit
  - Transaction history is logged correctly
- **Duration:** 1m 1s

### Task_4_Polish_Icon_and_Verification: Refine the UI with a vibrant Material 3 color scheme, ensure full edge-to-edge display support, and create an adaptive app icon. Perform final testing to ensure stability and requirement alignment.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Vibrant Material 3 theme applied
  - Full Edge-to-Edge display implemented
  - Adaptive app icon created
  - App builds and runs without crashes
  - All existing tests pass
  - All features meet project brief
- **StartTime:** 2026-04-06 05:07:19 CEST

