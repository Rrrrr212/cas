package org.apereo.cas.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Factory for managing and providing authenticators.
 * Uses Strategy pattern to select appropriate authenticator based on credential type.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticatorFactory {

    private final List<Authenticator<?, ?>> authenticators;

    private final List<AuthenticationStrategy> strategies;

    public Optional<Authenticator<?, ?>> getAuthenticator(Credential credential) {
        LOGGER.debug("Looking for authenticator for credential type: [{}]", 
            credential.getClass().getName());

        // First try using authentication strategies
        for (AuthenticationStrategy strategy : strategies) {
            if (strategy.supports(credential)) {
                LOGGER.debug("Using strategy: [{}] to select authenticator", 
                    strategy.getStrategyName());
                Authenticator<?, ?> authenticator = strategy.selectAuthenticator(credential);
                if (authenticator != null) {
                    return Optional.of(authenticator);
                }
            }
        }

        // Fall back to direct matching
        return authenticators.stream()
            .filter(auth -> auth.supports(credential.getClass()))
            .findFirst();
    }

    public Map<String, Authenticator<?, ?>> getAllAuthenticators() {
        return authenticators.stream()
            .collect(Collectors.toMap(Authenticator::getName, auth -> auth));
    }

    public <T extends Authenticator<?, ?>> Optional<T> getAuthenticatorByName(String name, 
                                                                               Class<T> type) {
        return authenticators.stream()
            .filter(auth -> auth.getName().equals(name))
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst();
    }
}
