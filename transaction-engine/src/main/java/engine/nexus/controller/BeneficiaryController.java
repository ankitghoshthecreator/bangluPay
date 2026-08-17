package engine.nexus.controller;

import engine.nexus.model.Beneficiary;
import engine.nexus.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BeneficiaryController — REST API for managing trusted payees.
 */
@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    /**
     * POST /api/beneficiaries
     * Add a new beneficiary for a customer.
     *
     * Body:
     * {
     *   "customerId": "...",
     *   "name": "Bob Kumar",
     *   "accountNumber": "BNGL10000002",   // optional if vpa provided
     *   "vpa": "bob@bngl",                 // optional if accountNumber provided
     *   "ifscCode": "BNGL0001001",
     *   "bankName": "BangluPay Bank",
     *   "nickname": "Bob",
     *   "maxLimit": "50000.00"             // optional, defaults to ₹2,00,000
     * }
     */
    @PostMapping
    public ResponseEntity<Beneficiary> add(@RequestBody Map<String, String> body) {
        UUID customerId = UUID.fromString(body.get("customerId"));
        BigDecimal maxLimit = body.containsKey("maxLimit")
                ? new BigDecimal(body.get("maxLimit")) : null;

        log.info("REST: Add beneficiary for customer={} name={}", customerId, body.get("name"));
        Beneficiary b = beneficiaryService.addBeneficiary(
                customerId,
                body.get("name"),
                body.get("accountNumber"),
                body.get("vpa"),
                body.get("ifscCode"),
                body.get("bankName"),
                body.get("nickname"),
                maxLimit
        );
        return ResponseEntity.ok(b);
    }

    /**
     * GET /api/beneficiaries/customer/{customerId}
     * List all beneficiaries for a customer.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Beneficiary>> list(@PathVariable UUID customerId) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(customerId));
    }

    /**
     * GET /api/beneficiaries/{id}
     * Get a specific beneficiary (verifies ownership by customerId query param).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Beneficiary> get(@PathVariable UUID id,
                                            @RequestParam UUID customerId) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiary(customerId, id));
    }

    /**
     * DELETE /api/beneficiaries/{id}
     * Delete a beneficiary (only the owning customer can delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        @RequestParam UUID customerId) {
        log.info("REST: Delete beneficiary id={} by customer={}", id, customerId);
        beneficiaryService.deleteBeneficiary(customerId, id);
        return ResponseEntity.noContent().build();
    }
}
