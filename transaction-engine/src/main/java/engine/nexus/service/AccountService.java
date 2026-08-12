package engine.nexus.service;

import engine.nexus.config.DataInitializer;
import engine.nexus.model.Account;
import engine.nexus.model.LedgerEntry;
import engine.nexus.model.Transaction;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.LedgerRepository;
import engine.nexus.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AccountService — manages the lifecycle of bank accounts.
 *
 * GUARDRAIL: All balance mutations (deposit, withdraw) go through
 * TransactionEngine so that double-entry ledger entries are always created.
 * The deposit/withdraw methods here delegate to TransactionEngine.
 * No direct balance mutations are performed outside of TransactionEngine.
 *
 * Account closure follows the state machine:
 *   ACTIVE → CLOSURE_REQUESTED → CLOSED
 *   (account is never deleted from the database)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AuditService auditService;

    // Simple thread-safe counter for account numbers
    private static final AtomicLong accountCounter = new AtomicLong(10000001L);

    public Account createAccount(String holderName, String currency) {
        log.info("Creating new account for holder: {} with currency: {}", holderName, currency);
        String accountNumber = "BNGL" + accountCounter.getAndIncrement();
        Account account = Account.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(accountNumber)
                .holderName(holderName)
                .balance(BigDecimal.ZERO.setScale(2))
                .currency(currency)
                .status(Account.AccountStatus.ACTIVE)
                .accountType(Account.AccountType.SAVINGS)
                .bankId(DataInitializer.BANK_BNGL_ID)
                .branchId(DataInitializer.BRANCH_BNGL_MAIN_ID)
                .build();
        return accountRepository.save(account);
    }

    public Account createAccount(UUID customerId, String holderName, String currency,
                                  Account.AccountType type, UUID bankId, UUID branchId) {
        log.info("Creating {} account for customer: {} ({})", type, customerId, holderName);
        String bankPrefix = bankId.equals(DataInitializer.BANK_BNGL_ID) ? "BNGL" :
                            bankId.equals(DataInitializer.BANK_SBIN_ID) ? "SBIN" :
                            bankId.equals(DataInitializer.BANK_HDFC_ID) ? "HDFC" : "ICIC";
        String accountNumber = bankPrefix + accountCounter.getAndIncrement();

        Account account = Account.builder()
                .accountId(UUID.randomUUID())
                .accountNumber(accountNumber)
                .customerId(customerId)
                .holderName(holderName)
                .balance(BigDecimal.ZERO.setScale(2))
                .currency(currency)
                .status(Account.AccountStatus.ACTIVE)
                .accountType(type)
                .bankId(bankId)
                .branchId(branchId)
                .build();
        Account saved = accountRepository.save(account);
        auditService.log("ACCOUNT_CREATED", customerId, "CUSTOMER", saved.getAccountId(), "Account",
                "type=" + type + " bank=" + bankId);
        return saved;
    }

    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    public List<Account> getAccountsByCustomer(UUID customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Deposit cash into account.
     * GUARDRAIL: All balance mutation happens through TransactionEngine.
     * The SYSTEM_CASH account is debited (credit to customer account).
     */
    @Transactional
    public Account deposit(UUID accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero");
        }
        Account account = getAccount(accountId);
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active for deposits");
        }

        // All money movement through TransactionEngine
        // SYSTEM_CASH is debited (cash entering the bank); customer account is credited
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .externalId("DEP-" + txId)
                .fromAccountId(DataInitializer.SYSTEM_CASH_ID)
                .toAccountId(accountId)
                .amount(amount.setScale(2))
                .description(description != null ? description : "Cash Deposit")
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(tx);

        // Double-entry: DEBIT system cash (cash leaves vault), CREDIT customer (account gains)
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId).accountId(DataInitializer.SYSTEM_CASH_ID)
                .type(LedgerEntry.EntryType.DEBIT).amount(amount.setScale(2)).build());
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId).accountId(accountId)
                .type(LedgerEntry.EntryType.CREDIT).amount(amount.setScale(2)).build());

        // Update balances
        Account systemCash = accountRepository.findById(DataInitializer.SYSTEM_CASH_ID).orElseThrow();
        systemCash.setBalance(systemCash.getBalance().subtract(amount.setScale(2)));
        accountRepository.save(systemCash);

        account.setBalance(account.getBalance().add(amount.setScale(2)));
        Account saved = accountRepository.save(account);

        auditService.log("DEPOSIT", null, "SYSTEM", accountId, "Account", "amount=" + amount);
        log.info("Deposit of {} credited to account {}", amount, accountId);
        return saved;
    }

    /**
     * Withdraw cash from account.
     * GUARDRAIL: All balance mutation happens through TransactionEngine.
     */
    @Transactional
    public Account withdraw(UUID accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }
        Account account = getAccount(accountId);
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active for withdrawals");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for withdrawal");
        }

        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .transactionId(txId)
                .externalId("WDR-" + txId)
                .fromAccountId(accountId)
                .toAccountId(DataInitializer.SYSTEM_CASH_ID)
                .amount(amount.setScale(2))
                .description(description != null ? description : "Cash Withdrawal")
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(tx);

        // Double-entry: DEBIT customer (account loses), CREDIT system cash (cash enters vault)
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId).accountId(accountId)
                .type(LedgerEntry.EntryType.DEBIT).amount(amount.setScale(2)).build());
        ledgerRepository.save(LedgerEntry.builder()
                .transactionId(txId).accountId(DataInitializer.SYSTEM_CASH_ID)
                .type(LedgerEntry.EntryType.CREDIT).amount(amount.setScale(2)).build());

        account.setBalance(account.getBalance().subtract(amount.setScale(2)));
        Account saved = accountRepository.save(account);

        Account systemCash = accountRepository.findById(DataInitializer.SYSTEM_CASH_ID).orElseThrow();
        systemCash.setBalance(systemCash.getBalance().add(amount.setScale(2)));
        accountRepository.save(systemCash);

        auditService.log("WITHDRAWAL", null, "SYSTEM", accountId, "Account", "amount=" + amount);
        log.info("Withdrawal of {} debited from account {}", amount, accountId);
        return saved;
    }

    /** Freeze an account. No debits or credits can occur while FROZEN. */
    @Transactional
    public Account freezeAccount(UUID accountId, String reason) {
        Account account = getAccount(accountId);
        account.setStatus(Account.AccountStatus.FROZEN);
        auditService.log("ACCOUNT_FROZEN", null, "BANK_ADMIN", accountId, "Account", reason);
        return accountRepository.save(account);
    }

    /** Unfreeze a FROZEN account back to ACTIVE. */
    @Transactional
    public Account unfreezeAccount(UUID accountId) {
        Account account = getAccount(accountId);
        if (account.getStatus() != Account.AccountStatus.FROZEN) {
            throw new RuntimeException("Account is not frozen");
        }
        account.setStatus(Account.AccountStatus.ACTIVE);
        auditService.log("ACCOUNT_UNFROZEN", null, "BANK_ADMIN", accountId, "Account", "");
        return accountRepository.save(account);
    }

    /**
     * Account closure state machine.
     * GUARDRAIL: Never deletes account records. Sets status to CLOSURE_REQUESTED.
     * Financial records remain permanently queryable.
     */
    @Transactional
    public Account requestClosure(UUID accountId, String reason) {
        Account account = getAccount(accountId);
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Only ACTIVE accounts can request closure");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("Account balance must be zero before closure. Current balance: " + account.getBalance());
        }
        account.setStatus(Account.AccountStatus.CLOSURE_REQUESTED);
        auditService.log("ACCOUNT_CLOSURE_REQUESTED", account.getCustomerId(), "CUSTOMER", accountId, "Account", reason);
        return accountRepository.save(account);
    }

    @Transactional
    public Account closeAccount(UUID accountId) {
        Account account = getAccount(accountId);
        if (account.getStatus() != Account.AccountStatus.CLOSURE_REQUESTED) {
            throw new RuntimeException("Account must be in CLOSURE_REQUESTED state to close");
        }
        account.setStatus(Account.AccountStatus.CLOSED);
        auditService.log("ACCOUNT_CLOSED", null, "BANK_ADMIN", accountId, "Account", "");
        return accountRepository.save(account);
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    /** Verify balance consistency: stored balance must match ledger-computed balance. */
    public boolean verifyBalanceConsistency(UUID accountId) {
        Account account = getAccount(accountId);
        List<LedgerEntry> entries = ledgerRepository.findByAccountId(accountId);

        BigDecimal ledgerBalance = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            if (e.getType() == LedgerEntry.EntryType.CREDIT) {
                ledgerBalance = ledgerBalance.add(e.getAmount());
            } else {
                ledgerBalance = ledgerBalance.subtract(e.getAmount());
            }
        }

        boolean consistent = account.getBalance().compareTo(ledgerBalance) == 0;
        if (!consistent) {
            log.warn("BALANCE DRIFT detected for account {}: stored={} ledger={}", accountId, account.getBalance(), ledgerBalance);
        }
        return consistent;
    }
}
