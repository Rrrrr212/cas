package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration class for authentication beans.
 */
@Configuration
public class AuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PrincipalFactory principalFactory() {
        return new DefaultPrincipalFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public OAuth2Authenticator oAuth2Authenticator(PrincipalFactory principalFactory,
                                                  AuthenticationProperties authenticationProperties,
                                                  RestTemplate restTemplate) {
        return new OAuth2Authenticator(principalFactory, authenticationProperties, restTemplate);
    }

    @Bean
    public SamlAuthenticator samlAuthenticator(PrincipalFactory principalFactory,
                                              AuthenticationProperties authenticationProperties) {
        return new SamlAuthenticator(principalFactory, authenticationProperties);
    }
}
