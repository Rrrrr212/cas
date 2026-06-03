package org.apereo.cas.gateway;

import org.apereo.cas.authentication.Authentication;
import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI for unified multi-factor authentication in the gateway.
 * Provides an abstraction layer for providers like WebAuthn and Duo
 * without modifying the existing MFA modules.
 *
 * @since 7.0.0
 */
public interface GatewayMfaProvider {
    
    /**
     * Check if this provider supports the given request.
     *
     * @param request the http request
     * @return true if supported
     */
    boolean supports(HttpServletRequest request);
    
    /**
     * Get the provider identifier.
     *
     * @return provider id
     */
    String getId();
    
    /**
     * Verify the multi-factor authentication.
     *
     * @param authentication current authentication
     * @param request the http request
     * @return true if verification succeeds
     */
    boolean verify(Authentication authentication, HttpServletRequest request);
}
