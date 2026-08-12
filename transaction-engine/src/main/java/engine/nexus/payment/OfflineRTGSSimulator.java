package engine.nexus.payment;

import engine.nexus.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OfflineRTGSSimulator — simulates RTGS payment rail.
 * DISCLAIMER: LOCAL SIMULATION. Not connected to RBI RTGS.
 * RTGS: Real Time Gross Settlement. Minimum ₹2,00,000. No upper limit.
 * Used for high-value, time-critical inter-bank transfers.
 */
@Component
@Slf4j
public class OfflineRTGSSimulator implements PaymentRail {

    private static final BigDecimal MIN = new BigDecimal("200000.00");
    private static final BigDecimal MAX = new BigDecimal("999999999.00");

    @Override
    public Payment.PaymentRailType getRailType() { return Payment.PaymentRailType.RTGS; }

    @Override
    public void validate(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        log.debug("[OFFLINE_RTGS_SIM] Validating RTGS: from={} to={} amount={}", fromAccountId, toAccountId, amount);
        if (amount.compareTo(MIN) < 0) {
            throw new RuntimeException("[OfflineRTGSSimulator] RTGS minimum transfer amount is ₹2,00,000");
        }
    }

    @Override public BigDecimal maxTransactionLimit() { return MAX; }
    @Override public BigDecimal minTransactionLimit() { return MIN; }
    @Override public String description() { return "Offline RTGS Simulator (LOCAL — not connected to RBI)"; }
}
