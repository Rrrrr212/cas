package org.apereo.cas.unified.gateway.storage;

import module java.base;
import org.apereo.cas.configuration.model.support.hazelcast.HazelcastTicketRegistryProperties;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.util.function.FunctionUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Hazelcast-backed implementation of {@link TicketStorage}.
 * <p>
 * Reuses the existing CAS Hazelcast infrastructure ({@link HazelcastInstance})
 * to store gateway session state in a distributed manner.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@Slf4j
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public class HazelcastTicketStorage implements TicketStorage {

    private static final String MAP_NAME = "unifiedGatewaySessions";

    private final HazelcastInstance hazelcastInstance;

    private final HazelcastTicketRegistryProperties properties;

    public HazelcastTicketStorage(final HazelcastInstance hazelcastInstance,
                                  final HazelcastTicketRegistryProperties properties) {
        this.hazelcastInstance = hazelcastInstance;
        this.properties = properties;
    }

    @Override
    public void store(final String key, final Serializable value, final Duration ttl) {
        FunctionUtils.doAndHandle(() -> {
            val map = getMap();
            val ttlSeconds = ttl.getSeconds();
            if (ttlSeconds > 0 && ttlSeconds < Integer.MAX_VALUE) {
                map.put(key, value, (int) ttlSeconds, TimeUnit.SECONDS);
            } else {
                map.put(key, value);
            }
            LOGGER.debug("Stored gateway session state for key [{}] with TTL [{}]", key, ttl);
            return null;
        });
    }

    @Override
    public @Nullable Serializable retrieve(final String key) {
        return FunctionUtils.doAndHandle(() -> {
            val map = getMap();
            val value = map.get(key);
            LOGGER.debug("Retrieved gateway session state for key [{}]", key);
            return value;
        });
    }

    @Override
    public boolean delete(final String key) {
        val map = getMap();
        val removed = map.remove(key);
        LOGGER.debug("Deleted gateway session state for key [{}]: {}", key, removed != null);
        return removed != null;
    }

    @Override
    public boolean contains(final String key) {
        val map = getMap();
        return map.containsKey(key);
    }

    private IMap<String, Serializable> getMap() {
        return hazelcastInstance.getMap(MAP_NAME);
    }
}
