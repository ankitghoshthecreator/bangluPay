package engine.nexus.repository;

import engine.nexus.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<RiskAlert> findByStatusOrderByCreatedAtDesc(RiskAlert.AlertStatus status);
}
