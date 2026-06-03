package org.apereo.cas.support.unifiedgateway.protocol;

import module java.base;
import java.util.Optional;
import java.util.Set;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

public class AutoProtocolRequestResolver {
    private static final Set<String> OAUTH_PARAMETERS = Set.of(
        "client_id", "response_type", "grant_type", "redirect_uri", "scope", "code_challenge");

    private static final Set<String> SAML_PARAMETERS = Set.of(
        "SAMLRequest", "SAMLResponse", "entityId", "providerId", "RelayState");

    public Optional<GatewayProtocol> resolve(final HttpServletRequest request) {
        val requestUri = StringUtils.defaultString(request.getRequestURI());
        if (requestUri.contains("/oauth2.0/")) {
            return Optional.of(GatewayProtocol.OAUTH2);
        }
        if (requestUri.contains("/idp/profile/SAML2/")) {
            return Optional.of(GatewayProtocol.SAML2);
        }
        if (hasAnyParameter(request, SAML_PARAMETERS)) {
            return Optional.of(GatewayProtocol.SAML2);
        }
        if (hasAnyParameter(request, OAUTH_PARAMETERS)) {
            return Optional.of(GatewayProtocol.OAUTH2);
        }
        return Optional.empty();
    }

    private static boolean hasAnyParameter(final HttpServletRequest request, final Set<String> parameterNames) {
        return parameterNames.stream()
            .map(request::getParameter)
            .anyMatch(StringUtils::isNotBlank);
    }

    public enum GatewayProtocol {
        OAUTH2,
        SAML2
    }
}
