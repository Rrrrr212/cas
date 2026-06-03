package org.apereo.cas.unifiedgateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.util.function.FunctionUtils;
import org.springframework.context.ApplicationContext;
import java.time.Duration;

/**
 * Redis-based ticket storage implementation.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisTicketStorage implements TicketStorage {

    private final ApplicationContext applicationContext;

    private Object redisTemplate;

    private Object getRedisTemplate() {
        if (redisTemplate == null) {
            redisTemplate = FunctionUtils.doUnchecked(() -> {
                val clazz = Class.forName("org.springframework.data.redis.core.RedisTemplate");
                return applicationContext.getBeanProvider(clazz).getIfAvailable();
            });
        }
        return redisTemplate;
    }

    @Override
    public void store(final String ticketId, final Object ticket, final Duration ttl) {
        val template = getRedisTemplate();
        if (template != null) {
            FunctionUtils.doUnchecked(() -> {
                val opsMethod = template.getClass().getMethod("opsForValue");
                val ops = opsMethod.invoke(template);
                val setMethod = ops.getClass().getMethod("set", String.class, Object.class, Duration.class);
                setMethod.invoke(ops, ticketId, ticket, ttl);
                LOGGER.debug("Stored ticket [{}] in Redis with TTL [{}]", ticketId, ttl);
            });
        } else {
            LOGGER.warn("RedisTemplate not available, cannot store ticket [{}]", ticketId);
        }
    }

    @Override
    public Object retrieve(final String ticketId) {
        val template = getRedisTemplate();
        if (template != null) {
            return FunctionUtils.doUnchecked(() -> {
                val opsMethod = template.getClass().getMethod("opsForValue");
                val ops = opsMethod.invoke(template);
                val getMethod = ops.getClass().getMethod("get", String.class);
                val result = getMethod.invoke(ops, ticketId);
                LOGGER.debug("Retrieved ticket [{}] from Redis: [{}]", ticketId, result != null);
                return result;
            });
        }
        LOGGER.warn("RedisTemplate not available, cannot retrieve ticket [{}]", ticketId);
        return null;
    }

    @Override
    public boolean remove(final String ticketId) {
        val template = getRedisTemplate();
        if (template != null) {
            return FunctionUtils.doUnchecked(() -> {
                val deleteMethod = template.getClass().getMethod("delete", String.class);
                val result = deleteMethod.invoke(template, ticketId);
                val success = result != null && Boolean.TRUE.equals(result);
                LOGGER.debug("Removed ticket [{}] from Redis: [{}]", ticketId, success);
                return success;
            });
        }
        LOGGER.warn("RedisTemplate not available, cannot remove ticket [{}]", ticketId);
        return false;
    }

    @Override
    public boolean exists(final String ticketId) {
        val template = getRedisTemplate();
        if (template != null) {
            return FunctionUtils.doUnchecked(() -> {
                val hasKeyMethod = template.getClass().getMethod("hasKey", String.class);
                val result = hasKeyMethod.invoke(template, ticketId);
                return result != null && Boolean.TRUE.equals(result);
            });
        }
        return false;
    }
}
