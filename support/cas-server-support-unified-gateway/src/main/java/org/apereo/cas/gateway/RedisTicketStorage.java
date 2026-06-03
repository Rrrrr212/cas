package org.apereo.cas.gateway;

import org.apereo.cas.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation for Gateway Ticket Storage.
 * Reuses existing CAS RedisTemplate.
 *
 * @since 7.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisTicketStorage implements TicketStorage {
    
    private final RedisTemplate<String, Ticket> redisTemplate;
    private static final String PREFIX = "CAS_GATEWAY_TICKET:";

    @Override
    public void save(final Ticket ticket) {
        LOGGER.debug("Saving ticket [{}] to Redis", ticket.getId());
        redisTemplate.opsForValue().set(PREFIX + ticket.getId(), ticket, 
                ticket.getExpirationPolicy().getTimeToLive(), TimeUnit.SECONDS);
    }

    @Override
    public Ticket get(final String ticketId) {
        LOGGER.debug("Fetching ticket [{}] from Redis", ticketId);
        return redisTemplate.opsForValue().get(PREFIX + ticketId);
    }

    @Override
    public boolean delete(final String ticketId) {
        LOGGER.debug("Deleting ticket [{}] from Redis", ticketId);
        return Boolean.TRUE.equals(redisTemplate.delete(PREFIX + ticketId));
    }
}
