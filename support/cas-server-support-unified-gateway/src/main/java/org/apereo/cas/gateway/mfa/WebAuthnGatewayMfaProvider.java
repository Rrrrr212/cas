package org.apereo.cas.gateway.mfa;

import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.Principal;

import java.io.Serial;
import java.util.Map;

public class WebAuthnGatewayMfaProvider implements GatewayMfaProvider {

    @Serial
    private static final long serialVersionUID = 7012345678901234567L;

    public static final String PROVIDER_ID = "mfa-webauthn";

    private final MultifactorAuthenticationProvider webauthnProvider;

    public WebAuthnGatewayMfaProvider(final MultifactorAuthenticationProvider webauthnProvider) {
        this.webauthnProvider = webauthnProvider;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public String getFriendlyName() {
        return "WebAuthn / FIDO2";
    }

    @Override
    public boolean isAvailable() {
        return webauthnProvider != null && webauthnProvider.isAvailable(null);
    }

    @Override
    public boolean isEnrolled(final Principal principal) {
        if (webauthnProvider == null) {
            return false;
        }
        val deviceManager = webauthnProvider.getDeviceManager();
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