package engine.nexus.repository;

import engine.nexus.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BankRepository extends JpaRepository<Bank, UUID> {
    Optional<Bank> findByBankCode(String bankCode);
    boolean existsByBankCode(String bankCode);
}
