package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog — append-only record of all significant system events.
 * This is never deleted or modified after creation.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;      // e.g. TRANSFER, DEPOSIT, KYC_VERIFIED, ACCOUNT_FROZEN

    private UUID actorId;       // Who performed the action (customerId or null for SYSTEM)

    private String actorRole;   // CUSTOMER, BANK_ADMIN, SYSTEM

    private UUID targetId;      // What entity was affected

    private String targetType;  // Account, Customer, Payment, UPIProfile

    private String details;     // JSON or text summary

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
