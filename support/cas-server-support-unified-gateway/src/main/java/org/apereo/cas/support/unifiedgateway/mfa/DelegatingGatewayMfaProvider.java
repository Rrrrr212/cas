package org.apereo.cas.support.unifiedgateway.mfa;

import module java.base;
import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;
import org.apache.commons.lang3.StringUtils;

public record DelegatingGatewayMfaProvider(String family,
                                           MultifactorAuthenticationProvider delegate) implements GatewayMfaProvider {
    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getFamily() {
        return family;
    }

    @Override
    public boolean supports(final String providerId) {
        return StringUtils.equalsIgnoreCase(getId(), providerId)
            || StringUtils.equalsIgnoreCase(getFamily(), providerId);
    }

    @Override
    public boolean isAvailable(final RegisteredService service) throws AuthenticationException {
        return delegate.isAvailable(service);
    }

    @Override
    public MultifactorAuthenticationProvider getDelegate() {
        return delegate;
    }
}
