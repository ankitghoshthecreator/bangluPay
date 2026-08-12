package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {

    @Id
    private UUID bankId;

    @Column(unique = true, nullable = false)
    private String bankCode; // BNGL, SBIN, HDFC, ICIC

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String ifscPrefix;
}
