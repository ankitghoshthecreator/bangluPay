package engine.nexus.service;

import engine.nexus.model.Account;
import engine.nexus.model.Payment;
import engine.nexus.model.RiskAlert;
import engine.nexus.model.Transaction;
import engine.nexus.model.UPIProfile;
import engine.nexus.payment.PaymentRail;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.PaymentRepository;
import engine.nexus.repository.UPIProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PaymentService — the central orchestrator for all payment operations.
 *
 * Flow for every payment:
 *   1. Idempotency check (payment-level, independent of TransactionEngine)
 *   2. RiskService pre-check
 *   3. PaymentRail validation (limits, currency)
 *   4. UPI PIN verification (for UPI rail only)
 *   5. TransactionEngine.processTransfer() — the only place money moves
 *   6. Payment record updated to SUCCESS/FAILED
 *
 * GUARDRAIL: This service NEVER directly mutates account balances.
 * All money movement is delegated to TransactionEngine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final UPIProfileRepository upiProfileRepository;
    private final TransactionEngine transactionEngine;
    private final RiskService riskService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    // Injected as a map from PaymentRailType → PaymentRail implementation
    private final List<PaymentRail> paymentRails;

    private Map<Payment.PaymentRailType, PaymentRail> getRailMap() {
        return paymentRails.stream()
                .collect(Collectors.toMap(PaymentRail::getRailType, Function.identity()));
    }

    /**
     * Initiate a payment via any rail (INTERNAL, IMPS, NEFT, RTGS).
     * For UPI payments, use initiateUPIPayment() instead.
     */
    @Transactional
    public Payment initiatePayment(UUID customerId,
                                   UUID fromAccountId,
                                   UUID toAccountId,
                                   BigDecimal amount,
                                   Payment.PaymentRailType railType,
                                   String idempotencyKey,
                                   String description) {

        log.info("[PaymentService] Initiating {} payment: from={} to={} amount={} key={}",
                railType, fromAccountId, toAccountId, amount, idempotencyKey);

        // 1. Idempotency — return existing payment for duplicate key
        return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {

            Payment payment = Payment.builder()
                    .paymentId(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .amount(amount)
                    .currency("INR")
                    .paymentRail(railType)
                    .status(Payment.PaymentStatus.INITIATED)
                    .description(description)
                    .referenceNumber("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build();
            paymentRepository.save(payment);

            try {
                // 2. Risk check
                List<RiskAlert> alerts = riskService.prePaymentCheck(customerId, fromAccountId, amount);
                boolean blocked = alerts.stream().anyMatch(a ->
                        a.getAlertType() == RiskAlert.AlertType.FROZEN_ACCOUNT_TRY);
                if (blocked) {
                    return failPayment(payment, "Payment blocked by risk engine: FROZEN_ACCOUNT_TRY");
                }

                // 3. Rail validation
                PaymentRail rail = getRailMap().get(railType);
                if (rail == null) {
                    return failPayment(payment, "Unsupported payment rail: " + railType);
                }
                rail.validate(fromAccountId, toAccountId, amount, "INR");

                // 4. Account state check
                Account from = accountRepository.findById(fromAccountId)
                        .orElseThrow(() -> new RuntimeException("Source account not found"));
                if (from.getStatus() != Account.AccountStatus.ACTIVE) {
                    return failPayment(payment, "Source account is not active: " + from.getStatus());
                }

                // 5. Update to AUTHORIZED then PROCESSING
                payment.setStatus(Payment.PaymentStatus.AUTHORIZED);
                paymentRepository.save(payment);
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                paymentRepository.save(payment);

                // 6. Execute via TransactionEngine (idempotent with payment UUID as external ID)
                Transaction tx = transactionEngine.processTransfer(
                        "PAY-" + payment.getPaymentId(),
                        fromAccountId, toAccountId, amount, description
                );
                payment.setTransactionId(tx.getTransactionId());
                payment.setStatus(Payment.PaymentStatus.SUCCESS);
                Payment saved = paymentRepository.save(payment);

                auditService.log("PAYMENT_SUCCESS", customerId, "CUSTOMER",
                        payment.getPaymentId(), "Payment",
                        "rail=" + railType + " amount=" + amount + " ref=" + payment.getReferenceNumber());
                log.info("[PaymentService] Payment SUCCESS: {} via {} amount={}", payment.getPaymentId(), railType, amount);
                return saved;

            } catch (Exception e) {
                log.error("[PaymentService] Payment FAILED: {} reason={}", payment.getPaymentId(), e.getMessage());
                auditService.log("PAYMENT_FAILED", customerId, "CUSTOMER",
                        payment.getPaymentId(), "Payment", e.getMessage());
                return failPayment(payment, e.getMessage());
            }
        });
    }

    /**
     * Initiate a UPI payment using Virtual Payment Addresses.
     * Requires UPI PIN verification before any money moves.
     */
    @Transactional
    public Payment initiateUPIPayment(String senderVpa,
                                      String receiverVpa,
                                      BigDecimal amount,
                                      String rawPin,
                                      String idempotencyKey,
                                      String description) {

        log.info("[PaymentService] UPI payment: {} → {} amount={}", senderVpa, receiverVpa, amount);

        // 1. Idempotency
        return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {

            // 2. Resolve VPAs
            UPIProfile senderProfile = upiProfileRepository.findByVpa(senderVpa)
                    .orElseThrow(() -> new RuntimeException("Sender VPA not found: " + senderVpa));
            UPIProfile receiverProfile = upiProfileRepository.findByVpa(receiverVpa)
                    .orElseThrow(() -> new RuntimeException("Receiver VPA not found: " + receiverVpa));

            if (senderProfile.getStatus() != UPIProfile.UPIStatus.ACTIVE) {
                throw new RuntimeException("Sender UPI profile is not active: " + senderProfile.getStatus());
            }
            if (receiverProfile.getStatus() != UPIProfile.UPIStatus.ACTIVE) {
                throw new RuntimeException("Receiver UPI profile is not active: " + receiverProfile.getStatus());
            }

            UUID fromAccountId = senderProfile.getLinkedAccountId();
            UUID toAccountId = receiverProfile.getLinkedAccountId();
            UUID customerId = senderProfile.getCustomerId();

            Payment payment = Payment.builder()
                    .paymentId(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .amount(amount)
                    .currency("INR")
                    .paymentRail(Payment.PaymentRailType.UPI)
                    .status(Payment.PaymentStatus.INITIATED)
                    .description(description)
                    .senderVpa(senderVpa)
                    .receiverVpa(receiverVpa)
                    .referenceNumber("UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build();
            paymentRepository.save(payment);

            try {
                // 3. UPI PIN verification — before any money moves
                if (!passwordEncoder.matches(rawPin, senderProfile.getPinHash())) {
                    senderProfile.setFailedPinAttempts(senderProfile.getFailedPinAttempts() + 1);
                    if (senderProfile.getFailedPinAttempts() >= 3) {
                        senderProfile.setStatus(UPIProfile.UPIStatus.BLOCKED);
                        riskService.recordFailedPinAttempt(customerId, senderProfile.getUpiId());
                        log.warn("[PaymentService] UPI profile {} BLOCKED after {} failed PIN attempts",
                                senderVpa, senderProfile.getFailedPinAttempts());
                    }
                    upiProfileRepository.save(senderProfile);
                    return failPayment(payment, "Invalid UPI PIN. Attempts: " + senderProfile.getFailedPinAttempts());
                }

                // Reset failed attempts on success
                senderProfile.setFailedPinAttempts(0);
                upiProfileRepository.save(senderProfile);

                // 4. Risk check
                List<RiskAlert> alerts = riskService.prePaymentCheck(customerId, fromAccountId, amount);
                boolean blocked = alerts.stream().anyMatch(a ->
                        a.getAlertType() == RiskAlert.AlertType.FROZEN_ACCOUNT_TRY);
                if (blocked) {
                    return failPayment(payment, "Payment blocked by risk engine");
                }

                // 5. UPI rail validation
                PaymentRail upiRail = getRailMap().get(Payment.PaymentRailType.UPI);
                upiRail.validate(fromAccountId, toAccountId, amount, "INR");

                // 6. Authorize → Process → Execute
                payment.setStatus(Payment.PaymentStatus.AUTHORIZED);
                paymentRepository.save(payment);
                payment.setStatus(Payment.PaymentStatus.PROCESSING);
                paymentRepository.save(payment);

                Transaction tx = transactionEngine.processTransfer(
                        "PAY-" + payment.getPaymentId(),
                        fromAccountId, toAccountId, amount, description
                );
                payment.setTransactionId(tx.getTransactionId());
                payment.setStatus(Payment.PaymentStatus.SUCCESS);
                Payment saved = paymentRepository.save(payment);

                auditService.log("UPI_PAYMENT_SUCCESS", customerId, "CUSTOMER",
                        payment.getPaymentId(), "Payment",
                        senderVpa + "→" + receiverVpa + " amount=" + amount);
                log.info("[PaymentService] UPI SUCCESS: {} → {} amount={} ref={}",
                        senderVpa, receiverVpa, amount, payment.getReferenceNumber());
                return saved;

            } catch (Exception e) {
                log.error("[PaymentService] UPI payment FAILED: {} reason={}", payment.getPaymentId(), e.getMessage());
                auditService.log("UPI_PAYMENT_FAILED", senderProfile.getCustomerId(), "CUSTOMER",
                        payment.getPaymentId(), "Payment", e.getMessage());
                return failPayment(payment, e.getMessage());
            }
        });
    }

    /**
     * Reverse a payment that has reached SUCCESS state.
     * The reversal is a new payment in the opposite direction using SYSTEM_REVERSAL as intermediate.
     */
    @Transactional
    public Payment reversePayment(UUID paymentId, String reason) {
        Payment original = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (original.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Only SUCCESS payments can be reversed. Current status: " + original.getStatus());
        }

        log.info("[PaymentService] Reversing payment {} reason={}", paymentId, reason);

        // Build reversal payment record
        String reversalKey = "REVERSAL-" + paymentId;
        return paymentRepository.findByIdempotencyKey(reversalKey).orElseGet(() -> {

            Payment reversal = Payment.builder()
                    .paymentId(UUID.randomUUID())
                    .idempotencyKey(reversalKey)
                    .fromAccountId(original.getToAccountId())   // reversed direction
                    .toAccountId(original.getFromAccountId())
                    .amount(original.getAmount())
                    .currency(original.getCurrency())
                    .paymentRail(original.getPaymentRail())
                    .status(Payment.PaymentStatus.INITIATED)
                    .description("Reversal of " + paymentId + ": " + reason)
                    .reversalOf(paymentId)
                    .referenceNumber("REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build();
            paymentRepository.save(reversal);

            try {
                reversal.setStatus(Payment.PaymentStatus.PROCESSING);
                paymentRepository.save(reversal);

                Transaction tx = transactionEngine.processTransfer(
                        "PAY-" + reversal.getPaymentId(),
                        reversal.getFromAccountId(), reversal.getToAccountId(),
                        reversal.getAmount(), reversal.getDescription()
                );
                reversal.setTransactionId(tx.getTransactionId());
                reversal.setStatus(Payment.PaymentStatus.REVERSED);

                // Mark original as reversed
                original.setStatus(Payment.PaymentStatus.REVERSED);
                paymentRepository.save(original);

                Payment saved = paymentRepository.save(reversal);
                auditService.log("PAYMENT_REVERSED", null, "BANK_ADMIN",
                        paymentId, "Payment", reason);
                log.info("[PaymentService] Reversal SUCCESS for original payment {}", paymentId);
                return saved;

            } catch (Exception e) {
                log.error("[PaymentService] Reversal FAILED for payment {}: {}", paymentId, e.getMessage());
                return failPayment(reversal, e.getMessage());
            }
        });
    }

    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public List<Payment> getPaymentHistory(UUID accountId) {
        return paymentRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.PROCESSING);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Payment failPayment(Payment payment, String reason) {
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        return paymentRepository.save(payment);
    }
}
