package org.apereo.cas.unifiedgateway;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory ticket storage implementation.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
public class InMemoryTicketStorage implements TicketStorage {

    private record TicketEntry(Object ticket, long expirationTime) {}

    private final Map<String, TicketEntry> storage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public InMemoryTicketStorage() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTickets, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void store(final String ticketId, final Object ticket, final Duration ttl) {
        val expirationTime = System.currentTimeMillis() + ttl.toMillis();
        storage.put(ticketId, new TicketEntry(ticket, expirationTime));
        LOGGER.debug("Stored ticket [{}] in memory with TTL [{}]", ticketId, ttl);
    }

    @Override
    public Object retrieve(final String ticketId) {
        val entry = storage.get(ticketId);
        if (entry != null && entry.expirationTime() > System.currentTimeMillis()) {
            LOGGER.debug("Retrieved ticket [{}] from memory", ticketId);
            return entry.ticket();
        }
        if (entry != null) {
            storage.remove(ticketId);
            LOGGER.debug("Ticket [{}] expired and removed from memory", ticketId);
        }
        return null;
    }

    @Override
    public boolean remove(final String ticketId) {
        val removed = storage.remove(ticketId) != null;
        LOGGER.debug("Removed ticket [{}] from memory: [{}]", ticketId, removed);
        return removed;
    }

    @Override
    public boolean exists(final String ticketId) {
        val entry = storage.get(ticketId);
        return entry != null && entry.expirationTime() > System.currentTimeMillis();
    }

    private void cleanupExpiredTickets() {
        val now = System.currentTimeMillis();
        val expiredCount = storage.entrySet().removeIf(entry -> entry.getValue().expirationTime() < now);
        if (expiredCount) {
            LOGGER.debug("Cleaned up expired tickets from memory storage");
        }
    }
}
