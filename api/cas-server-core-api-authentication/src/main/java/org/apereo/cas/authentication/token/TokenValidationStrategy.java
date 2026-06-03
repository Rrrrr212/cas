package org.apereo.cas.authentication.token;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;

import java.util.Map;
import java.util.Set;

/**
 * Strategy interface for token validation and principal extraction.
 *
 * @param <T> the specific token type
 */
public interface TokenValidationStrategy<T> {

    /**
     * Determines if the strategy supports the given credential.
     *
     * @param credential the credential
     * @return true if supported
     */
    boolean supports(Credential credential);

    /**
     * Validates the credential and extracts the underlying token object.
     *
     * @param credential the credential
     * @return the token object, or null if invalid
     * @throws Throwable the throwable
     */
    T validate(Credential credential) throws Throwable;

    /**
     * Checks if the token is expired.
     *
     * @param token the token
     * @return true if expired
     */
    boolean isExpired(T token);

    /**
     * Checks if the token has the required scopes.
     *
     * @param token          the token
     * @param requiredScopes the required scopes
     * @return true if it has required scopes
     */
    boolean hasRequiredScopes(T token, Set<String> requiredScopes);

    /**
     * Extracts the primary principal from the validated token.
     *
     * @param token the token
     * @return the principal
     */
    Principal extractPrincipal(T token);

    /**
     * Extracts additional attributes directly from the token itself.
     *
     * @param token the token
     * @return a map of additional attributes
     */
    Map<String, Object> extractAttributes(T token);
}
