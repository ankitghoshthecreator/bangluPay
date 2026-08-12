package engine.nexus.service;

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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEngine {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional
    public Transaction processTransfer(String externalId, UUID fromId, UUID toId, BigDecimal amount, String description) {
        log.info("Processing transfer from {} to {} for amount {} [ExternalId: {}]", fromId, toId, amount, externalId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (fromId.equals(toId)) {
            throw new RuntimeException("Sender and receiver accounts must be different");
        }

        // 1. Idempotency Check
        return transactionRepository.findByExternalId(externalId)
                .orElseGet(() -> {
                    // 2. Fetch Accounts
                    Account fromAccount = accountRepository.findById(fromId)
                            .orElseThrow(() -> new RuntimeException("Sender account not found"));
                    Account toAccount = accountRepository.findById(toId)
                            .orElseThrow(() -> new RuntimeException("Receiver account not found"));

                    // 3. Validation
                    if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
                        throw new RuntimeException("Sender account is not active");
                    }
                    if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
                        throw new RuntimeException("Receiver account is not active");
                    }
                    if (fromAccount.getBalance().compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient funds");
                    }

                    // 4. Create Transaction Record
                    Transaction tx = Transaction.builder()
                            .transactionId(UUID.randomUUID())
                            .externalId(externalId)
                            .fromAccountId(fromId)
                            .toAccountId(toId)
                            .amount(amount)
                            .description(description)
                            .status(Transaction.TransactionStatus.COMPLETED)
                            .build();
                    tx = transactionRepository.save(tx);

                    // 5. Create Ledger Entries (Double-Entry)
                    LedgerEntry debit = LedgerEntry.builder()
                            .transactionId(tx.getTransactionId())
                            .accountId(fromId)
                            .type(LedgerEntry.EntryType.DEBIT)
                            .amount(amount)
                            .build();

                    LedgerEntry credit = LedgerEntry.builder()
                            .transactionId(tx.getTransactionId())
                            .accountId(toId)
                            .type(LedgerEntry.EntryType.CREDIT)
                            .amount(amount)
                            .build();

                    ledgerRepository.save(debit);
                    ledgerRepository.save(credit);

                    // 6. Update Account Balances (Optimistic Locking handled by Hibernate @Version)
                    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
                    toAccount.setBalance(toAccount.getBalance().add(amount));

                    accountRepository.save(fromAccount);
                    accountRepository.save(toAccount);

                    log.info("Transfer completed successfully: {}", tx.getTransactionId());
                    return tx;
                });
    }
}
