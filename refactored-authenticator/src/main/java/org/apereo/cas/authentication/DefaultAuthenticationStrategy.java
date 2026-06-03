package org.apereo.cas.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default authentication strategy implementation that selects authenticator
 * based on credential type.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAuthenticationStrategy implements AuthenticationStrategy {

    private final List<Authenticator<?, ?>> authenticators;

    @Override
    public Authenticator<?, ?> selectAuthenticator(Credential credential) {
        LOGGER.debug("Selecting authenticator for credential type: [{}]", 
            credential.getClass().getName());
        
        return authenticators.stream()
            .filter(auth -> auth.supports(credential.getClass()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public String getStrategyName() {
        return "DefaultStrategy";
    }

    @Override
    public boolean supports(Credential credential) {
        return true; // Supports all credential types
    }
}
