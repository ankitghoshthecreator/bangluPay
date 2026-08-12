package engine.nexus.controller;

import engine.nexus.dto.TransactionRequest;
import engine.nexus.model.Transaction;
import engine.nexus.service.TransactionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionEngine transactionEngine;

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@RequestBody TransactionRequest request) {
        Transaction tx = transactionEngine.processTransfer(
                request.getExternalId(),
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getDescription()
        );
        return ResponseEntity.ok(tx);
    }
}
