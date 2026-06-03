package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.Principal;

/**
 * Main authenticator interface defining core authentication operations.
 *
 * @param <T> the credential type
 * @param <P> the principal type
 */
public interface Authenticator<T, P extends Principal> {

    P authenticate(T credential) throws AuthenticationException;

    boolean supports(Class<?> credentialType);

    String getName();
}
