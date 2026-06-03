package org.apereo.cas.gateway.mfa;

import org.apereo.cas.authentication.principal.Principal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GatewayMfaService {

    private final List<GatewayMfaProvider> providers;

    public GatewayMfaService(final List<GatewayMfaProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<GatewayMfaProvider> getAvailableProviders() {
        return providers.stream().filter(GatewayMfaProvider::isAvailable).toList();
    }

    public Optional<GatewayMfaProvider> findProvider(final String providerId) {
        return providers.stream()
            .filter(p -> p.getProviderId().equalsIgnoreCase(providerId))
            .findFirst();
    }

    public GatewayMfaResult authenticate(final String providerId, final Principal principal,
                                          final Map<String, Object> challengeResponse) {
        val provider = findProvider(providerId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown MFA provider: " + providerId));
        return provider.authenticate(principal, challengeResponse);
    }
}