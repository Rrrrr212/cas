package org.apereo.cas.support.unifiedgateway.mfa;

import module java.base;
import java.util.List;
import java.util.Optional;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

@RequiredArgsConstructor
public class GatewayMfaProviderRegistry {
    private final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProviders;

    public List<GatewayMfaProvider> getProviders() {
        return multifactorAuthenticationProviders.orderedStream()
            .filter(this::supportsGateway)
            .map(this::toGatewayProvider)
            .toList();
    }

    public Optional<GatewayMfaProvider> findProvider(final String providerId) {
        return getProviders().stream().filter(provider -> provider.supports(providerId)).findFirst();
    }

    private boolean supportsGateway(final MultifactorAuthenticationProvider provider) {
        val id = StringUtils.defaultString(provider.getId());
        return id.startsWith("mfa-duo") || id.startsWith("mfa-webauthn");
    }

    private GatewayMfaProvider toGatewayProvider(final MultifactorAuthenticationProvider provider) {
        val family = provider.getId().startsWith("mfa-duo") ? "duo" : "webauthn";
        return new DelegatingGatewayMfaProvider(family, provider);
    }
}
