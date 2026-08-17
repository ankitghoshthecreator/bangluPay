package engine.nexus.controller;

import engine.nexus.model.Payment;
import engine.nexus.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PaymentController — REST API for payment initiation, status, history, and reversal.
 *
 * All endpoints require an idempotencyKey in the request body to prevent duplicate processing.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/transfer
     * Initiate a payment via INTERNAL, IMPS, NEFT, or RTGS rail.
     *
     * Body:
     * {
     *   "customerId": "...",
     *   "fromAccountId": "...",
     *   "toAccountId": "...",
     *   "amount": "5000.00",
     *   "rail": "IMPS",
     *   "idempotencyKey": "client-unique-key-001",
     *   "description": "Rent payment"
     * }
     */
    @PostMapping("/transfer")
    public ResponseEntity<Payment> transfer(@RequestBody Map<String, String> body) {
        UUID customerId = UUID.fromString(body.get("customerId"));
        UUID fromAccountId = UUID.fromString(body.get("fromAccountId"));
        UUID toAccountId = UUID.fromString(body.get("toAccountId"));
        BigDecimal amount = new BigDecimal(body.get("amount"));
        Payment.PaymentRailType rail = Payment.PaymentRailType.valueOf(body.get("rail").toUpperCase());
        String idempotencyKey = body.get("idempotencyKey");
        String description = body.getOrDefault("description", "Transfer");

        log.info("REST: {} transfer from={} to={} amount={} key={}", rail, fromAccountId, toAccountId, amount, idempotencyKey);
        Payment payment = paymentService.initiatePayment(
                customerId, fromAccountId, toAccountId, amount, rail, idempotencyKey, description);
        return ResponseEntity.ok(payment);
    }

    /**
     * POST /api/payments/upi
     * Initiate a UPI payment using Virtual Payment Addresses.
     *
     * Body:
     * {
     *   "senderVpa": "alice@bngl",
     *   "receiverVpa": "bob@bngl",
     *   "amount": "2000.00",
     *   "pin": "1234",
     *   "idempotencyKey": "client-unique-key-002",
     *   "description": "Dinner split"
     * }
     */
    @PostMapping("/upi")
    public ResponseEntity<Payment> upiPay(@RequestBody Map<String, String> body) {
        String senderVpa = body.get("senderVpa");
        String receiverVpa = body.get("receiverVpa");
        BigDecimal amount = new BigDecimal(body.get("amount"));
        String rawPin = body.get("pin");
        String idempotencyKey = body.get("idempotencyKey");
        String description = body.getOrDefault("description", "UPI Payment");

        log.info("REST: UPI payment {}→{} amount={} key={}", senderVpa, receiverVpa, amount, idempotencyKey);
        Payment payment = paymentService.initiateUPIPayment(
                senderVpa, receiverVpa, amount, rawPin, idempotencyKey, description);
        return ResponseEntity.ok(payment);
    }

    /**
     * POST /api/payments/{id}/reverse
     * Reverse a successful payment.
     * Body: { "reason": "Accidental transfer" }
     */
    @PostMapping("/{id}/reverse")
    public ResponseEntity<Payment> reverse(@PathVariable UUID id,
                                            @RequestBody Map<String, String> body) {
        log.info("REST: Reverse payment id={} reason={}", id, body.get("reason"));
        Payment reversal = paymentService.reversePayment(id, body.getOrDefault("reason", "Reversal requested"));
        return ResponseEntity.ok(reversal);
    }

    /**
     * GET /api/payments/{id}
     * Get payment details by payment ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    /**
     * GET /api/payments/account/{accountId}
     * Get all payments (sent and received) for an account.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Payment>> getHistory(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(accountId));
    }
}
