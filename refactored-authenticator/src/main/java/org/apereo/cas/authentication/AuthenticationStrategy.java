package org.apereo.cas.authentication;

/**
 * Strategy interface for selecting the appropriate authenticator.
 * Part of the Strategy pattern for flexible authentication protocol selection.
 */
public interface AuthenticationStrategy {

    Authenticator<?, ?> selectAuthenticator(Credential credential);

    String getStrategyName();

    boolean supports(Credential credential);
}
