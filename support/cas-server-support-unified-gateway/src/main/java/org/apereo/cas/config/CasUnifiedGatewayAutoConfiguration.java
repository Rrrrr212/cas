package org.apereo.cas.config;

import module java.base;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.support.oauth.web.endpoints.OAuth20AuthorizeEndpointController;
import org.apereo.cas.support.saml.web.idp.profile.sso.SSOSamlIdPPostProfileHandlerController;
import org.apereo.cas.support.unifiedgateway.mfa.GatewayMfaProviderRegistry;
import org.apereo.cas.support.unifiedgateway.protocol.AutoProtocolRequestResolver;
import org.apereo.cas.support.unifiedgateway.protocol.AutoProtocolRoutingAspect;
import org.apereo.cas.support.unifiedgateway.ticket.HazelcastTicketStorage;
import org.apereo.cas.support.unifiedgateway.ticket.RedisTicketStorage;
import org.apereo.cas.support.unifiedgateway.ticket.TicketStorage;
import org.apereo.cas.support.unifiedgateway.web.UnifiedAuthController;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@AutoConfiguration(after = {
    CasOAuth20AutoConfiguration.class,
    CasSamlIdPAutoConfiguration.class
})
public class CasUnifiedGatewayAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AutoProtocolRequestResolver autoProtocolRequestResolver() {
        return new AutoProtocolRequestResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AutoProtocolRoutingAspect autoProtocolRoutingAspect(
        final AutoProtocolRequestResolver autoProtocolRequestResolver,
        final ObjectProvider<OAuth20AuthorizeEndpointController<?>> authorizeController,
        final ObjectProvider<SSOSamlIdPPostProfileHandlerController> ssoPostProfileHandlerController) {
        return new AutoProtocolRoutingAspect(autoProtocolRequestResolver, authorizeController, ssoPostProfileHandlerController);
    }

    @Bean
    @ConditionalOnMissingBean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public GatewayMfaProviderRegistry gatewayMfaProviderRegistry(
        final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProviders) {
        return new GatewayMfaProviderRegistry(multifactorAuthenticationProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public UnifiedAuthController unifiedAuthController(
        final GatewayMfaProviderRegistry gatewayMfaProviderRegistry,
        final ObjectProvider<TicketStorage> ticketStorages) {
        return new UnifiedAuthController(gatewayMfaProviderRegistry, ticketStorages);
    }

    @Bean
    @ConditionalOnBean(name = "redisTicketConnectionFactory")
    @ConditionalOnMissingBean(name = "redisTicketStorage")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketStorage redisTicketStorage(
        @Qualifier("redisTicketConnectionFactory")
        final RedisConnectionFactory redisTicketConnectionFactory) {
        return new RedisTicketStorage(redisTicketConnectionFactory);
    }

    @Bean
    @ConditionalOnBean(name = "casTicketRegistryHazelcastInstance")
    @ConditionalOnMissingBean(name = "hazelcastTicketStorage")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketStorage hazelcastTicketStorage(
        @Qualifier("casTicketRegistryHazelcastInstance")
        final HazelcastInstance casTicketRegistryHazelcastInstance) {
        return new HazelcastTicketStorage(casTicketRegistryHazelcastInstance, "cas:unified:gateway:tickets");
    }
}
