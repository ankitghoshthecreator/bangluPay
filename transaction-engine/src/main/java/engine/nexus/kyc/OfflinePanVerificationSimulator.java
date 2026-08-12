package engine.nexus.kyc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * OfflinePanVerificationSimulator
 *
 * DISCLAIMER: This is a LOCAL SIMULATION ONLY.
 * It does NOT connect to the Income Tax Department or any real PAN verification service.
 * Format validity ≠ PAN authenticity.
 * VerificationMethod = OFFLINE_SIMULATION.
 *
 * PAN format (AAAAA9999A):
 *   - 5 uppercase letters
 *   - 4 digits
 *   - 1 uppercase letter
 * 4th character encodes taxpayer type:
 *   P = Individual, C = Company, H = HUF, F = Firm, B = Body of Individuals, etc.
 *
 * Masking: first 6 chars masked as XXXXXX, last 4 shown
 */
@Service
@Slf4j
public class OfflinePanVerificationSimulator {

    private static final BCryptPasswordEncoder HASHER = new BCryptPasswordEncoder(10);
    private static final java.util.regex.Pattern PAN_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private static final java.util.Set<Character> VALID_4TH_CHARS =
            java.util.Set.of('P','C','H','F','A','T','B','L','J','G');

    public KYCVerificationResult verify(String rawPan) {
        log.info("[OFFLINE_SIMULATION] Verifying PAN (masked: {})", mask(rawPan));

        if (rawPan == null || rawPan.isBlank()) {
            return fail("PAN cannot be empty");
        }

        String pan = rawPan.toUpperCase().trim();

        if (!PAN_PATTERN.matcher(pan).matches()) {
            return fail("PAN format invalid — expected pattern: AAAAA9999A (e.g. ABCDE1234F)");
        }

        char taxpayerType = pan.charAt(3);
        if (!VALID_4TH_CHARS.contains(taxpayerType)) {
            return fail("PAN 4th character '" + taxpayerType + "' is not a recognized taxpayer type (OFFLINE_SIMULATION)");
        }

        String masked = mask(pan);
        String hashed = HASHER.encode(pan);

        log.info("[OFFLINE_SIMULATION] PAN format PASSED for masked: {}", masked);
        return new KYCVerificationResult(true, masked, hashed,
                "PAN format valid, taxpayer type=" + taxpayerType + " (OFFLINE_SIMULATION — not Income Tax Dept verified)",
                KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
    }

    private KYCVerificationResult fail(String reason) {
        return new KYCVerificationResult(false, null, null, reason,
                KYCVerificationResult.VerificationMethod.OFFLINE_SIMULATION);
    }

    private String mask(String pan) {
        if (pan == null || pan.length() != 10) return "XXXXXXXXXX";
        return "XXXXXX" + pan.substring(6);
    }
}
