# BangluPay

## Offline Banking & Payment Platform

BangluPay is an offline-first banking and payment simulation built around an existing **Transaction Engine**. The goal is to demonstrate the engineering architecture behind a complete banking/payment platform that models real banking operations—such as customer onboarding, account creation, identity verification, money transfers, UPI payments, and inter-bank transactions—while keeping the complete development environment local and **Offline Simulation**.

---

## Table of Contents

- [Project Overview](#project-overview)
- [1. Transaction Engine Audit & Testing](#1-transaction-engine-audit--testing)
- [2. Banking Core & Customer Management](#2-banking-core--customer-management)
- [3. Account Management & KYC](#3-account-management--kyc)
- [4. Payment & UPI System](#4-payment--upi-system)
- [5. Inter-Bank Transfer System](#5-inter-bank-transfer-system)
- [6. Ledger, Transactions & Reconciliation](#6-ledger-transactions--reconciliation)
- [7. Security, Risk & Offline Architecture](#7-security-risk--offline-architecture)
- [8. Testing, Simulation & Banking Dashboard](#8-testing-simulation--banking-dashboard)
- [9. Android Mobile Application](#9-android-mobile-application)
- [Core Architecture](#core-architecture)
- [Offline Requirement](#offline-requirement)
- [Important Financial Invariants](#important-financial-invariants)
- [Development Order](#development-order)
- [Definition of Done](#definition-of-done)
- [Disclaimer](#disclaimer)

---

## Project Overview

The project is divided into **9 major parts**:

1. **Transaction Engine Audit & Testing**
2. **Banking Core & Customer Management**
3. **Account Management & KYC**
4. **Payment & UPI System**
5. **Inter-Bank Transfer System**
6. **Ledger, Transactions & Reconciliation**
7. **Security, Risk & Offline Architecture**
8. **Testing, Simulation & Banking Dashboard**
9. **Android Mobile Application**

The implementation should proceed sequentially. Do not start implementing later modules until the dependencies of the earlier modules have been understood and tested.

---

# 1. Transaction Engine Audit & Testing

## Objective
The existing **Transaction Engine** is the foundation of the project. The first objective is to establish whether the transaction engine actually works by inspecting it and creating a test suite. Do not modify the transaction engine unnecessarily. The payment application should consume the transaction engine through a clean interface.

## Existing Transaction Engine
Before writing the payment application, inspect:

`E:\bangluPay\transaction-engine`

## What Must Be Inspected
Determine the following components and architectures:
- Programming language
- Framework
- Project structure
- Entry point
- APIs/interfaces
- Transaction model
- Account model
- Ledger implementation
- Database/storage mechanism
- Persistence mechanism
- Transaction states
- Concurrency handling
- Idempotency support
- Failure handling
- Rollback mechanisms
- Transaction validation
- Balance calculation
- Authentication/authorization
- Existing tests
- Configuration
- Logging
- Error handling

## Transaction Test Cases
Create a test suite that verifies standard operations. For example, a successful transfer:

```text
Account A
   |
   | ₹1000
   v
Account B
```

After the transaction, balances should reflect:

```text
Balance(A) = Balance(A_initial) - ₹1000
Balance(B) = Balance(B_initial) + ₹1000
```

Required testing scenarios:
- Successful transaction
- Insufficient balance
- Invalid account
- Duplicate transaction
- Concurrent transactions
- Failed transaction
- Rollback
- Partial failure
- Negative amount
- Zero amount
- Transaction status
- Transaction history
- Persistence after restart

---

# 2. Banking Core & Customer Management

## Objective
Build the banking domain around the transaction engine to support robust customer relations.

## Core Entities
Core models should include:
- `Customer`
- `Account`
- `Bank`
- `Branch`
- `Transaction`
- `Beneficiary`
- `Payment`
- `KYCRecord`
- `UPIProfile`
- `LedgerEntry`

## Customer Management
Customer management should support:
- Customer registration
- Customer profile
- Customer ID generation
- Contact information
- Address
- Date of birth
- Account ownership
- KYC status
- Customer status
- Account relationships

### Customer Relationships
```text
Customer
   |
   +---- Savings Account
   |
   +---- Current Account
   |
   +---- UPI Profile
   |
   +---- KYC Record
```

### Customer Lifecycle
```text
REGISTERED
    ↓
KYC_PENDING
    ↓
KYC_VERIFIED
    ↓
ACTIVE
    ↓
SUSPENDED / CLOSED
```

---

# 3. Account Management & KYC

## Objective
Implement complete account lifecycle management including identity verification.

## Account Creation
Support the following account types:
- Savings account
- Current account
- Salary account
- Basic account

Account creation should include fields such as: `Account Number`, `Customer ID`, `Bank ID`, `Branch ID`, `Account Type`, `Currency`, `Balance`, `Status`, and `Created At`.

### Account States
| State       | Meaning                           |
| ----------- | --------------------------------- |
| `PENDING`   | Account creation is pending       |
| `ACTIVE`    | Account is operational            |
| `FROZEN`    | Account is temporarily frozen     |
| `SUSPENDED` | Account operations are restricted |
| `CLOSED`    | Account is permanently closed     |

## Account Deletion / Closure
Do not simply delete financial records. Account closure should be a controlled banking operation where historical transactions remain available.

```text
ACTIVE
   ↓
CLOSURE_REQUESTED
   ↓
VALIDATION
   ↓
BALANCE_SETTLED
   ↓
CLOSED
```

## Aadhaar Verification
Because the system must work without Internet access, Aadhaar verification should initially be implemented as an **Offline Simulation**, not as a real UIDAI verification system. Never store Aadhaar numbers in plaintext.

```text
Aadhaar Number
      ↓
Local Verification Service
      ↓
Mock Aadhaar Registry
      ↓
VERIFIED / FAILED
```

## PAN Verification
Similarly, PAN verification should initially use a local mock verification service. The architecture should keep the verification service behind an interface so that a real regulated verification provider could theoretically be integrated later.

```text
PAN
 ↓
Local PAN Registry
 ↓
Verification
 ↓
VALID / INVALID
```

---

# 4. Payment & UPI System

## Objective
Implement the payment layer on top of the transaction engine.

## Internal Bank Transfer
```text
Account A
    ↓
Transaction Service
    ↓
Account B
```

## UPI
Implement a simulated offline UPI ecosystem. Example VPA addresses: `ankit@banglupay` and `rahul@anotherbank`.

UPI functionality should include:
- UPI ID creation
- UPI ID lookup
- UPI PIN simulation
- Payment initiation
- Payment authorization
- Payment processing
- Payment status
- Payment history
- Refund
- Payment reversal
- Failed payment
- Duplicate payment protection

Every payment must have a unique `paymentId`, `transactionId`, and `referenceNumber`. **Idempotency** must prevent the same payment request from being processed twice.

### Payment Lifecycle
```text
INITIATED
    ↓
AUTHORIZED
    ↓
PROCESSING
    ↓
SUCCESS
```

### Failure Paths
```text
INITIATED
    ↓
FAILED
```
or:
```text
PROCESSING
    ↓
REVERSED
```

---

# 5. Inter-Bank Transfer System

## Objective
Create multiple simulated banks inside the offline environment. These are simulated institutions, not real banking integrations. Examples include `BangluPay Bank`, `State Bank Simulator`, `HDFC Simulator`, `ICICI Simulator`, and `Axis Simulator`.

### Inter-Bank Transfer Flow
```text
Bank A
Account A
   |
   | ₹5,000
   v
Inter-Bank Transfer Service
   |
   v
Bank B
Account B
```

## Supported Features
- Internal transfers
- Inter-bank transfers
- Beneficiary creation
- Beneficiary validation
- Transfer limits
- Transfer status
- Failed transfers
- Reversals
- Refunds
- Transaction references

## Simulated Banking Rails
- `UPI`
- `IMPS`
- `NEFT`
- `RTGS`

*These should be explicitly represented as offline simulations, because the real payment networks require external infrastructure and regulated access.*

---

# 6. Ledger, Transactions & Reconciliation

## Objective
The financial ledger must be treated as the source of truth, maintaining a reliable **Double-entry Ledger**.

## Double-Entry Accounting
A transaction should produce corresponding ledger entries. For a transfer:

```text
Account A
Debit  ₹1000

Account B
Credit ₹1000
```

Never physically remove completed financial transactions merely because an account is closed.

## Ledger Maintenance
Maintain the following fields for each entry:
- Transaction ID
- Account ID
- Debit/Credit
- Amount
- Currency
- Timestamp
- Transaction type
- Reference
- Status
- Source
- Destination
- Metadata

## Supported Operations
- Transaction history
- Statements
- Balance calculation
- Ledger inspection
- Daily reconciliation
- Failed transaction reconciliation
- Reversal
- Refund
- Audit logs

---

# 7. Security, Risk & Offline Architecture

## Objective
The entire application must work without Internet access using robust internal security controls. Security-sensitive data should never be stored as plaintext.

## Required Implementations
- Authentication
- Authorization
- Password hashing
- PIN hashing
- Role-based access control
- Secure secrets
- Input validation
- Transaction authorization
- Rate limiting
- Idempotency
- Audit logging
- Account locking
- Suspicious transaction detection
- Transaction limits

## Example Roles
```text
CUSTOMER
BANK_EMPLOYEE
BANK_ADMIN
SYSTEM_ADMIN
AUDITOR
```

---

# 8. Testing, Simulation & Banking Dashboard

## Objective
The final part combines system testing with a usable banking interface.

## Testing Strategy
Create automated tests covering various levels.

### Unit & Integration Tests
Test individual services and integration paths:

```text
Payment Service
       ↓
Transaction Engine
       ↓
Ledger
       ↓
Database
```

### End-to-End Tests
```text
Create Customer
      ↓
Verify KYC
      ↓
Create Account
      ↓
Deposit ₹10,000
      ↓
Create UPI ID
      ↓
Send ₹2,000
      ↓
Verify Ledger
      ↓
Verify Receiver Balance
```

### Failure Tests
Ensure to test for:
- Insufficient funds
- Invalid account
- Invalid UPI ID
- Invalid PIN
- Duplicate request
- Concurrent payments
- Database failure
- Transaction engine failure
- Interrupted transaction
- Invalid KYC
- Frozen account
- Closed account

## Banking Dashboard
Create a UI that exposes functionalities based on user role.

### Customer Dashboard
- Dashboard
- Balance
- Account details
- Transaction history
- UPI
- Transfer money
- Beneficiaries
- Statements
- Profile
- KYC status

### Bank Admin Dashboard
- Customers
- Accounts
- Transactions
- Pending KYC
- Suspicious transactions
- Account management
- Transaction monitoring
- Reconciliation
- Audit logs

---

# 9. Android Mobile Application

## Objective
The frontend is a native Android application built with Kotlin and XML layouts, located in the `transaction/` directory. It integrates directly with the backend API to simulate user-facing banking activities. All APIs are invoked on the local Spring Boot backend using Retrofit with zero external network dependencies.

## Application Screens
The flow consists of three primary screens:
1. `LoginActivity`: The secure entry point, verifying the user's registered mobile number.
2. `RegisterActivity`: The portal to onboard new users to the banking ecosystem.
3. `DashboardActivity`: The core financial hub allowing users to:
   - Check real-time Available Balance.
   - Access their unique User QR Code for receiving payments.
   - Use Scan to Pay functionality to initiate secure transfers.

---

# Core Architecture

The most important architectural rule is:
> The **Transaction Engine** remains the financial transaction core. The Payment Application orchestrates banking operations around it.

## Recommended Separation
```text
bangluPay/
│
├── transaction-engine/
│
└── payment-application/
    │
    ├── customer-service/
    ├── account-service/
    ├── kyc-service/
    ├── payment-service/
    ├── upi-service/
    ├── bank-service/
    ├── ledger-service/
    ├── security/
    └── dashboard/
```

*The exact architecture must be adjusted after inspecting the existing transaction engine.*

---

# Offline Requirement

The application must continue functioning with:

```text
Wi-Fi OFF
Ethernet DISCONNECTED
Internet UNAVAILABLE
```

All core functionality must operate locally. External APIs should not be required for:
- Account creation
- Account closure
- KYC simulation
- PAN simulation
- Aadhaar simulation
- UPI simulation
- Bank transfer
- Inter-bank transfer
- Transaction history
- Ledger
- Statements
- Reconciliation

External integrations may be added later as optional adapters.

---

# Important Financial Invariants

The system must preserve financial correctness. Briefly, this ensures money is never created or destroyed without tracking.

## Balance Calculation
```text
Source Balance_new = Source Balance_old - Amount

Destination Balance_new = Destination Balance_old + Amount
```

## Double-Entry Equality
```text
Σ Debits = Σ Credits
```

## Transaction Uniqueness
```text
transactionId ≠ transactionId of every other transaction
```

## Idempotency Rules
```text
same idempotencyKey
        ↓
same transaction result
        ↓
no duplicate financial movement
```

---

# Development Order

Do not build everything simultaneously. Use this sequential order:

```text
1. Inspect Transaction Engine
        ↓
2. Test Transaction Engine
        ↓
3. Define Banking Domain
        ↓
4. Implement Customer + Account Management
        ↓
5. Implement KYC Simulation
        ↓
6. Implement Payment + UPI
        ↓
7. Implement Inter-Bank Transfers
        ↓
8. Implement Ledger + Security + Dashboard
        ↓
9. Full Integration Testing
```

---

# Definition of Done

The project is considered functional when the following complete workflow works offline. The complete workflow must continue working with the Internet disconnected.

```text
Create Customer
      ↓
Aadhaar Verification
      ↓
PAN Verification
      ↓
Create Bank Account
      ↓
Deposit Money
      ↓
Create UPI ID
      ↓
Create Beneficiary
      ↓
Transfer Money
      ↓
Transaction Engine
      ↓
Double-Entry Ledger
      ↓
Receiver Account
      ↓
Transaction History
      ↓
Statement
      ↓
Reconciliation
```

---

# Disclaimer

BangluPay is an offline banking/payment **Simulation and Engineering Project**. Real-world financial services require regulated infrastructure, authentication mechanisms, security controls, compliance procedures, and authorized integrations. 

## Simulation vs Real Banking Infrastructure
- Aadhaar verification is simulated locally.
- PAN verification is simulated locally.
- UPI is simulated.
- IMPS is simulated.
- NEFT is simulated.
- RTGS is simulated.
- Other banks are simulated.
- **No real banking network is accessed.**

The purpose of this project is to demonstrate the engineering architecture behind a banking/payment platform while keeping the complete development environment local and offline.