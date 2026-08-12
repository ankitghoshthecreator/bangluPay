package engine.nexus.kyc;

/**
 * KYCVerificationResult — returned by all offline KYC simulators.
 *
 * IMPORTANT DISCLAIMER:
 * verificationMethod = OFFLINE_SIMULATION means this was verified by a local
 * algorithm only, NOT by UIDAI, Income Tax Department, or any real authority.
 * A passing result does NOT prove Aadhaar ownership or PAN authenticity.
 */
public record KYCVerificationResult(
        boolean passed,
        String maskedIdentifier,
        String identifierHash,
        String reason,
        VerificationMethod method
) {
    public enum VerificationMethod {
        OFFLINE_SIMULATION
    }
}
