package org.apereo.cas.gateway;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.apereo.cas.ticket.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * Hazelcast implementation for Gateway Ticket Storage.
 * Reuses existing CAS HazelcastInstance.
 *
 * @since 7.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class HazelcastTicketStorage implements TicketStorage {

    private final HazelcastInstance hazelcastInstance;
    private static final String MAP_NAME = "cas-gateway-tickets";

    private IMap<String, Ticket> getTicketMap() {
        return hazelcastInstance.getMap(MAP_NAME);
    }

    @Override
    public void save(final Ticket ticket) {
        LOGGER.debug("Saving ticket [{}] to Hazelcast", ticket.getId());
        getTicketMap().set(ticket.getId(), ticket, 
                ticket.getExpirationPolicy().getTimeToLive(), TimeUnit.SECONDS);
    }

    @Override
    public Ticket get(final String ticketId) {
        LOGGER.debug("Fetching ticket [{}] from Hazelcast", ticketId);
        return getTicketMap().get(ticketId);
    }

    @Override
    public boolean delete(final String ticketId) {
        LOGGER.debug("Deleting ticket [{}] from Hazelcast", ticketId);
        return getTicketMap().remove(ticketId) != null;
    }
}
