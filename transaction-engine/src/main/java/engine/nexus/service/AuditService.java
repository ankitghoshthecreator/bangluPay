package engine.nexus.service;

import engine.nexus.model.AuditLog;
import engine.nexus.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AuditService — append-only logging of all significant system events.
 * AuditLog records are never modified or deleted.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, UUID actorId, String actorRole, UUID targetId, String targetType, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorRole(actorRole)
                .targetId(targetId)
                .targetType(targetType)
                .details(details)
                .build());
    }

    public java.util.List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public java.util.List<AuditLog> getLogsByTarget(UUID targetId) {
        return auditLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
    }
}
