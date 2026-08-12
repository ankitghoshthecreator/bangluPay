# BangluPay

## Offline Banking & Payment Platform

BangluPay is an offline-first banking and payment application built around an existing **Transaction Engine** located at:

```text
E:\bangluPay\transaction-engine
```

The goal is to build a complete banking/payment simulation that can operate **without an Internet connection**, while using the existing transaction engine as the core transaction-processing component.

The system should model real banking operations such as customer onboarding, account creation, identity verification, money transfers, UPI payments, inter-bank transactions, transaction history, account management, and banking operations.

The existing transaction engine must first be inspected and tested before the payment application is built around it.

---

# Project Overview

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

The existing transaction engine is the foundation of the project.

Before writing the payment application, inspect:

```text
E:\bangluPay\transaction-engine
```

Determine:

* Programming language
* Framework
* Project structure
* Entry point
* APIs/interfaces
* Transaction model
* Account model
* Ledger implementation
* Database/storage mechanism
* Persistence mechanism
* Transaction states
* Concurrency handling
* Idempotency support
* Failure handling
* Rollback mechanisms
* Transaction validation
* Balance calculation
* Authentication/authorization
* Existing tests
* Configuration
* Logging
* Error handling

The first objective is to establish whether the transaction engine actually works.

Create a test suite that verifies:

```text
Account A
    |
    | ₹1000
    v
Account B
```

After the transaction:

```text
Balance(A) = Balance(A_initial) - ₹1000
Balance(B) = Balance(B_initial) + ₹1000
```

Test:

* Successful transaction
* Insufficient balance
* Invalid account
* Duplicate transaction
* Concurrent transactions
* Failed transaction
* Rollback
* Partial failure
* Negative amount
* Zero amount
* Transaction status
* Transaction history
* Persistence after restart

Do not modify the transaction engine unnecessarily.

The payment application should consume the transaction engine through a clean interface.

---

# 2. Banking Core & Customer Management

Build the banking domain around the transaction engine.

Core entities should include:

```text
Customer
Account
Bank
Branch
Transaction
Beneficiary
Payment
KYCRecord
UPIProfile
LedgerEntry
```

Customer management should support:

* Customer registration
* Customer profile
* Customer ID generation
* Contact information
* Address
* Date of birth
* Account ownership
* KYC status
* Customer status
* Account relationships

Example:

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

Customer lifecycle:

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

Implement complete account lifecycle management.

## Account Creation

Support:

* Savings account
* Current account
* Salary account
* Basic account

Account creation should include:

```text
Account Number
Customer ID
Bank ID
Branch ID
Account Type
Currency
Balance
Status
Created At
```

Account states:

```text
PENDING
ACTIVE
FROZEN
SUSPENDED
CLOSED
```

## Account Deletion / Closure

Do not simply delete financial records.

Account closure should be a controlled banking operation:

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

Historical transactions must remain available after closure.

## Aadhaar Verification

Because the system must work without Internet access, Aadhaar verification should initially be implemented as an **offline mock/simulation**, not as a real UIDAI verification system.

Example:

```text
Aadhaar Number
      ↓
Local Verification Service
      ↓
Mock Aadhaar Registry
      ↓
VERIFIED / FAILED
```

Never store Aadhaar numbers in plaintext.

## PAN Verification

Similarly, PAN verification should initially use a local mock verification service.

Example:

```text
PAN
 ↓
Local PAN Registry
 ↓
Verification
 ↓
VALID / INVALID
```

The architecture should keep the verification service behind an interface so that a real regulated verification provider could theoretically be integrated later.

---

# 4. Payment & UPI System

Implement the payment layer on top of the transaction engine.

The payment system should support:

### Internal Bank Transfer

```text
Account A
    ↓
Transaction Service
    ↓
Account B
```

### UPI

Implement a simulated offline UPI ecosystem.

Example:

```text
ankit@banglupay
rahul@anotherbank
```

UPI functionality should include:

* UPI ID creation
* UPI ID lookup
* UPI PIN simulation
* Payment initiation
* Payment authorization
* Payment processing
* Payment status
* Payment history
* Refund
* Payment reversal
* Failed payment
* Duplicate payment protection

Payment lifecycle:

```text
INITIATED
    ↓
AUTHORIZED
    ↓
PROCESSING
    ↓
SUCCESS
```

Failure paths:

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

Every payment must have a unique:

```text
paymentId
transactionId
referenceNumber
```

Idempotency must prevent the same payment request from being processed twice.

---

# 5. Inter-Bank Transfer System

Create multiple simulated banks inside the offline environment.

Example:

```text
BangluPay Bank
State Bank Simulator
HDFC Simulator
ICICI Simulator
Axis Simulator
```

These are simulated institutions, not real banking integrations.

Example:

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

Support:

* Internal transfers
* Inter-bank transfers
* Beneficiary creation
* Beneficiary validation
* Transfer limits
* Transfer status
* Failed transfers
* Reversals
* Refunds
* Transaction references

Simulate banking rails such as:

```text
UPI
IMPS
NEFT
RTGS
```

However, these should be explicitly represented as **offline simulations**, because the real payment networks require external infrastructure and regulated access.

---

# 6. Ledger, Transactions & Reconciliation

The financial ledger must be treated as the source of truth.

A transaction should produce corresponding ledger entries.

For a transfer:

```text
Account A
Debit  ₹1000

Account B
Credit ₹1000
```

The fundamental invariant should be:

