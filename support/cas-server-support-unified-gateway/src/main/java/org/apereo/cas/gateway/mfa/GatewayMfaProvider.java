package org.apereo.cas.gateway.mfa;

import org.apereo.cas.authentication.principal.Principal;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;

public interface GatewayMfaProvider extends Serializable {

    String getProviderId();

    String getFriendlyName();

    boolean isAvailable();

    boolean isEnrolled(Principal principal);

    GatewayMfaResult authenticate(Principal principal, Map<String, Object> challengeResponse);

    record GatewayMfaResult(boolean success, @Nullable String errorMessage, @Nullable Map<String, Object> metadata) {

        public static GatewayMfaResult success(final Map<String, Object> metadata) {
            return new GatewayMfaResult(true, null, metadata);
        }

        public static GatewayMfaResult failure(final String errorMessage) {
            return new GatewayMfaResult(false, errorMessage, null);
        }
    }
}