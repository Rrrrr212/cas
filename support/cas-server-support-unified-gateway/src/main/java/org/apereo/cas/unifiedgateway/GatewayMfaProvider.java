package org.apereo.cas.unifiedgateway;

import java.util.Map;

/**
 * Unified MFA provider SPI for supporting multiple MFA methods.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
public interface GatewayMfaProvider {

    String BEAN_NAME = "gatewayMfaProvider";

    /**
     * Gets provider name.
     *
     * @return the provider name
     */
    String getName();

    /**
     * Check if provider is available/enabled.
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Initiate MFA challenge.
     *
     * @param userId the user id
     * @param context the context
     * @return challenge response
     */
    MfaChallengeResponse initiateChallenge(String userId, Map<String, Object> context);

    /**
     * Verify MFA response.
     *
     * @param userId the user id
     * @param response the response
     * @return verification result
     */
    MfaVerificationResult verifyResponse(String userId, MfaResponse response);

    record MfaChallengeResponse(boolean success, String challengeId, String challengeData, String message) {}

    record MfaResponse(String challengeId, String userResponse, Map<String, Object> additionalData) {}

    record MfaVerificationResult(boolean success, String message, Map<String, Object> details) {}
}
