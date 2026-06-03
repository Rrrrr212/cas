package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.unifiedgateway.*;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * Auto configuration for unified gateway.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfiguration
public class CasUnifiedGatewayAutoConfiguration {

    private static final BeanCondition CONDITION_REDIS = BeanCondition.onClass("org.springframework.data.redis.core.RedisTemplate");
    private static final BeanCondition CONDITION_HAZELCAST = BeanCondition.onClass("com.hazelcast.core.HazelcastInstance");

    @Configuration(value = "CasUnifiedGatewayProtocolConfiguration", proxyBeanMethods = false)
    static class CasUnifiedGatewayProtocolConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean
        public ProtocolRouter protocolRouter(final ConfigurableApplicationContext applicationContext) {
            return new ProtocolRouter(applicationContext);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean
        public UnifiedAuthController unifiedAuthController(
                final ProtocolRouter protocolRouter,
                final TicketStorage ticketStorage) {
            return new UnifiedAuthController(protocolRouter, ticketStorage);
        }
    }

    @Configuration(value = "CasUnifiedGatewayTicketStorageConfiguration", proxyBeanMethods = false)
    static class CasUnifiedGatewayTicketStorageConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = TicketStorage.BEAN_NAME)
        public TicketStorage ticketStorage(final ConfigurableApplicationContext applicationContext) {
            return BeanSupplier.of(TicketStorage.class)
                    .when(CONDITION_REDIS.given(applicationContext.getEnvironment()))
                    .supply(() -> new RedisTicketStorage(applicationContext))
                    .when(CONDITION_HAZELCAST.given(applicationContext.getEnvironment()))
                    .supply(() -> new HazelcastTicketStorage(applicationContext))
                    .otherwise(InMemoryTicketStorage::new)
                    .get();
        }
    }
}
