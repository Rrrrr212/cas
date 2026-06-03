package org.apereo.cas.authentication;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * OAuth2 credential representing an OAuth2 token and related data.
 */
@Getter
@Setter
public class OAuth2Credential implements Credential {

    private static final long serialVersionUID = 1L;

    private String token;

    private Map<String, Object> validationResponse;

    public OAuth2Credential(String token) {
        this.token = token;
    }

    @Override
    public String getId() {
        return token != null ? token.substring(0, Math.min(20, token.length())) : "unknown";
    }
}
