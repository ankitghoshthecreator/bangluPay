package engine.nexus.repository;

import engine.nexus.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);
    List<AuditLog> findByTargetIdOrderByCreatedAtDesc(UUID targetId);
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
