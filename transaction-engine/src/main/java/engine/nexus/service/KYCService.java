package engine.nexus.service;

import engine.nexus.kyc.KYCVerificationResult;
import engine.nexus.kyc.OfflineAadhaarVerificationSimulator;
import engine.nexus.kyc.OfflinePanVerificationSimulator;
import engine.nexus.model.Customer;
import engine.nexus.model.KYCRecord;
import engine.nexus.repository.CustomerRepository;
import engine.nexus.repository.KYCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KYCService {

    private final KYCRepository kycRepository;
    private final CustomerRepository customerRepository;
    private final OfflineAadhaarVerificationSimulator aadhaarSimulator;
    private final OfflinePanVerificationSimulator panSimulator;
    private final AuditService auditService;

    @Transactional
    public KYCRecord initiateKYC(UUID customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        return kycRepository.findByCustomerId(customerId).orElseGet(() -> {
            KYCRecord kyc = KYCRecord.builder()
                    .kycId(UUID.randomUUID())
                    .customerId(customerId)
                    .status(KYCRecord.KYCStatus.PENDING)
                    .build();
            return kycRepository.save(kyc);
        });
    }

    @Transactional
    public KYCRecord verifyAadhaar(UUID customerId, String rawAadhaar) {
        KYCRecord kyc = kycRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No KYC record found. Call initiateKYC first."));

        KYCVerificationResult result = aadhaarSimulator.verify(rawAadhaar);

        if (result.passed()) {
            kyc.setAadhaarMasked(result.maskedIdentifier());
            kyc.setAadhaarHash(result.identifierHash());
            kyc.setVerificationNotes("Aadhaar: " + result.reason());
        } else {
            kyc.setVerificationNotes("Aadhaar FAILED: " + result.reason());
            log.warn("Aadhaar verification failed for customer {}: {}", customerId, result.reason());
        }

        updateKYCStatus(kyc);
        kycRepository.save(kyc);
        auditService.log("KYC_AADHAAR_" + (result.passed() ? "PASSED" : "FAILED"), customerId, "CUSTOMER", customerId, "KYCRecord", result.reason());
        return kyc;
    }

    @Transactional
    public KYCRecord verifyPAN(UUID customerId, String rawPan) {
        KYCRecord kyc = kycRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No KYC record found. Call initiateKYC first."));

        KYCVerificationResult result = panSimulator.verify(rawPan);

        if (result.passed()) {
            kyc.setPanMasked(result.maskedIdentifier());
            kyc.setPanHash(result.identifierHash());
            kyc.setVerificationNotes((kyc.getVerificationNotes() != null ? kyc.getVerificationNotes() + " | " : "") + "PAN: " + result.reason());
        } else {
            kyc.setVerificationNotes((kyc.getVerificationNotes() != null ? kyc.getVerificationNotes() + " | " : "") + "PAN FAILED: " + result.reason());
            log.warn("PAN verification failed for customer {}: {}", customerId, result.reason());
        }

        updateKYCStatus(kyc);
        kycRepository.save(kyc);
        auditService.log("KYC_PAN_" + (result.passed() ? "PASSED" : "FAILED"), customerId, "CUSTOMER", customerId, "KYCRecord", result.reason());
        return kyc;
    }

    private void updateKYCStatus(KYCRecord kyc) {
        boolean aadhaarOk = kyc.getAadhaarMasked() != null;
        boolean panOk = kyc.getPanMasked() != null;

        if (aadhaarOk && panOk) {
            kyc.setStatus(KYCRecord.KYCStatus.VERIFIED);
            kyc.setVerifiedAt(LocalDateTime.now());
            // Activate the customer
            customerRepository.findById(kyc.getCustomerId()).ifPresent(c -> {
                c.setStatus(Customer.CustomerStatus.ACTIVE);
                customerRepository.save(c);
                log.info("Customer {} activated after KYC completion", kyc.getCustomerId());
            });
        }
    }

    public KYCRecord getKYC(UUID customerId) {
        return kycRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No KYC record for customer: " + customerId));
    }
}
