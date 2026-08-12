package engine.nexus.repository;

import engine.nexus.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomerId(UUID customerId);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByBankId(UUID bankId);
}
