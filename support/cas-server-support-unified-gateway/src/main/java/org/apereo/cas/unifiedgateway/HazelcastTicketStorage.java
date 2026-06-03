package org.apereo.cas.unifiedgateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.util.function.FunctionUtils;
import org.springframework.context.ApplicationContext;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Hazelcast-based ticket storage implementation.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class HazelcastTicketStorage implements TicketStorage {

    private static final String MAP_NAME = "cas-unified-gateway-tickets";

    private final ApplicationContext applicationContext;

    private Object hazelcastInstance;
    private Object ticketMap;

    private Object getHazelcastInstance() {
        if (hazelcastInstance == null) {
            hazelcastInstance = FunctionUtils.doUnchecked(() -> {
                val clazz = Class.forName("com.hazelcast.core.HazelcastInstance");
                return applicationContext.getBeanProvider(clazz).getIfAvailable();
            });
        }
        return hazelcastInstance;
    }

    private Object getTicketMap() {
        if (ticketMap == null) {
            val instance = getHazelcastInstance();
            if (instance != null) {
                ticketMap = FunctionUtils.doUnchecked(() -> {
                    val getMapMethod = instance.getClass().getMethod("getMap", String.class);
                    return getMapMethod.invoke(instance, MAP_NAME);
                });
            }
        }
        return ticketMap;
    }

    @Override
    public void store(final String ticketId, final Object ticket, final Duration ttl) {
        val map = getTicketMap();
        if (map != null) {
            FunctionUtils.doUnchecked(() -> {
                val putMethod = map.getClass().getMethod("put", Object.class, Object.class, long.class, TimeUnit.class);
                putMethod.invoke(map, ticketId, ticket, ttl.toSeconds(), TimeUnit.SECONDS);
                LOGGER.debug("Stored ticket [{}] in Hazelcast with TTL [{}]", ticketId, ttl);
            });
        } else {
            LOGGER.warn("HazelcastInstance not available, cannot store ticket [{}]", ticketId);
        }
    }

    @Override
    public Object retrieve(final String ticketId) {
        val map = getTicketMap();
        if (map != null) {
            return FunctionUtils.doUnchecked(() -> {
                val getMethod = map.getClass().getMethod("get", Object.class);
                val result = getMethod.invoke(map, ticketId);
                LOGGER.debug("Retrieved ticket [{}] from Hazelcast: [{}]", ticketId, result != null);
                return result;
            });
        }
        LOGGER.warn("HazelcastInstance not available, cannot retrieve ticket [{}]", ticketId);
        return null;
    }

    @Override
    public boolean remove(final String ticketId) {
        val map = getTicketMap();
        if (map != null) {
            return FunctionUtils.doUnchecked(() -> {
                val removeMethod = map.getClass().getMethod("remove", Object.class);
                val result = removeMethod.invoke(map, ticketId);
                LOGGER.debug("Removed ticket [{}] from Hazelcast", ticketId);
                return result != null;
            });
        }
        LOGGER.warn("HazelcastInstance not available, cannot remove ticket [{}]", ticketId);
        return false;
    }

    @Override
    public boolean exists(final String ticketId) {
        val map = getTicketMap();
        if (map != null) {
            return FunctionUtils.doUnchecked(() -> {
                val containsKeyMethod = map.getClass().getMethod("containsKey", Object.class);
                val result = containsKeyMethod.invoke(map, ticketId);
                return result != null && Boolean.TRUE.equals(result);
            });
        }
        return false;
    }
}
