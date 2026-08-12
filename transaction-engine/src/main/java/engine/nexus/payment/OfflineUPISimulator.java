package engine.nexus.payment;

import engine.nexus.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OfflineUPISimulator — simulates UPI payment rail behavior.
 *
 * DISCLAIMER: This does NOT connect to NPCI, any real UPI switch, or bank.
 * This is a LOCAL simulation of UPI payment behavior.
 *
 * UPI characteristics (simulated):
 * - 24x7 availability
 * - Immediate settlement
 * - Limit: ₹1 to ₹1,00,000 per transaction
 */
@Component
@Slf4j
public class OfflineUPISimulator implements PaymentRail {

    private static final BigDecimal MIN = new BigDecimal("1.00");
    private static final BigDecimal MAX = new BigDecimal("100000.00");

    @Override
    public Payment.PaymentRailType getRailType() {
        return Payment.PaymentRailType.UPI;
    }

    @Override
    public void validate(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        log.debug("[OFFLINE_UPI_SIM] Validating UPI payment: from={} to={} amount={}", fromAccountId, toAccountId, amount);
        if (!"INR".equalsIgnoreCase(currency)) {
            throw new RuntimeException("[OfflineUPISimulator] UPI only supports INR");
        }
        if (amount.compareTo(MIN) < 0) {
            throw new RuntimeException("[OfflineUPISimulator] UPI minimum transaction is ₹1.00");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new RuntimeException("[OfflineUPISimulator] UPI maximum transaction is ₹1,00,000 per transaction");
        }
    }

    @Override
    public BigDecimal maxTransactionLimit() { return MAX; }

    @Override
    public BigDecimal minTransactionLimit() { return MIN; }

    @Override
    public String description() {
        return "Offline UPI Simulator (LOCAL — not connected to NPCI or any real UPI switch)";
    }
}
