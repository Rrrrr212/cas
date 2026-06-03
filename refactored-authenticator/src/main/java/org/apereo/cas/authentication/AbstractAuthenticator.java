package org.apereo.cas.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;

import java.util.Map;

/**
 * Abstract base class for authenticators providing common functionality
 * for token validation and attribute mapping.
 *
 * @param <T> the credential type
 * @param <P> the principal type
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthenticator<T, P extends Principal> implements Authenticator<T, P> {

    protected final PrincipalFactory principalFactory;

    protected final AuthenticationProperties authenticationProperties;

    @Override
    public P authenticate(T credential) throws AuthenticationException {
        LOGGER.debug("Starting authentication process for credential: [{}]", credential);

        validateCredential(credential);
        
        Map<String, Object> attributes = extractAttributes(credential);
        String principalId = extractPrincipalId(credential, attributes);

        P principal = principalFactory.createPrincipal(principalId, attributes);
        
        LOGGER.debug("Authentication successful for principal: [{}]", principalId);
        return principal;
    }

    protected abstract void validateCredential(T credential) throws AuthenticationException;

    protected abstract String extractPrincipalId(T credential, Map<String, Object> attributes);

    protected abstract Map<String, Object> extractAttributes(T credential);

    protected Map<String, Object> mapAttributes(Map<String, Object> rawAttributes) {
        return authenticationProperties.getAttributeMapping().entrySet().stream()
            .filter(entry -> rawAttributes.containsKey(entry.getKey()))
            .collect(Map::of);
    }
}
