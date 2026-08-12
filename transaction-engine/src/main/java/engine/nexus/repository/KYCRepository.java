package engine.nexus.repository;

import engine.nexus.model.KYCRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface KYCRepository extends JpaRepository<KYCRecord, UUID> {
    Optional<KYCRecord> findByCustomerId(UUID customerId);
}
