package engine.nexus.kyc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * OfflineAadhaarVerificationSimulator
 *
 * DISCLAIMER: This is a LOCAL SIMULATION ONLY.
 * It does NOT connect to UIDAI or any real Aadhaar verification service.
 * Checksum validity ≠ Aadhaar ownership.
 * VerificationMethod = OFFLINE_SIMULATION.
 *
 * Verification logic:
 *   1. Format check: 12 digits, not all same
 *   2. Verhoeff algorithm checksum validation (standard Aadhaar checksum)
 *   3. BCrypt hash of the number is stored — original is never persisted in plaintext
 *
 * Masking: first 8 digits are masked as XXXXXXXX, last 4 shown
 */
@Service
@Slf4j
public class OfflineAadhaarVerificationSimulator {

    private static final BCryptPasswordEncoder HASHER = new BCryptPasswordEncoder(10);

    // Verhoeff multiplication table
    private static final int[][] D = {
        {0,1,2,3,4,5,6,7,8,9},
        {1,2,3,4,0,6,7,8,9,5},
        {2,3,4,0,1,7,8,9,5,6},
        {3,4,0,1,2,8,9,5,6,7},
        {4,0,1,2,3,9,5,6,7,8},
        {5,9,8,7,6,0,4,3,2,1},
        {6,5,9,8,7,1,0,4,3,2},
        {7,6,5,9,8,2,1,0,4,3},
        {8,7,6,5,9,3,2,1,0,4},
        {9,8,7,6,5,4,3,2,1,0}
    };

    // Verhoeff permutation table
    private static final int[][] P = {
        {0,1,2,3,4,5,6,7,8,9},
        {1,5,7,6,2,8,3,0,9,4},
        {5,8,0,3,7,9,6,1,4,2},
        {8,9,1,6,0,4,3,5,2,7},
        {9,4,5,3,1,2,6,8,7,0},
        {4,2,8,6,5,7,3,9,0,1},
        {2,7,9,3,8,0,6,4,1,5},
        {7,0,4,6,9,1,3,2,5,8}
    };

    // Verhoeff inverse table
    private static final int[] INV = {0,4,3,2,1,9,8,7,6,5};

    public KYCVerificationResult verify(String rawAadhaar) {
        log.info("[OFFLINE_SIMULATION] Verifying Aadhaar (masked: {})", mask(rawAadhaar));

        if (rawAadhaar == null || !rawAadhaar.matches("\\d{12}")) {
            return new KYCVerificationResult(false, null, null,
                    "Aadhaar must be exactly 12 digits", KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
        }

        // Reject obviously invalid (all same digits like 111111111111)
        if (rawAadhaar.chars().distinct().count() == 1) {
            return new KYCVerificationResult(false, null, null,
                    "Invalid Aadhaar format", KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
        }

        // Must not start with 0 or 1 (UIDAI rule)
        if (rawAadhaar.charAt(0) == '0' || rawAadhaar.charAt(0) == '1') {
            return new KYCVerificationResult(false, null, null,
                    "Aadhaar cannot begin with 0 or 1", KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
        }

        if (!verhoeffCheck(rawAadhaar)) {
            return new KYCVerificationResult(false, null, null,
                    "Aadhaar checksum validation failed (OFFLINE_SIMULATION)",
                    KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
        }

        String masked = mask(rawAadhaar);
        String hashed = HASHER.encode(rawAadhaar);

        log.info("[OFFLINE_SIMULATION] Aadhaar checksum PASSED for masked: {}", masked);
        return new KYCVerificationResult(true, masked, hashed,
                "Aadhaar format and checksum valid (OFFLINE_SIMULATION — not UIDAI verified)",
                KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
    }

    private boolean verhoeffCheck(String number) {
        int c = 0;
        int[] digits = number.chars()
                .map(Character::getNumericValue)
                .toArray();
        // Reverse the digit array
        for (int i = 0; i < digits.length / 2; i++) {
            int tmp = digits[i];
            digits[i] = digits[digits.length - 1 - i];
            digits[digits.length - 1 - i] = tmp;
        }
        for (int i = 0; i < digits.length; i++) {
            c = D[c][P[i % 8][digits[i]]];
        }
        return c == 0;
    }

    private String mask(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) return "XXXXXXXXXXXX";
        return "XXXXXXXX" + aadhaar.substring(8);
    }
}
