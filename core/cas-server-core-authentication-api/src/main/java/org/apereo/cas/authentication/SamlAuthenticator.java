package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationCloudConfigProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamlAuthenticator extends AbstractAuthenticator {

    private final AuthenticationProtocolStrategy strategy;

    public SamlAuthenticator(final String name,
                             final PrincipalFactory principalFactory,
                             final Integer order,
                             final AuthenticationCloudConfigProperties cloudConfig) {
        super(name, principalFactory, order, cloudConfig);
        this.strategy = new SamlProtocolStrategy(cloudConfig.getSaml(), principalFactory);
    }

    @Override
    protected AuthenticationProtocolStrategy getProtocolStrategy() {
        return strategy;
    }

    @Override
    public boolean supports(final Credential credential) {
        return credential instanceof TokenCredential tc && "SAML".equalsIgnoreCase(tc.getProtocol());
    }

    @Override
    public boolean supports(final Class<? extends Credential> clazz) {
        return TokenCredential.class.isAssignableFrom(clazz);
    }
}