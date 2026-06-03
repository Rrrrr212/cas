package org.apereo.cas.config;

import module java.base;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.OAuth2Authenticator;
import org.apereo.cas.authentication.OAuth2ProtocolStrategy;
import org.apereo.cas.authentication.SamlAuthenticator;
import org.apereo.cas.authentication.SamlProtocolStrategy;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Authentication)
@ConditionalOnProperty(name = "cas.authn.cloud.enabled", havingValue = "true")
@Configuration(value = "CasCoreAuthenticationCloudConfiguration", proxyBeanMethods = false)
public class CasCoreAuthenticationCloudConfiguration {

    @Configuration(value = "CasCoreAuthenticationCloudProtocolStrategiesConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    static class CasCoreAuthenticationCloudProtocolStrategiesConfiguration {

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnProperty(name = "cas.authn.cloud.oauth2.enabled", havingValue = "true")
        public OAuth2ProtocolStrategy oauth2ProtocolStrategy(
            final CasConfigurationProperties casProperties,
            @Qualifier(PrincipalFactory.BEAN_NAME) final PrincipalFactory principalFactory) {
            val cloudConfig = casProperties.getAuthn().getCloud();
            return new OAuth2ProtocolStrategy(cloudConfig.getOauth2(), principalFactory);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnProperty(name = "cas.authn.cloud.saml.enabled", havingValue = "true")
        public SamlProtocolStrategy samlProtocolStrategy(
            final CasConfigurationProperties casProperties,
            @Qualifier(PrincipalFactory.BEAN_NAME) final PrincipalFactory principalFactory) {
            val cloudConfig = casProperties.getAuthn().getCloud();
            return new SamlProtocolStrategy(cloudConfig.getSaml(), principalFactory);
        }
    }

    @Configuration(value = "CasCoreAuthenticationCloudHandlersConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    static class CasCoreAuthenticationCloudHandlersConfiguration {

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "oauth2Authenticator")
        @ConditionalOnProperty(name = "cas.authn.cloud.oauth2.enabled", havingValue = "true")
        public AuthenticationHandler oauth2Authenticator(
            final CasConfigurationProperties casProperties,
            @Qualifier(PrincipalFactory.BEAN_NAME) final PrincipalFactory principalFactory) {
            val cloudConfig = casProperties.getAuthn().getCloud();
            return new OAuth2Authenticator("oauth2-cloud-authenticator", principalFactory, 100, cloudConfig);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "samlAuthenticator")
        @ConditionalOnProperty(name = "cas.authn.cloud.saml.enabled", havingValue = "true")
        public AuthenticationHandler samlAuthenticator(
            final CasConfigurationProperties casProperties,
            @Qualifier(PrincipalFactory.BEAN_NAME) final PrincipalFactory principalFactory) {
            val cloudConfig = casProperties.getAuthn().getCloud();
            return new SamlAuthenticator("saml-cloud-authenticator", principalFactory, 200, cloudConfig);
        }
    }

    @Configuration(value = "CasCoreAuthenticationCloudPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    static class CasCoreAuthenticationCloudPlanConfiguration {

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnProperty(name = "cas.authn.cloud.enabled", havingValue = "true")
        public AuthenticationEventExecutionPlanConfigurer cloudAuthenticationPlanConfigurer(
            final ConfigurableApplicationContext applicationContext,
            @Qualifier("oauth2Authenticator") final AuthenticationHandler oauth2Authenticator,
            @Qualifier("samlAuthenticator") final AuthenticationHandler samlAuthenticator,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager,
            @Qualifier(PrincipalResolver.BEAN_NAME_PRINCIPAL_RESOLVER) final PrincipalResolver principalResolver) {
            return plan -> {
                if (BeanSupplier.isNotProxy(oauth2Authenticator)) {
                    LOGGER.info("Registering OAuth2 cloud authenticator");
                    plan.registerAuthenticationHandlerWithPrincipalResolver(oauth2Authenticator, principalResolver);
                }
                if (BeanSupplier.isNotProxy(samlAuthenticator)) {
                    LOGGER.info("Registering SAML cloud authenticator");
                    plan.registerAuthenticationHandlerWithPrincipalResolver(samlAuthenticator, principalResolver);
                }
            };
        }
    }
}