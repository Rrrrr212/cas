package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationCloudConfigProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Setter
@Getter
public abstract class AbstractAuthenticator extends AbstractPreAndPostProcessingAuthenticationHandler {

    protected final AuthenticationCloudConfigProperties cloudConfig;

    protected AbstractAuthenticator(final String name,
                                    final PrincipalFactory principalFactory,
                                    final Integer order,
                                    final AuthenticationCloudConfigProperties cloudConfig) {
        super(name, principalFactory, order);
        this.cloudConfig = cloudConfig;
    }

    protected abstract AuthenticationProtocolStrategy getProtocolStrategy();

    @Override
    protected AuthenticationHandlerExecutionResult doAuthentication(final Credential credential, final Service service)
        throws Throwable {
        val token = credential.getId();
        if (StringUtils.isBlank(token)) {
            throw new FailedLoginException("Token must not be blank");
        }
        LOGGER.debug("Authenticating token via protocol [{}]", getProtocolStrategy().getProtocolName());
        val strategy = getProtocolStrategy();
        if (!strategy.validateToken(token, service)) {
            throw new FailedLoginException("Token validation failed for protocol " + strategy.getProtocolName());
        }
        val principal = resolveAndMapPrincipal(token, service, strategy);
        LOGGER.debug("Authenticated principal [{}] via protocol [{}]", principal, strategy.getProtocolName());
        return createHandlerResult(credential, principal);
    }

    protected Principal resolveAndMapPrincipal(final String token, final Service service,
                                               final AuthenticationProtocolStrategy strategy) throws Throwable {
        return strategy.resolvePrincipal(token, service);
    }
}