package org.apereo.cas.configuration.model.core.authentication;

import module java.base;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties;
import org.apereo.cas.configuration.model.support.mongo.BaseMongoDbProperties;
import org.apereo.cas.configuration.support.RequiresModule;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@RequiresModule(name = "cas-server-core-authentication", automated = true)
@Getter
@Setter
@Accessors(chain = true)
@ConfigurationProperties(prefix = "cas.authn.cloud")
@RefreshScope
public class AuthenticationCloudConfigProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = -6548389854310194285L;

    private boolean enabled;

    @NestedConfigurationProperty
    private OAuth2Config oauth2 = new OAuth2Config();

    @NestedConfigurationProperty
    private SamlConfig saml = new SamlConfig();

    @NestedConfigurationProperty
    private LdapConfig ldap = new LdapConfig();

    @NestedConfigurationProperty
    private MongoDbConfig mongoDb = new MongoDbConfig();

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class OAuth2Config implements Serializable {
        @Serial
        private static final long serialVersionUID = 3624781990865491913L;

        private boolean enabled;

        private String clientId;

        private String clientSecret;

        private String introspectionUrl;

        private String userInfoUrl;

        private String tokenUrl;

        private String authorizationUrl;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class SamlConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = -4103042333630202837L;

        private boolean enabled;

        private String entityId;

        private String metadataUrl;

        private String validationUrl;

        private String assertionConsumerServiceUrl;

        private String signingCertificate;

        private String signingKey;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class LdapConfig extends AbstractLdapProperties {
        @Serial
        private static final long serialVersionUID = -1496179941839472914L;

        private boolean enabled;

        private String baseDn;

        private String userFilter;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class MongoDbConfig extends BaseMongoDbProperties {
        @Serial
        private static final long serialVersionUID = -3051573187651648403L;

        private boolean enabled;

        private String databaseName;

        private String collectionName;
    }
}