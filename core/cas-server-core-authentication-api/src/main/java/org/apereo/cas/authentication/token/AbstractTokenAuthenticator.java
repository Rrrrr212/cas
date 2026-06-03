package org.apereo.cas.authentication.token;

import org.apereo.cas.authentication.AbstractAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.DefaultAuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.configuration.model.support.token.TokenAuthProperties;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.security.auth.login.FailedLoginException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base abstract authentication handler for token-based authentication.
 *
 * @param <T> the token type
 */
@Slf4j
public abstract class AbstractTokenAuthenticator<T> extends AbstractAuthenticationHandler {

    protected final TokenAuthProperties properties;
    protected final TokenValidationStrategy<T> validationStrategy;

    protected AbstractTokenAuthenticator(final String name,
                                         final PrincipalFactory principalFactory,
                                         final Integer order,
                                         final TokenAuthProperties properties,
                                         final TokenValidationStrategy<T> validationStrategy) {
        super(name, principalFactory, order);
        this.properties = properties;
        this.validationStrategy = validationStrategy;
    }

    @Override
    public boolean supports(final Credential credential) {
        return validationStrategy.supports(credential);
    }

    @Override
    public AuthenticationHandlerExecutionResult authenticate(final Credential credential, final Service service) throws Throwable {
        val tokenObject = validationStrategy.validate(credential);

        if (tokenObject == null || validationStrategy.isExpired(tokenObject)) {
            LOGGER.error("Token validation failed or token expired for credential [{}]", credential.getId());
            throw new FailedLoginException("Token validation failed or expired for credential: " + credential.getId());
        }

        val requiredScopes = properties.getRequiredScopes();
        if (requiredScopes != null && !requiredScopes.isEmpty() && !validationStrategy.hasRequiredScopes(tokenObject, requiredScopes)) {
            LOGGER.error("Unable to authenticate token without required scopes [{}]", requiredScopes);
            throw new FailedLoginException("Token is missing required scopes");
        }

        val principal = validationStrategy.extractPrincipal(tokenObject);
        if (principal == null) {
            throw new FailedLoginException("Unable to extract principal from token");
        }

        val finalAttributes = new HashMap<String, Object>(principal.getAttributes());
        val additionalAttributes = validationStrategy.extractAttributes(tokenObject);
        if (additionalAttributes != null) {
            finalAttributes.putAll(additionalAttributes);
        }

        mapAttributes(finalAttributes, credential, principal, tokenObject);

        val finalPrincipal = this.principalFactory.createPrincipal(principal.getId(), finalAttributes);
        LOGGER.trace("Final authenticated principal based on token is [{}]", finalPrincipal);

        return new DefaultAuthenticationHandlerExecutionResult(this, credential, finalPrincipal);
    }

    /**
     * Map attributes. Subclasses can override to provide custom attribute mapping logic.
     *
     * @param attributes the attributes mapped so far
     * @param credential the credential
     * @param principal  the principal extracted from the token
     * @param token      the underlying token object
     */
    protected void mapAttributes(final Map<String, Object> attributes, final Credential credential,
                                 final Principal principal, final T token) {
        LOGGER.trace("Mapping attributes for principal [{}]", principal.getId());
    }
}
