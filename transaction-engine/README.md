# Ankit Bank Full-Stack Platform

Welcome to the **Ankit Bank** ecosystem. This complete project consists of a fault-tolerant payment processing backend and a clean, natively integrated Android mobile application. 

The architecture is split into two primary components:

---

## 1. Nexus Transaction Engine (Backend)
Located in the `transaction-engine/` directory.

This module implements a robust, exactly-once payment processing system that guarantees safe money movement across distributed services using a double-entry ledger.

### Key Features:
- **Exactly-Once Semantics:** Prevents duplicate debits/credits using an advanced idempotency layer.
- **Double-Entry Ledger:** Ensures total debit strictly equals total credit; no money is accidentally created or destroyed.
- **State Machine Architecture:** Handles partial failures, retries, and concurrent transaction requests deterministically.
- **Technology Stack:** Java 22, Spring Boot, Virtual Threads, SQLite.

---

## 2. Ankit Bank Mobile App (Frontend)
Located in the `transaction/` directory.

The frontend is a clean, native Android application built using Kotlin and XML layouts representing the user-facing side of the Nexus Transaction Engine. It follows a direct 3-page structural flow:

### App Flow:
1. **Login (`LoginActivity`)**: The secure entry point, verifying the user's mobile number.
2. **Register (`RegisterActivity`)**: The portal to onboard new users to Ankit Bank.
3. **Dashboard (`DashboardActivity`)**: The core financial hub where users can:
   - Check their real-time `Available Balance`.
   - Access their unique User `QR Code` for receiving payments.
   - Use the `Scan to Pay` functionality to initiate a secure transaction.

---

## Getting Started

### To run the Backend:
1. Navigate to the `transaction-engine` directory.
2. Ensure you have Java 22+ and Maven installed.
3. Run `mvn spring-boot:run` to launch the API and database environment.

### To build the Mobile App:
1. Open the `transaction` directory in Android Studio.
2. Sync the Gradle project files.
3. Build and launch the APK onto an Android Emulator (`./gradlew assembleDebug`).

---

**Author:** Ankit Ghosh
**Project Type:** Comprehensive Full-Stack Banking Simulation
