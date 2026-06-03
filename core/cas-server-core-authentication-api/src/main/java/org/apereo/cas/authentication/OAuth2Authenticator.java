package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationCloudConfigProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OAuth2Authenticator extends AbstractAuthenticator {

    private final AuthenticationProtocolStrategy strategy;

    public OAuth2Authenticator(final String name,
                               final PrincipalFactory principalFactory,
                               final Integer order,
                               final AuthenticationCloudConfigProperties cloudConfig) {
        super(name, principalFactory, order, cloudConfig);
        this.strategy = new OAuth2ProtocolStrategy(cloudConfig.getOauth2(), principalFactory);
    }

    @Override
    protected AuthenticationProtocolStrategy getProtocolStrategy() {
        return strategy;
    }

    @Override
    public boolean supports(final Credential credential) {
        return credential instanceof TokenCredential tc && "OAUTH2".equalsIgnoreCase(tc.getProtocol());
    }

    @Override
    public boolean supports(final Class<? extends Credential> clazz) {
        return TokenCredential.class.isAssignableFrom(clazz);
    }
}