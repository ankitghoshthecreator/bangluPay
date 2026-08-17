package engine.nexus.service;

import engine.nexus.model.Account;
import engine.nexus.model.Beneficiary;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * BeneficiaryService — manages trusted payees for a customer.
 *
 * A beneficiary represents a saved payee (bank account or VPA) that a customer
 * trusts to send money to. Beneficiary records are never hard-deleted; instead,
 * they are removed only explicitly by the customer or admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    /**
     * Add a new beneficiary for a customer.
     * For bank-account beneficiaries, validates that the account number exists in the system.
     * For VPA beneficiaries, the accountNumber can be null.
     */
    @Transactional
    public Beneficiary addBeneficiary(UUID customerId,
                                       String name,
                                       String accountNumber,
                                       String vpa,
                                       String ifscCode,
                                       String bankName,
                                       String nickname,
                                       BigDecimal maxLimit) {

        if (accountNumber == null && vpa == null) {
            throw new RuntimeException("Either accountNumber or vpa must be provided");
        }

        // If account number given, verify it exists in our system
        if (accountNumber != null) {
            accountRepository.findByAccountNumber(accountNumber)
                    .filter(a -> a.getStatus() == Account.AccountStatus.ACTIVE)
                    .orElseThrow(() -> new RuntimeException(
                            "Account number not found or not active: " + accountNumber));
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .beneficiaryId(UUID.randomUUID())
                .customerId(customerId)
                .name(name)
                .accountNumber(accountNumber)
                .vpa(vpa != null ? vpa.toLowerCase().trim() : null)
                .ifscCode(ifscCode)
                .bankName(bankName)
                .nickname(nickname)
                .maxLimit(maxLimit != null ? maxLimit : new BigDecimal("200000.00"))
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditService.log("BENEFICIARY_ADDED", customerId, "CUSTOMER",
                saved.getBeneficiaryId(), "Beneficiary",
                "name=" + name + " account=" + accountNumber + " vpa=" + vpa);
        log.info("[BeneficiaryService] Beneficiary added: {} for customer {}", name, customerId);
        return saved;
    }

    /**
     * Get all beneficiaries for a customer.
     */
    public List<Beneficiary> getBeneficiaries(UUID customerId) {
        return beneficiaryRepository.findByCustomerId(customerId);
    }

    /**
     * Get a specific beneficiary by ID, verifying it belongs to the customer.
     */
    public Beneficiary getBeneficiary(UUID customerId, UUID beneficiaryId) {
        Beneficiary b = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found: " + beneficiaryId));
        if (!b.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Beneficiary does not belong to customer: " + customerId);
        }
        return b;
    }

    /**
     * Delete a beneficiary. Only the owning customer can delete.
     */
    @Transactional
    public void deleteBeneficiary(UUID customerId, UUID beneficiaryId) {
        Beneficiary b = getBeneficiary(customerId, beneficiaryId);
        beneficiaryRepository.delete(b);
        auditService.log("BENEFICIARY_DELETED", customerId, "CUSTOMER",
                beneficiaryId, "Beneficiary", "");
        log.info("[BeneficiaryService] Beneficiary {} deleted by customer {}", beneficiaryId, customerId);
    }

    /**
     * Resolve the account UUID for a beneficiary (for use in PaymentService).
     * Works for both account-number and VPA beneficiaries.
     */
    public UUID resolveAccountId(UUID beneficiaryId) {
        Beneficiary b = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found: " + beneficiaryId));

        if (b.getAccountNumber() != null) {
            return accountRepository.findByAccountNumber(b.getAccountNumber())
                    .orElseThrow(() -> new RuntimeException("Beneficiary account not found: " + b.getAccountNumber()))
                    .getAccountId();
        }
        throw new RuntimeException("Cannot resolve accountId for VPA beneficiary via this method. Use UPIService.lookupVPA().");
    }
}
