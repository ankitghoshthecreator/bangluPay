package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RiskAlert — created by the risk engine when suspicious activity is detected.
 * This is informational only; it does not modify transactions retroactively.
 */
@Entity
@Table(name = "risk_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID customerId;

    private UUID accountId;

    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = AlertStatus.OPEN;
    }

    public enum AlertType {
        HIGH_VELOCITY,      // Too many transactions in short time
        LARGE_AMOUNT,       // Transaction exceeds threshold
        FAILED_PIN_LOCK,    // Too many failed PIN attempts
        FROZEN_ACCOUNT_TRY, // Attempt to pay from frozen account
        UNUSUAL_PATTERN     // Other suspicious activity
    }

    public enum AlertStatus {
        OPEN, REVIEWED, DISMISSED, ESCALATED
    }
}
