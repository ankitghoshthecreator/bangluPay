package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upi_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UPIProfile {

    @Id
    private UUID upiId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, unique = true)
    private String vpa; // e.g., alice@bngl

    @Column(nullable = false)
    private UUID linkedAccountId;

    @Column(nullable = false)
    private String pinHash;

    private int failedPinAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UPIStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UPIStatus.ACTIVE;
        }
    }

    public enum UPIStatus {
        ACTIVE, BLOCKED, FROZEN
    }
}
