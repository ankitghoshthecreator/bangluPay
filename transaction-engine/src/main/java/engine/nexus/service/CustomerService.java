package engine.nexus.service;

import engine.nexus.model.Customer;
import engine.nexus.model.KYCRecord;
import engine.nexus.model.UserAuth;
import engine.nexus.repository.CustomerRepository;
import engine.nexus.repository.KYCRepository;
import engine.nexus.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KYCRepository kycRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public Customer register(String fullName, String email, String phone, String address, String rawPassword) {
        if (customerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }
        if (customerRepository.existsByPhone(phone)) {
            throw new RuntimeException("Phone already registered: " + phone);
        }

        Customer customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .fullName(fullName)
                .email(email.toLowerCase().trim())
                .phone(phone.trim())
                .address(address)
                .status(Customer.CustomerStatus.PENDING_KYC)
                .build();
        customer = customerRepository.save(customer);

        // Create authentication record — password hashed with BCrypt, never stored plaintext
        UserAuth auth = UserAuth.builder()
                .authId(UUID.randomUUID())
                .customerId(customer.getCustomerId())
                .username(phone.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserAuth.UserRole.CUSTOMER)
                .locked(false)
                .failedLoginAttempts(0)
                .build();
        userAuthRepository.save(auth);

        // Initiate KYC record in PENDING state
        kycRepository.save(KYCRecord.builder()
                .kycId(UUID.randomUUID())
                .customerId(customer.getCustomerId())
                .status(KYCRecord.KYCStatus.PENDING)
                .build());

        auditService.log("CUSTOMER_REGISTERED", customer.getCustomerId(), "CUSTOMER",
                customer.getCustomerId(), "Customer", "email=" + email);
        log.info("Registered customer: {} ({})", fullName, customer.getCustomerId());
        return customer;
    }

    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found for email: " + email));
    }

    /** Login verification — checks password hash and account lock status. */
    public UserAuth authenticate(String username, String rawPassword) {
        UserAuth auth = userAuthRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (auth.isLocked()) {
            if (auth.getLockedUntil() != null && java.time.LocalDateTime.now().isBefore(auth.getLockedUntil())) {
                throw new RuntimeException("Account locked until: " + auth.getLockedUntil());
            } else {
                auth.setLocked(false);
                auth.setFailedLoginAttempts(0);
                userAuthRepository.save(auth);
            }
        }

        if (!passwordEncoder.matches(rawPassword, auth.getPasswordHash())) {
            auth.setFailedLoginAttempts(auth.getFailedLoginAttempts() + 1);
            if (auth.getFailedLoginAttempts() >= 5) {
                auth.setLocked(true);
                auth.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(30));
                auditService.log("ACCOUNT_LOCKED", auth.getCustomerId(), "SYSTEM", auth.getCustomerId(), "UserAuth", "Too many failed attempts");
            }
            userAuthRepository.save(auth);
            throw new RuntimeException("Invalid credentials");
        }

        auth.setFailedLoginAttempts(0);
        auth.setLastLogin(java.time.LocalDateTime.now());
        return userAuthRepository.save(auth);
    }

    @Transactional
    public Customer updateProfile(UUID customerId, String fullName, String address) {
        Customer c = getCustomer(customerId);
        c.setFullName(fullName);
        c.setAddress(address);
        auditService.log("CUSTOMER_PROFILE_UPDATED", customerId, "CUSTOMER", customerId, "Customer", "");
        return customerRepository.save(c);
    }
}
