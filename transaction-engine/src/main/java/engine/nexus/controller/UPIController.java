package engine.nexus.controller;

import engine.nexus.model.UPIProfile;
import engine.nexus.service.UPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UPIController — REST API for VPA registration, lookup, PIN management, and block/unblock.
 */
@RestController
@RequestMapping("/api/upi")
@RequiredArgsConstructor
@Slf4j
public class UPIController {

    private final UPIService upiService;

    /**
     * POST /api/upi/register
     * Register a new VPA for a customer.
     *
     * Body:
     * {
     *   "customerId": "...",
     *   "accountId": "...",
     *   "vpa": "alice@bngl",
     *   "pin": "1234"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<UPIProfile> register(@RequestBody Map<String, String> body) {
        UUID customerId = UUID.fromString(body.get("customerId"));
        UUID accountId = UUID.fromString(body.get("accountId"));
        String vpa = body.get("vpa");
        String rawPin = body.get("pin");

        log.info("REST: Register VPA={} for customer={}", vpa, customerId);
        UPIProfile profile = upiService.registerVPA(customerId, accountId, vpa, rawPin);
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/upi/lookup/{vpa}
     * Look up a VPA. Used to verify a payee before sending money.
     */
    @GetMapping("/lookup/{vpa}")
    public ResponseEntity<UPIProfile> lookup(@PathVariable String vpa) {
        return ResponseEntity.ok(upiService.lookupVPA(vpa));
    }

    /**
     * POST /api/upi/change-pin
     * Change the UPI PIN for a VPA. Requires current PIN verification.
     *
     * Body:
     * {
     *   "customerId": "...",
     *   "vpa": "alice@bngl",
     *   "oldPin": "1234",
     *   "newPin": "5678"
     * }
     */
    @PostMapping("/change-pin")
    public ResponseEntity<UPIProfile> changePin(@RequestBody Map<String, String> body) {
        UUID customerId = UUID.fromString(body.get("customerId"));
        log.info("REST: Change PIN for VPA={} customer={}", body.get("vpa"), customerId);
        UPIProfile profile = upiService.changePin(
                customerId, body.get("vpa"), body.get("oldPin"), body.get("newPin"));
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/upi/profile/{customerId}
     * Get all UPI profiles for a customer.
     */
    @GetMapping("/profile/{customerId}")
    public ResponseEntity<List<UPIProfile>> getProfiles(@PathVariable UUID customerId) {
        return ResponseEntity.ok(upiService.getProfilesByCustomer(customerId));
    }

    /**
     * POST /api/upi/block
     * Block a VPA (admin or risk-triggered).
     * Body: { "vpa": "alice@bngl", "reason": "Suspicious activity" }
     */
    @PostMapping("/block")
    public ResponseEntity<UPIProfile> block(@RequestBody Map<String, String> body) {
        log.info("REST: Block VPA={} reason={}", body.get("vpa"), body.get("reason"));
        return ResponseEntity.ok(upiService.blockVPA(body.get("vpa"), body.get("reason")));
    }

    /**
     * POST /api/upi/unblock
     * Unblock a blocked VPA.
     * Body: { "vpa": "alice@bngl" }
     */
    @PostMapping("/unblock")
    public ResponseEntity<UPIProfile> unblock(@RequestBody Map<String, String> body) {
        log.info("REST: Unblock VPA={}", body.get("vpa"));
        return ResponseEntity.ok(upiService.unblockVPA(body.get("vpa")));
    }
}
