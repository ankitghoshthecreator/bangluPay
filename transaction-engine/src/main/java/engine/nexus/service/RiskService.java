package engine.nexus.service;

import engine.nexus.model.Account;
import engine.nexus.model.RiskAlert;
import engine.nexus.repository.AccountRepository;
import engine.nexus.repository.PaymentRepository;
import engine.nexus.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RiskService — simulates basic fraud and risk detection.
 *
 * DISCLAIMER: This is a SIMULATION of risk detection, not a production
 * anti-fraud system. Rules are simplified for demonstration purposes.
 *
 * Rules:
 * 1. HIGH_VELOCITY: >10 transactions in the last hour
 * 2. LARGE_AMOUNT: Single transaction > ₹50,000
 * 3. FROZEN_ACCOUNT_TRY: Attempt to send from a frozen account
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000.00");
    private static final int VELOCITY_LIMIT = 10;

    private final RiskAlertRepository riskAlertRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    /**
     * Check risk before allowing a payment to proceed.
     * Returns a list of created alerts (empty = clean).
     */
    public List<RiskAlert> prePaymentCheck(UUID customerId, UUID fromAccountId, BigDecimal amount) {
        java.util.List<RiskAlert> alerts = new java.util.ArrayList<>();

        // Rule 1: Large amount
        if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            RiskAlert alert = createAlert(customerId, fromAccountId, null,
                    RiskAlert.AlertType.LARGE_AMOUNT,
                    "Transaction amount ₹" + amount + " exceeds threshold of ₹" + LARGE_AMOUNT_THRESHOLD);
            alerts.add(alert);
            log.warn("[RISK] LARGE_AMOUNT alert for customer {} amount {}", customerId, amount);
        }

        // Rule 2: Velocity check (last hour)
        long recentCount = paymentRepository.findByFromAccountId(fromAccountId).stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .count();
        if (recentCount >= VELOCITY_LIMIT) {
            RiskAlert alert = createAlert(customerId, fromAccountId, null,
                    RiskAlert.AlertType.HIGH_VELOCITY,
                    "High transaction velocity: " + recentCount + " payments in last hour");
            alerts.add(alert);
            log.warn("[RISK] HIGH_VELOCITY alert for customer {} ({} transactions)", customerId, recentCount);
        }

        // Rule 3: Frozen account check
        accountRepository.findById(fromAccountId).ifPresent(acc -> {
            if (acc.getStatus() == Account.AccountStatus.FROZEN) {
                RiskAlert riskAlert = createAlert(customerId, fromAccountId, null,
                        RiskAlert.AlertType.FROZEN_ACCOUNT_TRY,
                        "Payment attempted from FROZEN account " + fromAccountId);
                alerts.add(riskAlert);
                log.warn("[RISK] FROZEN_ACCOUNT_TRY for customer {}", customerId);
            }
        });

        return alerts;
    }

    public void recordFailedPinAttempt(UUID customerId, UUID upiProfileId) {
        createAlert(customerId, null, upiProfileId,
                RiskAlert.AlertType.FAILED_PIN_LOCK,
                "Failed UPI PIN attempt recorded");
    }

    private RiskAlert createAlert(UUID customerId, UUID accountId, UUID paymentId,
                                   RiskAlert.AlertType type, String description) {
        RiskAlert alert = RiskAlert.builder()
                .customerId(customerId)
                .accountId(accountId)
                .paymentId(paymentId)
                .alertType(type)
                .description(description)
                .status(RiskAlert.AlertStatus.OPEN)
                .build();
        return riskAlertRepository.save(alert);
    }

    public List<RiskAlert> getOpenAlerts() {
        return riskAlertRepository.findByStatusOrderByCreatedAtDesc(RiskAlert.AlertStatus.OPEN);
    }

    public List<RiskAlert> getAlertsForCustomer(UUID customerId) {
        return riskAlertRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public void dismissAlert(Long alertId) {
        riskAlertRepository.findById(alertId).ifPresent(a -> {
            a.setStatus(RiskAlert.AlertStatus.DISMISSED);
            riskAlertRepository.save(a);
        });
    }
}
