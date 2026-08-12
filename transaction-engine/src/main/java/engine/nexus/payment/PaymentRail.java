package engine.nexus.payment;

import engine.nexus.model.Payment;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * PaymentRail — the abstraction every payment rail must implement.
 *
 * GUARDRAIL: A PaymentRail MUST NOT directly mutate account balances.
 * It validates rail-specific rules and parameters, then delegates all money
 * movement to the PaymentService → TransactionEngine.
 *
 * Rail-specific behaviors (e.g., NEFT batch windows, RTGS minimum amounts)
 * are enforced here at the adapter level, not inside the core TransactionEngine.
 */
public interface PaymentRail {

    Payment.PaymentRailType getRailType();

    /**
     * Validate rail-specific constraints before initiating the payment.
     * Throws RuntimeException with a descriptive message if validation fails.
     */
    void validate(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency);

    /**
     * Returns the maximum allowed transfer per transaction on this rail.
     */
    BigDecimal maxTransactionLimit();

    /**
     * Returns the minimum allowed transfer per transaction on this rail.
     */
    BigDecimal minTransactionLimit();

    /**
     * Human-readable description of this rail for audit/display.
     */
    String description();
}
