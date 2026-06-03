package org.apereo.cas.unified.gateway.mfa;

import module java.base;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.services.RegisteredService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SPI interface that provides a unified abstraction for multi-factor authentication
 * providers (such as WebAuthn, Duo, etc.) within the unified gateway context.
 * <p>
 * Implementations of this interface can be registered as Spring beans and will be
 * auto-discovered by the unified gateway controller to handle MFA challenges.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public interface GatewayMfaProvider {

    /**
     * Returns the unique identifier for this MFA provider.
     *
     * @return provider identifier (e.g., "webauthn", "duo")
     */
    String getProviderId();

    /**
     * Determines whether this provider supports the given authentication context.
     *
     * @param authentication the current authentication
     * @param service the target registered service
     * @return true if this provider can handle MFA for the given context
     */
    boolean supports(Authentication authentication, RegisteredService service);

    /**
     * Initiates the MFA challenge for the given principal.
     *
     * @param principal the authenticated principal
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @return true if the MFA challenge was successfully initiated
     */
    boolean initiateChallenge(Principal principal, HttpServletRequest request, HttpServletResponse response);

    /**
     * Validates the MFA response for the given principal.
     *
     * @param principal the authenticated principal
     * @param request the HTTP servlet request
     * @return true if the MFA validation succeeded
     */
    boolean validateResponse(Principal principal, HttpServletRequest request);

    /**
     * Returns the order of this provider in the chain of MFA providers.
     * Lower values indicate higher priority.
     *
     * @return order value
     */
    default int getOrder() {
        return 0;
    }
}
