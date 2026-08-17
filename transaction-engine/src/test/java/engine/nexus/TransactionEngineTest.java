package engine.nexus;

import engine.nexus.model.Account;
import engine.nexus.model.LedgerEntry;
import engine.nexus.model.Transaction;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.LedgerRepository;
import engine.nexus.repository.TransactionRepository;
import engine.nexus.service.AccountService;
import engine.nexus.service.TransactionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransactionEngineTest {

    @Autowired
    private TransactionEngine transactionEngine;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        // Re-seed system cash account since deleteAll removed it
        if (!accountRepository.existsById(engine.nexus.config.DataInitializer.SYSTEM_CASH_ID)) {
            accountRepository.save(Account.builder()
                    .accountId(engine.nexus.config.DataInitializer.SYSTEM_CASH_ID)
                    .accountNumber("SYSTEM-CASH-0001")
                    .holderName("SYSTEM_CASH")
                    .balance(BigDecimal.ZERO.setScale(2))
                    .currency("INR")
                    .status(Account.AccountStatus.ACTIVE)
                    .accountType(Account.AccountType.CURRENT)
                    .bankId(engine.nexus.config.DataInitializer.BANK_BNGL_ID)
                    .build());
        }

        accountA = accountService.createAccount("Alice", "INR");
        accountA.setBalance(new BigDecimal("10000.00"));
        accountA = accountRepository.save(accountA);

        accountB = accountService.createAccount("Bob", "INR");
        accountB.setBalance(new BigDecimal("5000.00"));
        accountB = accountRepository.save(accountB);
    }

    private void assertBalanceEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual, "Actual balance should not be null");
        assertEquals(0, expected.compareTo(actual), "Expected balance " + expected + " but got " + actual);
    }

    @Test
    @DisplayName("Successful Transfer & Financial Invariants")
    void testSuccessfulTransfer() {
        BigDecimal transferAmount = new BigDecimal("2000.00");
        String extId = "TX-SUCCESS-001";

        BigDecimal oldBalA = accountA.getBalance();
        BigDecimal oldBalB = accountB.getBalance();

        Transaction tx = transactionEngine.processTransfer(
                extId, accountA.getAccountId(), accountB.getAccountId(), transferAmount, "Test Payment"
        );

        assertNotNull(tx);
        assertEquals(Transaction.TransactionStatus.COMPLETED, tx.getStatus());

        Account updatedA = accountService.getAccount(accountA.getAccountId());
        Account updatedB = accountService.getAccount(accountB.getAccountId());

        // Balance check
        assertBalanceEquals(new BigDecimal("8000.00"), updatedA.getBalance());
        assertBalanceEquals(new BigDecimal("7000.00"), updatedB.getBalance());

        // Invariant checks
        assertBalanceEquals(updatedA.getBalance(), oldBalA.subtract(transferAmount));
        assertBalanceEquals(updatedB.getBalance(), oldBalB.add(transferAmount));

        // Double-entry ledger check
        List<LedgerEntry> entries = ledgerRepository.findByTransactionId(tx.getTransactionId());
        assertEquals(2, entries.size());

        BigDecimal totalDebit = entries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, totalDebit.compareTo(totalCredit), "Total Debit must equal Total Credit");
        assertEquals(0, totalDebit.compareTo(transferAmount));
    }

    @Test
    @DisplayName("Failure Case — Insufficient Balance")
    void testInsufficientBalance() {
        BigDecimal transferAmount = new BigDecimal("15000.00");

        Exception ex = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-FAIL-FUNDS", accountA.getAccountId(), accountB.getAccountId(), transferAmount, "Overdraft"
                )
        );

        assertTrue(ex.getMessage().contains("Insufficient funds"));

        Account updatedA = accountService.getAccount(accountA.getAccountId());
        assertBalanceEquals(new BigDecimal("10000.00"), updatedA.getBalance());
    }

    @Test
    @DisplayName("Failure Case — Invalid Source Account")
    void testInvalidSourceAccount() {
        UUID fakeId = UUID.randomUUID();

        Exception ex = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-FAIL-SRC", fakeId, accountB.getAccountId(), new BigDecimal("100.00"), "Bad Source"
                )
        );

        assertTrue(ex.getMessage().contains("Sender account not found"));
    }

    @Test
    @DisplayName("Failure Case — Invalid Destination Account")
    void testInvalidDestinationAccount() {
        UUID fakeId = UUID.randomUUID();

        Exception ex = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-FAIL-DEST", accountA.getAccountId(), fakeId, new BigDecimal("100.00"), "Bad Dest"
                )
        );

        assertTrue(ex.getMessage().contains("Receiver account not found"));
    }

    @Test
    @DisplayName("Failure Case — Zero or Negative Amount")
    void testZeroOrNegativeAmount() {
        Exception ex1 = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-ZERO", accountA.getAccountId(), accountB.getAccountId(), BigDecimal.ZERO, "Zero"
                )
        );
        assertTrue(ex1.getMessage().contains("Amount must be greater than zero"));

        Exception ex2 = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-NEG", accountA.getAccountId(), accountB.getAccountId(), new BigDecimal("-50.00"), "Negative"
                )
        );
        assertTrue(ex2.getMessage().contains("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("Failure Case — Inactive Receiver Account")
    void testInactiveReceiverAccount() {
        accountB.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(accountB);

        Exception ex = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-FROZEN-DEST", accountA.getAccountId(), accountB.getAccountId(), new BigDecimal("500.00"), "To Frozen"
                )
        );

        assertTrue(ex.getMessage().contains("Receiver account is not active"));
    }

    @Test
    @DisplayName("Failure Case — Self Transfer")
    void testSelfTransfer() {
        Exception ex = assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-SELF", accountA.getAccountId(), accountA.getAccountId(), new BigDecimal("500.00"), "Self"
                )
        );

        assertTrue(ex.getMessage().contains("Sender and receiver accounts must be different"));
    }

    @Test
    @DisplayName("Idempotency — Duplicate Submission")
    void testIdempotency() {
        String extId = "IDEMPOTENT-KEY-123";
        BigDecimal amt = new BigDecimal("1000.00");

        Transaction tx1 = transactionEngine.processTransfer(
                extId, accountA.getAccountId(), accountB.getAccountId(), amt, "First Try"
        );

        Transaction tx2 = transactionEngine.processTransfer(
                extId, accountA.getAccountId(), accountB.getAccountId(), amt, "Second Try (Retry)"
        );

        assertEquals(tx1.getTransactionId(), tx2.getTransactionId());

        Account updatedA = accountService.getAccount(accountA.getAccountId());
        Account updatedB = accountService.getAccount(accountB.getAccountId());

        // Balance updated only once
        assertBalanceEquals(new BigDecimal("9000.00"), updatedA.getBalance());
        assertBalanceEquals(new BigDecimal("6000.00"), updatedB.getBalance());
    }

    @Test
    @DisplayName("Deposit & Withdrawal Double-Entry Ledger")
    void testDepositAndWithdrawalLedger() {
        Account acc = accountService.deposit(accountA.getAccountId(), new BigDecimal("3000.00"), "Cash Deposit");
        assertBalanceEquals(new BigDecimal("13000.00"), acc.getBalance());

        List<LedgerEntry> entries = ledgerRepository.findByAccountId(acc.getAccountId());
        assertFalse(entries.isEmpty());

        LedgerEntry depositEntry = entries.get(entries.size() - 1);
        assertEquals(LedgerEntry.EntryType.CREDIT, depositEntry.getType());
        assertEquals(0, new BigDecimal("3000.00").compareTo(depositEntry.getAmount()));
    }

    @Test
    @DisplayName("Concurrency — Double-Spend Prevention (₹8000 × 2 from ₹10000 account)")
    void testConcurrentTransfersDoubleSpendPrevention() throws InterruptedException {
        // Account A has ₹10,000. Two threads try to transfer ₹8,000 each.
        // Exactly one must succeed and exactly one must fail.
        BigDecimal largeAmount = new BigDecimal("8000.00");
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final String extId = "CONCURRENT-TX-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    transactionEngine.processTransfer(
                            extId, accountA.getAccountId(), accountB.getAccountId(), largeAmount, "Concurrent Transfer"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one transfer should succeed");
        assertEquals(1, failCount.get(), "Exactly one transfer should fail");

        Account finalA = accountService.getAccount(accountA.getAccountId());
        Account finalB = accountService.getAccount(accountB.getAccountId());

        // Balance must never go negative — the succeeded transfer deducted ₹8,000
        assertTrue(finalA.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Account A balance must never be negative");
        // Sum of balances must be conserved (A+B = 10000+5000 = 15000)
        BigDecimal totalBalance = finalA.getBalance().add(finalB.getBalance());
        assertBalanceEquals(new BigDecimal("15000.00"), totalBalance);
    }

    @Test
    @DisplayName("Rollback — Transaction Failure Leaves Balances Unchanged")
    void testRollbackOnFailure() {
        // A transfer with an invalid toAccountId must not mutate any balance
        UUID badId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () ->
                transactionEngine.processTransfer(
                        "TX-ROLLBACK", accountA.getAccountId(), badId, new BigDecimal("500.00"), "Should Rollback"
                )
        );

        Account checkA = accountService.getAccount(accountA.getAccountId());
        assertBalanceEquals(new BigDecimal("10000.00"), checkA.getBalance());

        // No ledger entries should exist for this failed transaction
        List<Transaction> allTx = transactionRepository.findAll();
        boolean hasRollbackTx = allTx.stream().anyMatch(t -> "TX-ROLLBACK".equals(t.getExternalId()));
        assertFalse(hasRollbackTx, "A failed transaction must not persist in the transaction table");
    }

    @Test
    @DisplayName("Persistence — Balance Survives After Repository Save/Reload Cycle")
    void testPersistenceAndReload() {
        transactionEngine.processTransfer(
                "TX-PERSIST", accountA.getAccountId(), accountB.getAccountId(), new BigDecimal("3000.00"), "Persist Test"
        );

        // Simulate reload by fetching fresh from repository (not from session cache)
        accountRepository.flush();
        Account freshA = accountRepository.findById(accountA.getAccountId()).orElseThrow();
        Account freshB = accountRepository.findById(accountB.getAccountId()).orElseThrow();

        assertBalanceEquals(new BigDecimal("7000.00"), freshA.getBalance());
        assertBalanceEquals(new BigDecimal("8000.00"), freshB.getBalance());
    }

    @Test
    @DisplayName("Financial Invariant — Sum of All Balances Is Conserved After Transfers")
    void testSystemWideBalanceConservation() {
        BigDecimal initialTotal = accountA.getBalance().add(accountB.getBalance());

        transactionEngine.processTransfer("TX-C1", accountA.getAccountId(), accountB.getAccountId(), new BigDecimal("1000.00"), "T1");
        transactionEngine.processTransfer("TX-C2", accountA.getAccountId(), accountB.getAccountId(), new BigDecimal("2000.00"), "T2");
        transactionEngine.processTransfer("TX-C3", accountB.getAccountId(), accountA.getAccountId(), new BigDecimal("500.00"), "T3");

        Account finalA = accountService.getAccount(accountA.getAccountId());
        Account finalB = accountService.getAccount(accountB.getAccountId());
        BigDecimal finalTotal = finalA.getBalance().add(finalB.getBalance());

        assertBalanceEquals(initialTotal, finalTotal);
    }
}
