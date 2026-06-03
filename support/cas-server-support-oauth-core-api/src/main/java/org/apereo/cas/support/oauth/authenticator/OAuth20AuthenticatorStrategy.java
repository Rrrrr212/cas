package org.apereo.cas.support.oauth.authenticator;

import module java.base;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.authenticator.Authenticator;

/**
 * Strategy interface for selecting the appropriate OAuth20 authenticator
 * based on the incoming request context (grant type, authentication method, etc.).
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
public interface OAuth20AuthenticatorStrategy {

    /**
     * Determine whether this strategy can handle the given request context.
     *
     * @param callContext the current HTTP call context
     * @return true if this strategy is applicable
     */
    boolean supports(CallContext callContext);

    /**
     * Return the authenticator that should be used for this strategy.
     *
     * @return the authenticator instance
     */
    Authenticator getAuthenticator();

    /**
     * Return the authentication method name this strategy represents.
     *
     * @return the method name (e.g., "access_token", "client_credentials", "refresh_token")
     */
    String getMethodName();
}
