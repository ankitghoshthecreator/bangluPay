package engine.nexus.repository;

import engine.nexus.model.UPIProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UPIProfileRepository extends JpaRepository<UPIProfile, UUID> {
    Optional<UPIProfile> findByVpa(String vpa);
    List<UPIProfile> findByCustomerId(UUID customerId);
    boolean existsByVpa(String vpa);
}
