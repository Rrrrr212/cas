package org.apereo.cas.gateway;

public enum ProtocolType {

    OAUTH2("oauth2", "/oauth2.0/authorize"),

    SAML2("saml2", "/idp/profile/SAML2/Redirect/SSO");

    private final String protocolName;

    private final String defaultEndpoint;

    ProtocolType(final String protocolName, final String defaultEndpoint) {
        this.protocolName = protocolName;
        this.defaultEndpoint = defaultEndpoint;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public static ProtocolType fromParameter(final String parameter) {
        if (parameter == null || parameter.isBlank()) {
            return null;
        }
        for (val type : values()) {
            if (type.protocolName.equalsIgnoreCase(parameter.trim())) {
                return type;
            }
        }
        return null;
    }
}