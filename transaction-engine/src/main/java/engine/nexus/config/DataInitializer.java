package engine.nexus.config;

import engine.nexus.model.*;
import engine.nexus.repository.*;
import engine.nexus.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DataInitializer — seeds the required banks, branches, and system accounts on first startup.
 *
 * System Accounts (mandatory for double-entry balance on deposits/withdrawals/fees):
 *   - SYSTEM_CASH        → represents physical cash entering/leaving the system
 *   - SYSTEM_SETTLEMENT  → settlement account for inter-bank clearing
 *   - SYSTEM_FEES        → fee collection account
 *   - SYSTEM_REVERSAL    → source/target for reversal transactions
 *
 * Simulated Banks (fictional — no connection to real institutions):
 *   - BNGL → BangluPay Bank
 *   - SBIN → State Bank Sim
 *   - HDFC → HDFC Sim
 *   - ICIC → ICICI Sim
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    // Fixed UUIDs for system accounts so they survive restarts
    public static final UUID SYSTEM_CASH_ID       = UUID.fromString("00000001-0000-0000-0000-000000000001");
    public static final UUID SYSTEM_SETTLEMENT_ID = UUID.fromString("00000001-0000-0000-0000-000000000002");
    public static final UUID SYSTEM_FEES_ID       = UUID.fromString("00000001-0000-0000-0000-000000000003");
    public static final UUID SYSTEM_REVERSAL_ID   = UUID.fromString("00000001-0000-0000-0000-000000000004");

    public static final UUID BANK_BNGL_ID = UUID.fromString("00000002-0000-0000-0000-000000000001");
    public static final UUID BANK_SBIN_ID = UUID.fromString("00000002-0000-0000-0000-000000000002");
    public static final UUID BANK_HDFC_ID = UUID.fromString("00000002-0000-0000-0000-000000000003");
    public static final UUID BANK_ICIC_ID = UUID.fromString("00000002-0000-0000-0000-000000000004");

    public static final UUID BRANCH_BNGL_MAIN_ID = UUID.fromString("00000003-0000-0000-0000-000000000001");
    public static final UUID BRANCH_SBIN_MAIN_ID = UUID.fromString("00000003-0000-0000-0000-000000000002");
    public static final UUID BRANCH_HDFC_MAIN_ID = UUID.fromString("00000003-0000-0000-0000-000000000003");
    public static final UUID BRANCH_ICIC_MAIN_ID = UUID.fromString("00000003-0000-0000-0000-000000000004");

    private final AccountRepository accountRepository;
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedBanks();
        seedBranches();
        seedSystemAccounts();
        log.info("DataInitializer complete — banks, branches, and system accounts are ready.");
    }

    private void seedBanks() {
        seedBank(BANK_BNGL_ID, "BNGL", "BangluPay Bank (Sim)", "BNGL");
        seedBank(BANK_SBIN_ID, "SBIN", "State Bank Sim", "SBIN");
        seedBank(BANK_HDFC_ID, "HDFC", "HDFC Sim", "HDFC");
        seedBank(BANK_ICIC_ID, "ICIC", "ICICI Sim", "ICIC");
    }

    private void seedBank(UUID id, String code, String name, String ifscPrefix) {
        if (!bankRepository.existsById(id)) {
            bankRepository.save(Bank.builder()
                    .bankId(id)
                    .bankCode(code)
                    .bankName(name)
                    .ifscPrefix(ifscPrefix)
                    .build());
            log.info("Seeded bank: {} ({})", name, code);
        }
    }

    private void seedBranches() {
        seedBranch(BRANCH_BNGL_MAIN_ID, BANK_BNGL_ID, "BangluPay HQ Branch", "BNGL0001001", "Mumbai");
        seedBranch(BRANCH_SBIN_MAIN_ID, BANK_SBIN_ID, "SBI Sim Main Branch",  "SBIN0001001", "Delhi");
        seedBranch(BRANCH_HDFC_MAIN_ID, BANK_HDFC_ID, "HDFC Sim Main Branch", "HDFC0001001", "Bangalore");
        seedBranch(BRANCH_ICIC_MAIN_ID, BANK_ICIC_ID, "ICICI Sim Main Branch","ICIC0001001", "Chennai");
    }

    private void seedBranch(UUID id, UUID bankId, String name, String ifsc, String city) {
        if (!branchRepository.existsById(id)) {
            branchRepository.save(Branch.builder()
                    .branchId(id)
                    .bankId(bankId)
                    .branchName(name)
                    .ifscCode(ifsc)
                    .city(city)
                    .build());
            log.info("Seeded branch: {} ({})", name, ifsc);
        }
    }

    private void seedSystemAccounts() {
        // SYSTEM_CASH: represents physical cash pool. Credited when customers deposit cash,
        //              debited when customers withdraw cash.
        seedSystemAccount(SYSTEM_CASH_ID, "SYSTEM_CASH", "SYSTEM-CASH-0001");

        // SYSTEM_SETTLEMENT: inter-bank clearing pool. Used as intermediary for cross-bank transfers.
        seedSystemAccount(SYSTEM_SETTLEMENT_ID, "SYSTEM_SETTLEMENT", "SYSTEM-SETL-0001");

        // SYSTEM_FEES: fee collection. Credited when service fees are deducted from customer accounts.
        seedSystemAccount(SYSTEM_FEES_ID, "SYSTEM_FEES", "SYSTEM-FEES-0001");

        // SYSTEM_REVERSAL: source for reversal transactions that return funds to customers.
        seedSystemAccount(SYSTEM_REVERSAL_ID, "SYSTEM_REVERSAL", "SYSTEM-REVS-0001");
    }

    private void seedSystemAccount(UUID id, String name, String accountNumber) {
        if (!accountRepository.existsById(id)) {
            accountRepository.save(Account.builder()
                    .accountId(id)
                    .accountNumber(accountNumber)
                    .holderName(name)
                    .balance(BigDecimal.ZERO.setScale(2))
                    .currency("INR")
                    .status(Account.AccountStatus.ACTIVE)
                    .accountType(Account.AccountType.CURRENT)
                    .bankId(BANK_BNGL_ID)
                    .build());
            log.info("Seeded system account: {} ({})", name, accountNumber);
        }
    }
}
