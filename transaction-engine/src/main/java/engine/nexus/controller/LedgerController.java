package engine.nexus.controller;

import engine.nexus.model.LedgerEntry;
import engine.nexus.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LedgerController — REST API for statements, balances, reconciliation, and integrity checks.
 */
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {

    private final LedgerService ledgerService;

    /**
     * GET /api/ledger/statement/{accountId}
     * Get the full ledger statement for an account.
     * Optional query params: from, to (ISO datetime, e.g. 2026-01-01T00:00:00)
     */
    @GetMapping("/statement/{accountId}")
    public ResponseEntity<List<LedgerEntry>> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        if (from != null && to != null) {
            LocalDateTime fromDt = LocalDateTime.parse(from);
            LocalDateTime toDt = LocalDateTime.parse(to);
            return ResponseEntity.ok(ledgerService.getStatement(accountId, fromDt, toDt));
        }
        return ResponseEntity.ok(ledgerService.getStatement(accountId));
    }

    /**
     * GET /api/ledger/balance/{accountId}
     * Get the ledger-computed balance for an account.
     * This is calculated from ledger entries, not from the stored account balance.
     */
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<Map<String, Object>> getLedgerBalance(@PathVariable UUID accountId) {
        BigDecimal balance = ledgerService.getLedgerBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", accountId.toString(),
                "ledgerBalance", balance
        ));
    }

    /**
     * GET /api/ledger/reconcile/{accountId}
     * Reconcile stored balance against ledger balance.
     * Returns a report showing whether they match and any drift.
     */
    @GetMapping("/reconcile/{accountId}")
    public ResponseEntity<Map<String, Object>> reconcile(@PathVariable UUID accountId) {
        log.info("REST: Reconcile account={}", accountId);
        return ResponseEntity.ok(ledgerService.reconcile(accountId));
    }

    /**
     * GET /api/ledger/integrity
     * Verify system-wide double-entry integrity: Σ Debits = Σ Credits.
     * Admin-only endpoint.
     */
    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        log.info("REST: Double-entry integrity check requested");
        return ResponseEntity.ok(ledgerService.verifyDoubleEntryIntegrity());
    }

    /**
     * GET /api/ledger/transaction/{transactionId}
     * Get all ledger entries for a specific transaction.
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<LedgerEntry>> getTransactionEntries(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(ledgerService.getTransactionEntries(transactionId));
    }
}