```text
Total Debits = Total Credits
```

For every successful double-entry transaction:

```text
Σ Debit = Σ Credit
```

Maintain:

* Transaction ID
* Account ID
* Debit/Credit
* Amount
* Currency
* Timestamp
* Transaction type
* Reference
* Status
* Source
* Destination
* Metadata

Support:

* Transaction history
* Statements
* Balance calculation
* Ledger inspection
* Daily reconciliation
* Failed transaction reconciliation
* Reversal
* Refund
* Audit logs

Never physically remove completed financial transactions merely because an account is closed.

---

# 7. Security, Risk & Offline Architecture

The entire application must work without Internet access.

The architecture should therefore be:

```text
                    OFFLINE MACHINE
                         |
             +-----------+-----------+
             |                       |
        Payment API             Banking UI
             |                       |
             +-----------+-----------+
                         |
                  Banking Services
                         |
              +----------+----------+
              |                     |
        Payment Service       KYC Service
              |                     |
              +----------+----------+
                         |
                Transaction Engine
                         |
                    Ledger/DB
```

No external network dependency should be required for normal operation.

Implement:

* Authentication
* Authorization
* Password hashing
* PIN hashing
* Role-based access control
* Secure secrets
* Input validation
* Transaction authorization
* Rate limiting
* Idempotency
* Audit logging
* Account locking
* Suspicious transaction detection
* Transaction limits

Example roles:

```text
CUSTOMER
BANK_EMPLOYEE
BANK_ADMIN
SYSTEM_ADMIN
AUDITOR
```

Security-sensitive data should never be stored as plaintext.

The project should clearly distinguish between:

```text
Simulation
```

and

```text
Real Banking Infrastructure
```

This project must not claim to perform real Aadhaar, PAN, UPI, IMPS, NEFT, or RTGS operations.

---

# 8. Testing, Simulation & Banking Dashboard

The final part combines system testing with a usable banking interface.

## Testing

Create automated tests for:

### Unit Tests

Test individual services.

### Integration Tests

Test:

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

Example:

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

Test:

* Insufficient funds
* Invalid account
* Invalid UPI ID
* Invalid PIN
* Duplicate request
* Concurrent payments
* Database failure
* Transaction engine failure
* Interrupted transaction
* Invalid KYC
* Frozen account
* Closed account

## Banking Dashboard

Create a UI that exposes:

### Customer

* Dashboard
* Balance
* Account details
* Transaction history
* UPI
* Transfer money
* Beneficiaries
* Statements
* Profile
* KYC status

### Bank Admin

* Customers
* Accounts
* Transactions
* Pending KYC
* Suspicious transactions
* Account management
* Transaction monitoring
* Reconciliation
* Audit logs

---

# 9. Android Mobile Application

The frontend is a native Android application built with Kotlin and XML layouts, located in the `transaction/` directory. It integrates directly with the backend API to simulate user-facing banking activities.

The flow consists of three primary screens:
1. **Login (`LoginActivity`)**: The secure entry point, verifying the user's registered mobile number.
2. **Register (`RegisterActivity`)**: The portal to onboard new users to the banking ecosystem.
3. **Dashboard (`DashboardActivity`)**: The core financial hub allowing users to:
   - Check real-time `Available Balance`.
   - Access their unique User `QR Code` for receiving payments.
   - Use `Scan to Pay` functionality to initiate secure transfers.

All APIs are invoked on the local Spring Boot backend using Retrofit with zero external network dependencies.

---

# Core Architecture

The most important architectural rule is:

> The Transaction Engine remains the financial transaction core. The Payment Application orchestrates banking operations around it.

Recommended separation:

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

The exact architecture must be adjusted after inspecting the existing transaction engine.

---

# Offline Requirement

The application must continue functioning with:

```text
Wi-Fi OFF
Ethernet DISCONNECTED
Internet UNAVAILABLE
```

All core functionality must operate locally.

External APIs should not be required for:

* Account creation
* Account closure
* KYC simulation
* PAN simulation
* Aadhaar simulation
* UPI simulation
* Bank transfer
* Inter-bank transfer
* Transaction history
* Ledger
* Statements
* Reconciliation

External integrations may be added later as optional adapters.

---

# Important Financial Invariants

The system must preserve financial correctness.

For every successful transaction:

```text
Source Balance_new
=
Source Balance_old - Amount
```

and:

```text
Destination Balance_new
=
Destination Balance_old + Amount
```

For the complete ledger:

```text
Σ Debits = Σ Credits
```

For every transaction:

```text
transactionId ≠ transactionId of every other transaction
```

For idempotent requests:

```text
same idempotencyKey
        ↓
same transaction result
        ↓
no duplicate financial movement
```

These invariants should be tested automatically.

---

# Development Order

Do not build everything simultaneously.

Use this order:

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

The project is considered functional when the following complete workflow works offline:

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

And the entire workflow continues to work with the Internet disconnected.

---

# Disclaimer

BangluPay is an offline banking/payment **simulation and engineering project**.

It does not connect to or perform transactions through real:

* UPI
* NPCI
* Aadhaar
* UIDAI
* PAN verification systems
* Banks
* IMPS
* NEFT
* RTGS

Real-world financial services require regulated infrastructure, authentication mechanisms, security controls, compliance procedures, and authorized integrations.

The purpose of this project is to demonstrate the engineering architecture behind a banking/payment platform while keeping the complete development environment local and offline.# bangluPay
#   b a n g l u P a y  
 