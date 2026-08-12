package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment — represents a single payment request going through a PaymentRail.
 * Every Payment ultimately maps to a Transaction in the TransactionEngine.
 * Immutable once it reaches a terminal state (SUCCESS, FAILED, REVERSED, REFUNDED).
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private UUID paymentId;

    /** Global idempotency key — prevents any rail from submitting twice */
    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    /** The TransactionEngine transaction that moved the money */
    private UUID transactionId;

    /** If this payment is a reversal, points to the original payment */
    private UUID reversalOf;

    /** If this payment is a refund, points to the original payment */
    private UUID refundOf;

    @Column(nullable = false)
    private UUID fromAccountId;

    @Column(nullable = false)
    private UUID toAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRailType paymentRail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String description;

    private String referenceNumber;

    /** VPA for UPI payments */
    private String senderVpa;
    private String receiverVpa;

    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        INITIATED, AUTHORIZED, PROCESSING, SUCCESS, FAILED, REVERSED, REFUNDED
    }

    public enum PaymentRailType {
        UPI, IMPS, NEFT, RTGS, INTERNAL
    }
}
