package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    private UUID accountId;

    @Column(unique = true)
    private String accountNumber; // e.g. BNGL0000001

    private String holderName;   // Denormalized display name

    private UUID customerId;     // Link to Customer entity

    private UUID bankId;         // Link to Bank entity

    private UUID branchId;       // Link to Branch entity

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version
    private Long version;        // Optimistic locking for concurrency safety

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = AccountStatus.ACTIVE;
        if (accountType == null) accountType = AccountType.SAVINGS;
        if (balance == null) balance = BigDecimal.ZERO.setScale(2);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AccountType {
        SAVINGS, CURRENT, SALARY, BASIC
    }

    /**
     * Full account lifecycle states.
     * ACTIVE      - Normal operational state.
     * FROZEN      - Temporarily blocked; no debits or credits.
     * SUSPENDED   - Temporarily restricted; admin intervention required.
     * CLOSURE_REQUESTED - Customer has requested account closure.
     * CLOSED      - Account fully closed; records preserved immutably.
     */
    public enum AccountStatus {
        PENDING, ACTIVE, FROZEN, SUSPENDED, CLOSURE_REQUESTED, CLOSED
    }
}
