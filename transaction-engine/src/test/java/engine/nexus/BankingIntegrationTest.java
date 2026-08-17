package engine.nexus;

import engine.nexus.config.DataInitializer;
import engine.nexus.kyc.KYCVerificationResult;
import engine.nexus.kyc.OfflineAadhaarVerificationSimulator;
import engine.nexus.kyc.OfflinePanVerificationSimulator;
import engine.nexus.model.*;
import engine.nexus.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class BankingIntegrationTest {

    @Autowired private CustomerService customerService;
    @Autowired private KYCService kycService;
    @Autowired private AccountService accountService;
    @Autowired private UPIService upiService;
    @Autowired private PaymentService paymentService;
    @Autowired private LedgerService ledgerService;

    @MockBean private OfflineAadhaarVerificationSimulator aadhaarSimulator;
    @MockBean private OfflinePanVerificationSimulator panSimulator;

    @BeforeEach
    void setUp() {
        when(aadhaarSimulator.verify(anyString())).thenReturn(
                new KYCVerificationResult(true, "XXXXXXXX1234", "hash", "Valid", KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION)
        );
        when(panSimulator.verify(anyString())).thenReturn(
                new KYCVerificationResult(true, "XXXXXX1234", "hash", "Valid", KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION)
        );
    }

    @Test
    @DisplayName("End-to-End Banking Workflow")
    void testEndToEndBankingWorkflow() {
        // 1. Register customer A → assert PENDING_KYC
        String phoneA = "9876543" + System.currentTimeMillis() % 1000;
        Customer customerA = customerService.register("Alice E2E", phoneA + "@example.com", phoneA, "123 Street", "Password@123");
        assertEquals(Customer.CustomerStatus.PENDING_KYC, customerA.getStatus());
        
        kycService.initiateKYC(customerA.getCustomerId());

        // 2 & 3. Verify Aadhaar & PAN → assert customer ACTIVE
        kycService.verifyAadhaar(customerA.getCustomerId(), "123412341234");
        KYCRecord kycA = kycService.verifyPAN(customerA.getCustomerId(), "ABCDE1234F");
        assertEquals(KYCRecord.KYCStatus.VERIFIED, kycA.getStatus());
        
        customerA = customerService.getCustomer(customerA.getCustomerId());
        assertEquals(Customer.CustomerStatus.ACTIVE, customerA.getStatus());

        // 4. Open savings account via AccountService
        Account accountA = accountService.createAccount(
                customerA.getCustomerId(), "Alice E2E", "INR", Account.AccountType.SAVINGS, DataInitializer.BANK_BNGL_ID, DataInitializer.BRANCH_BNGL_MAIN_ID
        );

        // 5. Deposit ₹10,000 → verify ledger entry + balance
        accountService.deposit(accountA.getAccountId(), new BigDecimal("10000.00"), "Initial Deposit");
        accountA = accountService.getAccount(accountA.getAccountId());
        assertEquals(0, new BigDecimal("10000.00").compareTo(accountA.getBalance()));

        // 6. Register UPI VPA
        String vpaA = "alice" + System.currentTimeMillis() + "@bngl";
        upiService.registerVPA(customerA.getCustomerId(), accountA.getAccountId(), vpaA, "1234");

        // 7. Register a second customer + account + deposit
        String phoneB = "8876543" + System.currentTimeMillis() % 1000;
        Customer customerB = customerService.register("Bob E2E", phoneB + "@example.com", phoneB, "456 Avenue", "Password@123");
        kycService.initiateKYC(customerB.getCustomerId());
        kycService.verifyAadhaar(customerB.getCustomerId(), "123412341234");
        kycService.verifyPAN(customerB.getCustomerId(), "ABCDE1234F");
        Account accountB = accountService.createAccount(
                customerB.getCustomerId(), "Bob E2E", "INR", Account.AccountType.SAVINGS, DataInitializer.BANK_BNGL_ID, DataInitializer.BRANCH_BNGL_MAIN_ID
        );
        String vpaB = "bob" + System.currentTimeMillis() + "@bngl";
        upiService.registerVPA(customerB.getCustomerId(), accountB.getAccountId(), vpaB, "5678");

        // 8. Send ₹2,000 via UPI → verify both balances + ledger
        String upiKey = "E2E-UPI-" + System.currentTimeMillis();
        Payment payment = paymentService.initiateUPIPayment(
                vpaA, vpaB, new BigDecimal("2000.00"), "1234", upiKey, "Test UPI"
        );
        assertEquals(Payment.PaymentStatus.SUCCESS, payment.getStatus());

        accountA = accountService.getAccount(accountA.getAccountId());
        accountB = accountService.getAccount(accountB.getAccountId());
        assertEquals(0, new BigDecimal("8000.00").compareTo(accountA.getBalance()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(accountB.getBalance()));

        // 9. Check payment history
        List<Payment> history = paymentService.getPaymentHistory(accountA.getAccountId());
        assertFalse(history.isEmpty());

        // 10. Check statement
        List<LedgerEntry> statement = ledgerService.getStatement(accountA.getAccountId());
        assertFalse(statement.isEmpty());

        // 11. Verify ledger double-entry integrity
        Map<String, Object> integrity = ledgerService.verifyDoubleEntryIntegrity();
        assertTrue((Boolean) integrity.get("balanced"));

        // 12. Attempt duplicate payment → verify idempotency
        Payment duplicate = paymentService.initiateUPIPayment(
                vpaA, vpaB, new BigDecimal("2000.00"), "1234", upiKey, "Duplicate"
        );
        assertEquals(payment.getPaymentId(), duplicate.getPaymentId()); // Returns the same payment object
        
        // Ensure balance didn't change
        accountA = accountService.getAccount(accountA.getAccountId());
        assertEquals(0, new BigDecimal("8000.00").compareTo(accountA.getBalance()));

        // 13. Attempt payment from frozen account → verify rejection
        accountService.freezeAccount(accountA.getAccountId(), "Security check");
        String frozenKey = "E2E-FROZEN-" + System.currentTimeMillis();
        Payment failedPayment = paymentService.initiateUPIPayment(
                vpaA, vpaB, new BigDecimal("100.00"), "1234", frozenKey, "Frozen UPI"
        );
        assertEquals(Payment.PaymentStatus.FAILED, failedPayment.getStatus());

        // 14. Request account closure with non-zero balance → verify rejection
        accountService.unfreezeAccount(accountA.getAccountId());
        final UUID accAId = accountA.getAccountId();
        assertThrows(RuntimeException.class, () -> accountService.requestClosure(accAId, "Closing"));

        // 15. Withdraw remaining, then close → verify CLOSED
        accountService.withdraw(accountA.getAccountId(), new BigDecimal("8000.00"), "Withdraw all");
        accountService.requestClosure(accountA.getAccountId(), "Closing");
        accountA = accountService.closeAccount(accountA.getAccountId());
        assertEquals(Account.AccountStatus.CLOSED, accountA.getStatus());
    }
}
