package org.apereo.cas.support.oauth.authenticator;

import module java.base;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link OAuth20AuthenticatorStrategy} that
 * iterates through registered strategies and delegates to the first
 * one that supports the current request context.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOAuth20AuthenticatorStrategy implements OAuth20AuthenticatorStrategy {

    private final List<OAuth20AuthenticatorStrategy> strategies;

    @Override
    public boolean supports(final CallContext callContext) {
        return resolveAuthenticator(callContext).isPresent();
    }

    @Override
    public Authenticator getAuthenticator() {
        throw new UnsupportedOperationException("Use resolveAuthenticator(callContext) instead");
    }

    @Override
    public String getMethodName() {
        return "default";
    }

    /**
     * Resolve the appropriate authenticator for the given call context.
     *
     * @param callContext the current HTTP call context
     * @return the matching authenticator, or empty if none matches
     */
    public Optional<Authenticator> resolveAuthenticator(final CallContext callContext) {
        for (val strategy : strategies) {
            if (strategy.supports(callContext)) {
                LOGGER.debug("Resolved authenticator strategy [{}] for request", strategy.getMethodName());
                return Optional.of(strategy.getAuthenticator());
            }
        }
        LOGGER.debug("No authenticator strategy matched the request context");
        return Optional.empty();
    }
}
