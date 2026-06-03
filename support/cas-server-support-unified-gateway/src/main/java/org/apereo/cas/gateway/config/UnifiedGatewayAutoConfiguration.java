package org.apereo.cas.gateway.config;

import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.gateway.mfa.DuoGatewayMfaProvider;
import org.apereo.cas.gateway.mfa.GatewayMfaProvider;
import org.apereo.cas.gateway.mfa.GatewayMfaService;
import org.apereo.cas.gateway.mfa.WebAuthnGatewayMfaProvider;
import org.apereo.cas.gateway.routing.AutoProtocolRoutingHandler;
import org.apereo.cas.gateway.routing.GatewayRoutingProperties;
import org.apereo.cas.gateway.storage.HazelcastTicketStorage;
import org.apereo.cas.gateway.storage.RedisTicketStorage;
import org.apereo.cas.gateway.storage.TicketStorage;
import org.apereo.cas.gateway.web.UnifiedAuthController;
import org.apereo.cas.ticket.registry.TicketRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnClass(name = "org.apereo.cas.support.saml.config.CasSamlAutoConfiguration")
public class UnifiedGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "gatewayRoutingProperties")
    public GatewayRoutingProperties gatewayRoutingProperties() {
        return new GatewayRoutingProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = "autoProtocolRoutingHandler")
    public AutoProtocolRoutingHandler autoProtocolRoutingHandler(
        final GatewayRoutingProperties gatewayRoutingProperties) {
        return new AutoProtocolRoutingHandler(gatewayRoutingProperties);
    }

    @Bean
    public WebMvcConfigurer gatewayWebMvcConfigurer(final AutoProtocolRoutingHandler routingHandler) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(routingHandler).addPathPatterns("/gateway/**");
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apereo.cas.ticket.registry.RedisTicketRegistry")
    static class RedisTicketStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = TicketStorage.BEAN_NAME)
        public TicketStorage gatewayTicketStorage(final TicketRegistry ticketRegistry) {
            return new RedisTicketStorage(ticketRegistry);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apereo.cas.ticket.registry.HazelcastTicketRegistry")
    static class HazelcastTicketStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = TicketStorage.BEAN_NAME)
        public TicketStorage gatewayTicketStorage(final TicketRegistry ticketRegistry) {
            return new HazelcastTicketStorage(ticketRegistry);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class GatewayMfaConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "gatewayMfaService")
        public GatewayMfaService gatewayMfaService(
            final ObjectProvider<MultifactorAuthenticationProvider> mfaProviders) {

            val providers = new ArrayList<GatewayMfaProvider>();

            mfaProviders.stream()
                .filter(p -> WebAuthnGatewayMfaProvider.PROVIDER_ID.equals(p.getId()))
                .findFirst()
                .ifPresent(p -> providers.add(new WebAuthnGatewayMfaProvider(p)));

            mfaProviders.stream()
                .filter(p -> DuoGatewayMfaProvider.PROVIDER_ID.equals(p.getId()))
                .findFirst()
                .ifPresent(p -> providers.add(new DuoGatewayMfaProvider(p)));

            return new GatewayMfaService(providers);
        }
    }

    @Bean
    @ConditionalOnBean(name = TicketStorage.BEAN_NAME)
    public UnifiedAuthController unifiedAuthController(
        final GatewayMfaService gatewayMfaService,
        final TicketStorage gatewayTicketStorage) {
        return new UnifiedAuthController(gatewayMfaService, gatewayTicketStorage);
    }
}