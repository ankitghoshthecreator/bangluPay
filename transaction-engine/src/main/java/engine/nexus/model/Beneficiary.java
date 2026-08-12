package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    private UUID beneficiaryId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String name;

    private String accountNumber;

    private String vpa;

    private String ifscCode;

    private String bankName;

    private String nickname;

    private BigDecimal maxLimit;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
