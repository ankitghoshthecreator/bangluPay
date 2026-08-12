package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private EntryType type;

    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum EntryType {
        DEBIT, CREDIT
    }
}
