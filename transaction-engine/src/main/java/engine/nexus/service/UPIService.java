package engine.nexus.service;

import engine.nexus.model.UPIProfile;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.UPIProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UPIService — manages Virtual Payment Address (VPA) lifecycle and PIN operations.
 *
 * GUARDRAIL: Raw UPI PINs are NEVER stored or logged. Only BCrypt hashes are persisted.
 *
 * VPA format expected: localpart@bankcode (e.g., alice@bngl, rahul@sbin)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UPIService {

    private final UPIProfileRepository upiProfileRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new VPA for a customer linked to a specific bank account.
     * The UPI PIN is immediately hashed; the raw PIN is discarded.
     */
    @Transactional
    public UPIProfile registerVPA(UUID customerId, UUID accountId, String vpa, String rawPin) {
        // Validate the linked account exists and is active
        accountRepository.findById(accountId)
                .filter(a -> a.getStatus() == engine.nexus.model.Account.AccountStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Account not found or not active: " + accountId));

        // Validate account belongs to customer
        accountRepository.findById(accountId).ifPresent(a -> {
            if (a.getCustomerId() != null && !a.getCustomerId().equals(customerId)) {
                throw new RuntimeException("Account " + accountId + " does not belong to customer " + customerId);
            }
        });

        // VPA uniqueness
        if (upiProfileRepository.existsByVpa(vpa)) {
            throw new RuntimeException("VPA already registered: " + vpa);
        }

        // PIN validation
        if (rawPin == null || rawPin.length() < 4 || rawPin.length() > 6 || !rawPin.matches("\\d+")) {
            throw new RuntimeException("UPI PIN must be 4–6 digits");
        }

        UPIProfile profile = UPIProfile.builder()
                .upiId(UUID.randomUUID())
                .customerId(customerId)
                .vpa(vpa.toLowerCase().trim())
                .linkedAccountId(accountId)
                .pinHash(passwordEncoder.encode(rawPin))  // BCrypt — raw PIN never stored
                .failedPinAttempts(0)
                .status(UPIProfile.UPIStatus.ACTIVE)
                .build();

        UPIProfile saved = upiProfileRepository.save(profile);
        auditService.log("UPI_VPA_REGISTERED", customerId, "CUSTOMER", saved.getUpiId(), "UPIProfile", "vpa=" + vpa);
        log.info("[UPIService] VPA registered: {} for customer {} → account {}", vpa, customerId, accountId);
        return saved;
    }

    /**
     * Look up a VPA and return its profile.
     * Used to resolve receiver VPA before initiating a payment.
     */
    public UPIProfile lookupVPA(String vpa) {
        return upiProfileRepository.findByVpa(vpa.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("VPA not found: " + vpa));
    }

    /** Returns true if the raw PIN matches the stored hash. */
    public boolean verifyPin(UUID upiProfileId, String rawPin) {
        UPIProfile profile = upiProfileRepository.findById(upiProfileId)
                .orElseThrow(() -> new RuntimeException("UPI profile not found: " + upiProfileId));
        return passwordEncoder.matches(rawPin, profile.getPinHash());
    }

    /**
     * Change the UPI PIN — requires the current PIN to be verified first.
     * Raw PINs are never stored; only BCrypt hashes are persisted.
     */
    @Transactional
    public UPIProfile changePin(UUID customerId, String vpa, String oldRawPin, String newRawPin) {
        UPIProfile profile = upiProfileRepository.findByVpa(vpa.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("VPA not found: " + vpa));

        if (!profile.getCustomerId().equals(customerId)) {
            throw new RuntimeException("VPA does not belong to this customer");
        }
        if (!passwordEncoder.matches(oldRawPin, profile.getPinHash())) {
            throw new RuntimeException("Current PIN is incorrect");
        }
        if (newRawPin == null || newRawPin.length() < 4 || newRawPin.length() > 6 || !newRawPin.matches("\\d+")) {
            throw new RuntimeException("New UPI PIN must be 4–6 digits");
        }

        profile.setPinHash(passwordEncoder.encode(newRawPin));
        profile.setFailedPinAttempts(0);
        UPIProfile saved = upiProfileRepository.save(profile);

        auditService.log("UPI_PIN_CHANGED", customerId, "CUSTOMER", profile.getUpiId(), "UPIProfile", "vpa=" + vpa);
        log.info("[UPIService] PIN changed for VPA: {} customer: {}", vpa, customerId);
        return saved;
    }

    /** Block a VPA. No payments can be initiated from/to a BLOCKED VPA. */
    @Transactional
    public UPIProfile blockVPA(String vpa, String reason) {
        UPIProfile profile = upiProfileRepository.findByVpa(vpa.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("VPA not found: " + vpa));

        profile.setStatus(UPIProfile.UPIStatus.BLOCKED);
        UPIProfile saved = upiProfileRepository.save(profile);
        auditService.log("UPI_VPA_BLOCKED", profile.getCustomerId(), "BANK_ADMIN",
                profile.getUpiId(), "UPIProfile", "reason=" + reason);
        log.warn("[UPIService] VPA BLOCKED: {} reason={}", vpa, reason);
        return saved;
    }

    /** Unblock a previously blocked VPA. */
    @Transactional
    public UPIProfile unblockVPA(String vpa) {
        UPIProfile profile = upiProfileRepository.findByVpa(vpa.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("VPA not found: " + vpa));

        if (profile.getStatus() != UPIProfile.UPIStatus.BLOCKED) {
            throw new RuntimeException("VPA is not blocked");
        }
        profile.setStatus(UPIProfile.UPIStatus.ACTIVE);
        profile.setFailedPinAttempts(0);
        UPIProfile saved = upiProfileRepository.save(profile);
        auditService.log("UPI_VPA_UNBLOCKED", profile.getCustomerId(), "BANK_ADMIN",
                profile.getUpiId(), "UPIProfile", "");
        return saved;
    }

    /** Get the UPI profile(s) for a customer. */
    public List<UPIProfile> getProfilesByCustomer(UUID customerId) {
        return upiProfileRepository.findByCustomerId(customerId);
    }
}
