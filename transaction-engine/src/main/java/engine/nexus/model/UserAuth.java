package engine.nexus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserAuth — stores authentication credentials for a customer.
 * Passwords and PINs are NEVER stored in plaintext.
 * BCrypt hash is stored. The original credential is never recoverable.
 */
@Entity
@Table(name = "user_auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuth {

    @Id
    private UUID authId;

    @Column(nullable = false, unique = true)
    private UUID customerId;

    @Column(nullable = false, unique = true)
    private String username;  // usually mobile number or email

    @Column(nullable = false)
    private String passwordHash;  // BCrypt

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private int failedLoginAttempts;

    private boolean locked;

    private LocalDateTime lockedUntil;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum UserRole {
        CUSTOMER, BANK_EMPLOYEE, BANK_ADMIN, AUDITOR, SYSTEM_ADMIN
    }
}
