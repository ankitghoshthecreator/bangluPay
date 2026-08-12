package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCRecord {

    @Id
    private UUID kycId;

    @Column(nullable = false, unique = true)
    private UUID customerId;

    private String aadhaarMasked;

    private String aadhaarHash;

    private String panMasked;

    private String panHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KYCStatus status;

    private String verificationNotes;

    private LocalDateTime verifiedAt;

    public enum KYCStatus {
        PENDING, VERIFIED, REJECTED
    }
}
