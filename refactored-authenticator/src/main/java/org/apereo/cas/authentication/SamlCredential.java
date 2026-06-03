package org.apereo.cas.authentication;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * SAML credential representing a SAML assertion and related data.
 */
@Getter
@Setter
public class SamlCredential implements Credential {

    private static final long serialVersionUID = 1L;

    private String assertion;

    private Map<String, Object> assertionData;

    public SamlCredential(String assertion) {
        this.assertion = assertion;
    }

    @Override
    public String getId() {
        return "saml_" + (assertion != null ? assertion.hashCode() : "unknown");
    }
}
