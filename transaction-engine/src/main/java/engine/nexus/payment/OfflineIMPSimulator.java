package engine.nexus.payment;

import engine.nexus.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OfflineIMPSimulator — simulates IMPS payment rail.
 * DISCLAIMER: LOCAL SIMULATION. Not connected to NPCI IMPS.
 * IMPS: Immediate Payment Service, 24x7, limit ₹1 to ₹5,00,000.
 */
@Component
@Slf4j
public class OfflineIMPSimulator implements PaymentRail {

    private static final BigDecimal MIN = new BigDecimal("1.00");
    private static final BigDecimal MAX = new BigDecimal("500000.00");

    @Override
    public Payment.PaymentRailType getRailType() { return Payment.PaymentRailType.IMPS; }

    @Override
    public void validate(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        log.debug("[OFFLINE_IMPS_SIM] Validating IMPS: from={} to={} amount={}", fromAccountId, toAccountId, amount);
        if (amount.compareTo(MIN) < 0) throw new RuntimeException("[OfflineIMPSimulator] IMPS minimum is ₹1.00");
        if (amount.compareTo(MAX) > 0) throw new RuntimeException("[OfflineIMPSimulator] IMPS maximum is ₹5,00,000");
    }

    @Override public BigDecimal maxTransactionLimit() { return MAX; }
    @Override public BigDecimal minTransactionLimit() { return MIN; }
    @Override public String description() { return "Offline IMPS Simulator (LOCAL — not connected to NPCI)"; }
}
