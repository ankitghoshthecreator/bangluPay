package engine.nexus.payment;

import engine.nexus.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OfflineNEFTSimulator — simulates NEFT payment rail.
 * DISCLAIMER: LOCAL SIMULATION. Not connected to RBI NEFT.
 * NEFT: National Electronic Funds Transfer. No upper limit; minimum ₹1.
 * In this simulation, settlement is instant (real NEFT has batch windows).
 */
@Component
@Slf4j
public class OfflineNEFTSimulator implements PaymentRail {

    private static final BigDecimal MIN = new BigDecimal("1.00");
    private static final BigDecimal MAX = new BigDecimal("999999999.00");

    @Override
    public Payment.PaymentRailType getRailType() { return Payment.PaymentRailType.NEFT; }

    @Override
    public void validate(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        log.debug("[OFFLINE_NEFT_SIM] Validating NEFT: from={} to={} amount={}", fromAccountId, toAccountId, amount);
        if (amount.compareTo(MIN) < 0) throw new RuntimeException("[OfflineNEFTSimulator] NEFT minimum is ₹1.00");
    }

    @Override public BigDecimal maxTransactionLimit() { return MAX; }
    @Override public BigDecimal minTransactionLimit() { return MIN; }
    @Override public String description() { return "Offline NEFT Simulator (LOCAL — not connected to RBI)"; }
}
