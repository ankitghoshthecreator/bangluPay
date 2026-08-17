package engine.nexus.controller;

import engine.nexus.model.KYCRecord;
import engine.nexus.service.KYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * KYCController — REST API for customer identity verification.
 *
 * KYC flow:
 *   POST /initiate  → creates PENDING KYC record
 *   POST /aadhaar   → verifies Aadhaar (offline simulation)
 *   POST /pan       → verifies PAN (offline simulation)
 *   GET  /          → check current KYC status
 *
 * When both Aadhaar and PAN are verified, customer status automatically moves to ACTIVE.
 */
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
public class KYCController {

    private final KYCService kycService;

    /**
     * POST /api/kyc/{customerId}/initiate
     * Create or retrieve the KYC record for a customer.
     */
    @PostMapping("/{customerId}/initiate")
    public ResponseEntity<KYCRecord> initiate(@PathVariable UUID customerId) {
        log.info("REST: Initiate KYC for customer={}", customerId);
        return ResponseEntity.ok(kycService.initiateKYC(customerId));
    }

    /**
     * POST /api/kyc/{customerId}/aadhaar
     * Body: { "aadhaar": "123456789012" }
     *
     * Verifies the Aadhaar number using offline checksum simulation.
     * The raw number is hashed (BCrypt) and masked — never stored as plaintext.
     */
    @PostMapping("/{customerId}/aadhaar")
    public ResponseEntity<KYCRecord> verifyAadhaar(@PathVariable UUID customerId,
                                                     @RequestBody Map<String, String> body) {
        log.info("REST: Aadhaar verification for customer={}", customerId);
        KYCRecord record = kycService.verifyAadhaar(customerId, body.get("aadhaar"));
        return ResponseEntity.ok(record);
    }

    /**
     * POST /api/kyc/{customerId}/pan
     * Body: { "pan": "ABCDE1234F" }
     *
     * Verifies the PAN using offline format simulation.
     * The raw PAN is hashed and masked — never stored as plaintext.
     */
    @PostMapping("/{customerId}/pan")
    public ResponseEntity<KYCRecord> verifyPAN(@PathVariable UUID customerId,
                                                @RequestBody Map<String, String> body) {
        log.info("REST: PAN verification for customer={}", customerId);
        KYCRecord record = kycService.verifyPAN(customerId, body.get("pan"));
        return ResponseEntity.ok(record);
    }

    /**
     * GET /api/kyc/{customerId}
     * Get the current KYC status for a customer.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<KYCRecord> getKYCStatus(@PathVariable UUID customerId) {
        return ResponseEntity.ok(kycService.getKYC(customerId));
    }
}
