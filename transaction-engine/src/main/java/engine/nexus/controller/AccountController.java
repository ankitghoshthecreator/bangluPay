package engine.nexus.controller;

import lombok.extern.slf4j.Slf4j;

import engine.nexus.model.Account;
import engine.nexus.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
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

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable("id") UUID id, @RequestParam("amount") BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(id, amount, "Direct REST Deposit"));
    }
}
