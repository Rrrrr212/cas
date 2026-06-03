package org.apereo.cas.support.unifiedgateway.ticket;

import module java.base;
import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;

public interface TicketStorage {
    String getBackend();

    void save(String ticketId, Serializable ticketState, Duration ttl);

    Optional<Serializable> get(String ticketId);

    void delete(String ticketId);
}
