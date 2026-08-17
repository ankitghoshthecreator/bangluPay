package engine.nexus.controller;

import engine.nexus.model.Account;
import engine.nexus.model.Customer;
import engine.nexus.model.UserAuth;
import engine.nexus.service.AccountService;
import engine.nexus.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CustomerController — REST API for customer registration, authentication, and profile management.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    /**
     * POST /api/customers/register
     * Register a new customer. Returns PENDING_KYC status until KYC is completed.
     */
    @PostMapping("/register")
    public ResponseEntity<Customer> register(@RequestBody Map<String, String> body) {
        log.info("REST: Register customer email={}", body.get("email"));
        Customer customer = customerService.register(
                body.get("fullName"),
                body.get("email"),
                body.get("phone"),
                body.get("address"),
                body.get("password")
        );
        return ResponseEntity.ok(customer);
    }

    /**
     * POST /api/customers/login
     * Authenticate a customer by phone (username) + password.
     * Returns the UserAuth record on success (password hash is excluded by JSON serialization).
     */
    @PostMapping("/login")
    public ResponseEntity<UserAuth> login(@RequestBody Map<String, String> body) {
        log.info("REST: Login attempt username={}", body.get("username"));
        UserAuth auth = customerService.authenticate(body.get("username"), body.get("password"));
        return ResponseEntity.ok(auth);
    }

    /**
     * GET /api/customers/{id}
     * Get customer profile by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    /**
     * GET /api/customers
     * List all customers (admin use).
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    /**
     * PUT /api/customers/{id}/profile
     * Update customer display name and address.
     */
    @PutMapping("/{id}/profile")
    public ResponseEntity<Customer> updateProfile(@PathVariable UUID id,
                                                   @RequestBody Map<String, String> body) {
        log.info("REST: Update profile customerId={}", id);
        Customer updated = customerService.updateProfile(id, body.get("fullName"), body.get("address"));
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/customers/{id}/accounts
     * Get all bank accounts belonging to a customer.
     */
    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<Account>> getAccounts(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountsByCustomer(id));
    }
}
