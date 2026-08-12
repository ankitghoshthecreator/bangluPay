📌 Exactly-Once Payment Processing System
🧠 Overview

This project implements a fault-tolerant payment processing system that guarantees exactly-once money movement across distributed services.

In real-world banking systems, retries, timeouts, and partial failures can lead to:

Duplicate debits
Missing credits
Inconsistent balances

This system addresses those issues using idempotency, state machines, and a double-entry ledger, ensuring financial correctness under failure conditions.

🚨 Problem Statement

Distributed financial systems cannot rely on simple request-response guarantees due to:

Network retries
Service crashes
Concurrent requests
Partial transaction execution

The challenge is to ensure:

Money is neither created nor destroyed, and every transaction executes exactly once.

🏗️ System Design
🔁 Transaction Lifecycle (State Machine)
INIT → AUTHORIZED → SETTLED
        ↓
      FAILED
INIT → Transaction created
AUTHORIZED → Debit validated
SETTLED → Debit + Credit completed
FAILED → Transaction aborted safely
💰 Double-Entry Ledger

Every transaction is recorded as:

Account	Debit	Credit
Sender Account	100	
Receiver Account		100

✔ Ensures:

Total Debit = Total Credit
No money creation/destruction
🔐 Idempotency Layer

Each request includes an Idempotency Key:

Prevents duplicate execution
Returns same response for repeated requests
Handles retries safely
⚙️ Core Components
API Layer → Accepts transaction requests
Idempotency Service → Deduplicates requests
Transaction Engine → Manages state transitions
Ledger Service → Maintains financial records
Database → Persistent storage
Cache (Redis) → Fast idempotency + locking
🧪 Failure Handling

System is designed to handle:

🔁 Duplicate API retries
⏱️ Timeout after debit but before credit
💥 Service crash during processing
⚔️ Concurrent transaction requests
Recovery Strategy
Replay-safe operations
State-based recovery
Ledger invariants validation
🔍 Key Features
Exactly-once transaction processing
Strong financial consistency
Replay-safe architecture
Deterministic state transitions
Double-entry accounting system
Failure simulation and recovery
🛠️ Tech Stack
Language: Java
Framework: Spring Boot
Database: SQLite (Production-ready ACID compliance)
Concurrency: Java 22 Virtual Threads
Architecture: REST APIs + Double-Entry Ledger
📂 Project Structure
/src
 ├── controller       # API endpoints
 ├── service          # Business logic
 ├── engine           # Transaction state machine
 ├── ledger           # Double-entry system
 ├── repository       # Database access
 ├── model            # Entities
 └── config           # Configurations
🚀 How It Works (Flow)
Client sends transaction request with idempotency key
System checks if request already processed
If new:
Create transaction (INIT)
Move to AUTHORIZED
Execute debit + credit
Move to SETTLED
Ledger updated with both entries
Response returned
🧩 Challenges Addressed
Ensuring exactly-once semantics without distributed locks
Handling partial failures safely
Maintaining ledger consistency under concurrency
Designing replay-safe transaction processing
📊 Future Improvements
Kafka-based event streaming
Distributed tracing (OpenTelemetry)
Multi-currency ledger support
Horizontal scaling with sharding
Stronger consistency via consensus mechanisms
🎯 Key Learnings
Financial systems require strong invariants, not eventual consistency
Idempotency is critical for safe retries
State machines simplify complex workflows
Ledger design is central to system correctness
⚠️ Disclaimer

This project is a simulation of real-world financial systems designed for learning and demonstration purposes.
It does not represent a production-ready banking system.

👨‍💻 Author

Ankit Ghosh

⭐ If you found this useful

Give it a star ⭐ and feel free to fork or contribute!