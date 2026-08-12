package engine.nexus.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionRequest {
    private String externalId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String description;
}
