package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    private UUID branchId;

    @Column(nullable = false)
    private UUID bankId;

    @Column(nullable = false)
    private String branchName;

    @Column(unique = true, nullable = false)
    private String ifscCode;

    private String city;
}
