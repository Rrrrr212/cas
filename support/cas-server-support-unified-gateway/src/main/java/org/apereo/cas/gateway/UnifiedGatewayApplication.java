package org.apereo.cas.gateway;

import org.apereo.cas.services.ServicesManager;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;

/**
 * Auto configuration for the Unified Gateway.
 *
 * @since 7.0.0
 */
@AutoConfiguration
public class UnifiedGatewayApplication {

    @Bean
    @ConditionalOnMissingBean(name = "redisTicketStorage")
    @ConditionalOnClass(RedisTemplate.class)
    public TicketStorage redisTicketStorage(
            @Qualifier("redisTemplate") final RedisTemplate<String, org.apereo.cas.ticket.Ticket> redisTemplate) {
        return new RedisTicketStorage(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(name = "hazelcastTicketStorage")
    @ConditionalOnClass(HazelcastInstance.class)
    public TicketStorage hazelcastTicketStorage(
            @Qualifier("casTicketRegistryHazelcastInstance") final HazelcastInstance hazelcastInstance) {
        return new HazelcastTicketStorage(hazelcastInstance);
    }

    @Bean
    @ConditionalOnMissingBean(name = "unifiedAuthController")
    public UnifiedAuthController unifiedAuthController(
            @Qualifier("servicesManager") final ServicesManager servicesManager,
            @Qualifier("redisTicketStorage") final TicketStorage ticketStorage,
            final List<GatewayMfaProvider> mfaProviders) {
        return new UnifiedAuthController(servicesManager, ticketStorage, mfaProviders);
    }
}
