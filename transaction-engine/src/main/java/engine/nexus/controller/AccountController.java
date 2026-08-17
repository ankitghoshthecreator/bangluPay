package engine.nexus.controller;

import lombok.extern.slf4j.Slf4j;
import engine.nexus.model.Account;
import engine.nexus.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestParam("holderName") String holderName, @RequestParam("currency") String currency) {
        log.info("REST request to create account for holder: {}", holderName);
        return ResponseEntity.ok(accountService.createAccount(holderName, currency));
    }
    
    @PostMapping("/full")
    public ResponseEntity<Account> createFullAccount(@RequestBody Map<String, String> body) {
        UUID customerId = UUID.fromString(body.get("customerId"));
        String holderName = body.get("holderName");
        String currency = body.getOrDefault("currency", "INR");
        Account.AccountType type = Account.AccountType.valueOf(body.get("type").toUpperCase());
        UUID bankId = UUID.fromString(body.get("bankId"));
        UUID branchId = UUID.fromString(body.get("branchId"));
        
        log.info("REST request to create {} account for customer: {}", type, customerId);
        return ResponseEntity.ok(accountService.createAccount(customerId, holderName, currency, type, bankId, branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Account>> getCustomerAccounts(@PathVariable("customerId") UUID customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable("id") UUID id, @RequestParam("amount") BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(id, amount, "Direct REST Deposit"));
    }
    
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable("id") UUID id, @RequestParam("amount") BigDecimal amount) {
        return ResponseEntity.ok(accountService.withdraw(id, amount, "Direct REST Withdrawal"));
    }
    
    @PostMapping("/{id}/freeze")
    public ResponseEntity<Account> freeze(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(accountService.freezeAccount(id, body.getOrDefault("reason", "Admin request")));
    }
    
    @PostMapping("/{id}/unfreeze")
    public ResponseEntity<Account> unfreeze(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(accountService.unfreezeAccount(id));
    }
    
    @PostMapping("/{id}/request-closure")
    public ResponseEntity<Account> requestClosure(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(accountService.requestClosure(id, body.getOrDefault("reason", "Customer request")));
    }
    
    @PostMapping("/{id}/close")
    public ResponseEntity<Account> close(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(accountService.closeAccount(id));
    }
}
