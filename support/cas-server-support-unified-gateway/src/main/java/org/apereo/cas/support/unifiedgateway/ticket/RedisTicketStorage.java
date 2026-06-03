package org.apereo.cas.support.unifiedgateway.ticket;

import module java.base;
import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import lombok.val;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisTicketStorage implements TicketStorage {
    private final RedisTemplate<String, Serializable> redisTemplate;

    public RedisTicketStorage(final RedisConnectionFactory connectionFactory) {
        val template = new RedisTemplate<String, Serializable>();
        val keySerializer = new StringRedisSerializer();
        val valueSerializer = new JdkSerializationRedisSerializer();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        this.redisTemplate = template;
    }

    @Override
    public String getBackend() {
        return "redis";
    }

    @Override
    public void save(final String ticketId, final Serializable ticketState, final Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(ticketId, ticketState, ttl);
            return;
        }
        redisTemplate.opsForValue().set(ticketId, ticketState);
    }

    @Override
    public Optional<Serializable> get(final String ticketId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(ticketId));
    }

    @Override
    public void delete(final String ticketId) {
        redisTemplate.delete(ticketId);
    }
}
