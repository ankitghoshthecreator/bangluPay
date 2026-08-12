package engine.nexus.repository;

import engine.nexus.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionId(UUID transactionId);
    List<LedgerEntry> findByAccountId(UUID accountId);
}
