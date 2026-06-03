package org.apereo.cas.authentication;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 authenticator implementation for OAuth2 token validation and authentication.
 */
@Slf4j
public class OAuth2Authenticator extends AbstractAuthenticator<OAuth2Credential, Principal> {

    private final RestTemplate restTemplate;

    public OAuth2Authenticator(PrincipalFactory principalFactory,
                               AuthenticationProperties authenticationProperties,
                               RestTemplate restTemplate) {
        super(principalFactory, authenticationProperties);
        this.restTemplate = restTemplate;
    }

    @Override
    protected void validateCredential(OAuth2Credential credential) throws AuthenticationException {
        LOGGER.debug("Validating OAuth2 credential: [{}]", credential.getToken());
        AuthenticationProperties.OAuth2Properties oauthProps = authenticationProperties.getOauth2();
        
        try {
            Map<String, Object> validationRequest = new HashMap<>();
            validationRequest.put("token", credential.getToken());
            validationRequest.put("client_id", oauthProps.getClientId());
            validationRequest.put("client_secret", oauthProps.getClientSecret());

            Map<String, Object> response = restTemplate.postForObject(
                oauthProps.getTokenValidationUrl(), 
                validationRequest, 
                Map.class
            );

            if (response == null || !Boolean.TRUE.equals(response.get("valid"))) {
                throw new AuthenticationException("Invalid OAuth2 token");
            }

            credential.setValidationResponse(response);
        } catch (Exception e) {
            LOGGER.error("OAuth2 token validation failed", e);
            throw new AuthenticationException("OAuth2 token validation error", e);
        }
    }

    @Override
    protected String extractPrincipalId(OAuth2Credential credential, Map<String, Object> attributes) {
        Object userId = attributes.get("user_id");
        return userId != null ? userId.toString() : credential.getToken().substring(0, 10);
    }

    @Override
    protected Map<String, Object> extractAttributes(OAuth2Credential credential) {
        Map<String, Object> rawAttributes = credential.getValidationResponse();
        return mapAttributes(rawAttributes);
    }

    @Override
    public boolean supports(Class<?> credentialType) {
        return OAuth2Credential.class.isAssignableFrom(credentialType);
    }

    @Override
    public String getName() {
        return "OAuth2Authenticator";
    }
}
