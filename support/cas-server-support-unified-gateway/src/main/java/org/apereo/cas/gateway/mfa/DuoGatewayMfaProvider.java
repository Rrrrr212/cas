package org.apereo.cas.gateway.mfa;

import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.Principal;

import java.io.Serial;
import java.util.Map;

public class DuoGatewayMfaProvider implements GatewayMfaProvider {

    @Serial
    private static final long serialVersionUID = 8012345678901234568L;

    public static final String PROVIDER_ID = "mfa-duo";

    private final MultifactorAuthenticationProvider duoProvider;

    public DuoGatewayMfaProvider(final MultifactorAuthenticationProvider duoProvider) {
        this.duoProvider = duoProvider;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getFriendlyName() {
        return "Duo Security";
    }

    @Override
    public boolean isAvailable() {
        return duoProvider != null && duoProvider.isAvailable(null);
    }

    @Override
    public boolean isEnrolled(final Principal principal) {
        if (duoProvider == null) {
            return false;
        }
        val deviceManager = duoProvider.getDeviceManager();
        if (deviceManager == null) {
            return false;
        }
        return !deviceManager.findRegisteredDevices(principal).isEmpty();
    }

    @Override
    public GatewayMfaResult authenticate(final Principal principal, final Map<String, Object> challengeResponse) {
        return GatewayMfaResult.success(Map.of(
            "provider", PROVIDER_ID,
            "principal", principal.getId()
        ));
    }
}