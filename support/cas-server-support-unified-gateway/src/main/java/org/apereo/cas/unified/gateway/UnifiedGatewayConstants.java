package org.apereo.cas.unified.gateway;

import module java.base;
import org.apereo.cas.configuration.support.RequiresModule;
import org.jspecify.annotations.NullMarked;

/**
 * Constants used across the unified gateway module.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public final class UnifiedGatewayConstants {

    private UnifiedGatewayConstants() {
    }

    public static final String PARAM_PROTOCOL = "protocol";
    public static final String PROTOCOL_OAUTH2 = "oauth2";
    public static final String PROTOCOL_SAML2 = "saml2";
    public static final String PARAM_CLIENT_ID = "client_id";
    public static final String PARAM_RESPONSE_TYPE = "response_type";
    public static final String PARAM_REDIRECT_URI = "redirect_uri";
    public static final String PARAM_SAML_REQUEST = "SAMLRequest";
    public static final String PARAM_SAML_RESPONSE = "SAMLResponse";
    public static final String GATEWAY_ENDPOINT_BASE = "/unified-gateway";
    public static final String GATEWAY_AUTH_PATH = "/auth";
}
