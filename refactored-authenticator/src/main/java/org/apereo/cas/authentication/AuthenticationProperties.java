package org.apereo.cas.authentication;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for authentication, managed via Spring Cloud Config.
 * Provides centralized configuration for authentication-related settings.
 */
@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "cas.authentication")
public class AuthenticationProperties {

    private Map<String, String> attributeMapping = new HashMap<>();

    private OAuth2Properties oauth2 = new OAuth2Properties();

    private SamlProperties saml = new SamlProperties();

    private LdapProperties ldap = new LdapProperties();

    private MongoProperties mongo = new MongoProperties();

    @Getter
    @Setter
    public static class OAuth2Properties {
        private String tokenValidationUrl;
        private String clientId;
        private String clientSecret;
        private int connectionTimeout = 5000;
    }

    @Getter
    @Setter
    public static class SamlProperties {
        private String idpMetadataUrl;
        private String entityId;
        private boolean signRequests = true;
    }

    @Getter
    @Setter
    public static class LdapProperties {
        private String url;
        private String baseDn;
        private String userDn;
        private String password;
    }

    @Getter
    @Setter
    public static class MongoProperties {
        private String uri;
        private String database;
        private String collection;
    }
}
