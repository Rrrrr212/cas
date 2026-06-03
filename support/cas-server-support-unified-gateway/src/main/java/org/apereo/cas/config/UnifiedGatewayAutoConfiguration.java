package org.apereo.cas.config;

import module java.base;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.hazelcast.HazelcastTicketRegistryProperties;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.unified.gateway.mfa.GatewayMfaProvider;
import org.apereo.cas.unified.gateway.storage.HazelcastTicketStorage;
import org.apereo.cas.unified.gateway.storage.RedisTicketStorage;
import org.apereo.cas.unified.gateway.storage.TicketStorage;
import org.apereo.cas.unified.gateway.web.UnifiedAuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.List;

/**
 * Auto-configuration for the Unified Gateway module.
 * <p>
 * This configuration registers the unified authentication controller,
 * ticket storage implementations, and MFA provider discovery.
 * It follows the standard CAS {@code cas-server-support-*} module pattern.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@Slf4j
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfiguration
public class UnifiedGatewayAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(name = "unifiedAuthController")
    static class UnifiedGatewayControllerConfiguration {

        @Bean
        public UnifiedAuthController unifiedAuthController(
                @Qualifier(TicketRegistry.BEAN_NAME) final TicketRegistry ticketRegistry,
                @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager,
                @Qualifier("unifiedTicketStorage") final TicketStorage ticketStorage,
                final ObjectProvider<List<GatewayMfaProvider>> mfaProviders) {
            return new UnifiedAuthController(
                ticketRegistry,
                servicesManager,
                ticketStorage,
                mfaProviders.getIfAvailable(List::of)
            );
        }
    }

    @Configuration
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(name = "redisUnifiedTicketStorage")
    static class RedisStorageConfiguration {

        @Bean
        public TicketStorage redisUnifiedTicketStorage(
                final StringRedisTemplate stringRedisTemplate,
                final ObjectMapper objectMapper) {
            LOGGER.debug("Registering Redis-backed TicketStorage for unified gateway");
            return new RedisTicketStorage(stringRedisTemplate, objectMapper);
        }
    }

    @Configuration
    @ConditionalOnClass(HazelcastInstance.class)
    @ConditionalOnBean(HazelcastInstance.class)
    @ConditionalOnMissingBean(name = "hazelcastUnifiedTicketStorage")
    static class HazelcastStorageConfiguration {

        @Bean
        public TicketStorage hazelcastUnifiedTicketStorage(
                final HazelcastInstance hazelcastInstance,
                final CasConfigurationProperties casProperties) {
            LOGGER.debug("Registering Hazelcast-backed TicketStorage for unified gateway");
            return new HazelcastTicketStorage(
                hazelcastInstance,
                casProperties.getTicket().getRegistry().getHazelcast()
            );
        }
    }
}
