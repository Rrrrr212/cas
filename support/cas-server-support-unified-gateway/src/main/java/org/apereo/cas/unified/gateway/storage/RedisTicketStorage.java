package org.apereo.cas.unified.gateway.storage;

import module java.base;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.redis.core.CasRedisTemplate;
import org.apereo.cas.util.function.FunctionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed implementation of {@link TicketStorage}.
 * <p>
 * Reuses the existing CAS Redis infrastructure ({@link CasRedisTemplate})
 * to store gateway session state in a distributed manner.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@Slf4j
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public class RedisTicketStorage implements TicketStorage {

    private static final String KEY_PREFIX = "unified-gateway:session:";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public RedisTicketStorage(final StringRedisTemplate redisTemplate,
                              final ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(final String key, final Serializable value, final Duration ttl) {
        FunctionUtils.doAndHandle(() -> {
            val fullKey = KEY_PREFIX + key;
            val json = objectMapper.writeValueAsString(Map.of("value", value));
            redisTemplate.opsForValue().set(fullKey, json, ttl);
            LOGGER.debug("Stored gateway session state for key [{}] with TTL [{}]", key, ttl);
            return null;
        });
    }

    @Override
    public @Nullable Serializable retrieve(final String key) {
        return FunctionUtils.doAndHandle(() -> {
            val fullKey = KEY_PREFIX + key;
            val json = redisTemplate.opsForValue().get(fullKey);
            if (json == null) {
                LOGGER.debug("No gateway session state found for key [{}]", key);
                return null;
            }
            @SuppressWarnings("unchecked")
            val map = objectMapper.readValue(json, Map.class);
            LOGGER.debug("Retrieved gateway session state for key [{}]", key);
            return (Serializable) map.get("value");
        });
    }

    @Override
    public boolean delete(final String key) {
        val fullKey = KEY_PREFIX + key;
        val result = Boolean.TRUE.equals(redisTemplate.delete(fullKey));
        LOGGER.debug("Deleted gateway session state for key [{}]: {}", key, result);
        return result;
    }

    @Override
    public boolean contains(final String key) {
        val fullKey = KEY_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }
}
