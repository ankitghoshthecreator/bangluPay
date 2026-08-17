package engine.nexus.service;

import engine.nexus.model.Account;
import engine.nexus.model.LedgerEntry;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LedgerService — statement generation, balance computation, and integrity checks.
 *
 * The ledger is the AUTHORITATIVE source of truth for balances.
 * Any discrepancy between stored account balance and ledger-computed balance
 * is a CRITICAL financial integrity violation and must be flagged immediately.
 *
 * GUARDRAIL: This service is read-only. It never creates or modifies ledger entries.
 * Entries are exclusively created by TransactionEngine and AccountService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;

    /**
     * Get the full ledger statement for an account.
     * Returns all entries, ordered by creation time (oldest first).
     */
    public List<LedgerEntry> getStatement(UUID accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        return ledgerRepository.findByAccountId(accountId);
    }

    /**
     * Get ledger entries for an account within a date range.
     */
    public List<LedgerEntry> getStatement(UUID accountId, LocalDateTime from, LocalDateTime to) {
        return ledgerRepository.findByAccountId(accountId).stream()
                .filter(e -> e.getCreatedAt() != null
                        && !e.getCreatedAt().isBefore(from)
                        && !e.getCreatedAt().isAfter(to))
                .collect(Collectors.toList());
    }

    /**
     * Compute the balance for an account from the ledger (not from stored balance).
     * CREDIT entries increase balance; DEBIT entries decrease it.
     */
    public BigDecimal getLedgerBalance(UUID accountId) {
        List<LedgerEntry> entries = ledgerRepository.findByAccountId(accountId);
        BigDecimal balance = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            if (e.getType() == LedgerEntry.EntryType.CREDIT) {
                balance = balance.add(e.getAmount());
            } else {
                balance = balance.subtract(e.getAmount());
            }
        }
        return balance.setScale(2);
    }

    /**
     * Reconcile stored balance vs ledger-computed balance for an account.
     * Returns a reconciliation report map with keys:
     *   accountId, storedBalance, ledgerBalance, consistent, drift
     */
    public Map<String, Object> reconcile(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        BigDecimal storedBalance = account.getBalance();
        BigDecimal ledgerBalance = getLedgerBalance(accountId);
        BigDecimal drift = storedBalance.subtract(ledgerBalance).abs();
        boolean consistent = storedBalance.compareTo(ledgerBalance) == 0;

        Map<String, Object> report = new HashMap<>();
        report.put("accountId", accountId.toString());
        report.put("accountNumber", account.getAccountNumber());
        report.put("storedBalance", storedBalance);
        report.put("ledgerBalance", ledgerBalance);
        report.put("consistent", consistent);
        report.put("drift", drift);
        report.put("reconciledAt", LocalDateTime.now().toString());

        if (!consistent) {
            log.error("[LedgerService] BALANCE DRIFT DETECTED for account {}: stored={} ledger={} drift={}",
                    accountId, storedBalance, ledgerBalance, drift);
        } else {
            log.info("[LedgerService] Account {} balance consistent: {}", accountId, storedBalance);
        }

        return report;
    }

    /**
     * System-wide double-entry integrity check.
     * For every transaction, the sum of DEBIT entries must equal the sum of CREDIT entries.
     * Returns a summary report.
     */
    public Map<String, Object> verifyDoubleEntryIntegrity() {
        List<LedgerEntry> allEntries = ledgerRepository.findAll();

        BigDecimal totalDebits = allEntries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = allEntries.stream()
                .filter(e -> e.getType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal imbalance = totalDebits.subtract(totalCredits).abs();
        boolean balanced = totalDebits.compareTo(totalCredits) == 0;

        Map<String, Object> report = new HashMap<>();
        report.put("totalDebits", totalDebits);
        report.put("totalCredits", totalCredits);
        report.put("balanced", balanced);
        report.put("imbalance", imbalance);
        report.put("totalEntries", allEntries.size());
        report.put("checkedAt", LocalDateTime.now().toString());

        if (!balanced) {
            log.error("[LedgerService] DOUBLE-ENTRY INTEGRITY VIOLATION: totalDebits={} totalCredits={} imbalance={}",
                    totalDebits, totalCredits, imbalance);
        } else {
            log.info("[LedgerService] Double-entry integrity OK: debits=credits={}", totalDebits);
        }

        return report;
    }

    /**
     * Get all ledger entries for a specific transaction.
     */
    public List<LedgerEntry> getTransactionEntries(UUID transactionId) {
        return ledgerRepository.findByTransactionId(transactionId);
    }

    /**
     * Get all ledger entries in the system (admin use only).
     */
    public List<LedgerEntry> getFullLedger() {
        return ledgerRepository.findAll();
    }
}
