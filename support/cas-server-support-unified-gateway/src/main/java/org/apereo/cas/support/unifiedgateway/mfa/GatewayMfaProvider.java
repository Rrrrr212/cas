package org.apereo.cas.support.unifiedgateway.mfa;

import module java.base;
import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;

public interface GatewayMfaProvider {
    String getId();

    String getFamily();

    boolean supports(String providerId);

    boolean isAvailable(RegisteredService service) throws AuthenticationException;

    MultifactorAuthenticationProvider getDelegate();
}
