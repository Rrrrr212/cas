package org.apereo.cas.unifiedgateway;

import java.time.Duration;

/**
 * Ticket storage interface for unified gateway.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
public interface TicketStorage {

    String BEAN_NAME = "ticketStorage";

    /**
     * Store a ticket.
     *
     * @param ticketId the ticket id
     * @param ticket the ticket data
     * @param ttl the time to live
     */
    void store(String ticketId, Object ticket, Duration ttl);

    /**
     * Retrieve a ticket.
     *
     * @param ticketId the ticket id
     * @return the ticket data, or null if not found
     */
    Object retrieve(String ticketId);

    /**
     * Remove a ticket.
     *
     * @param ticketId the ticket id
     * @return true if ticket existed and was removed
     */
    boolean remove(String ticketId);

    /**
     * Check if ticket exists.
     *
     * @param ticketId the ticket id
     * @return true if ticket exists
     */
    boolean exists(String ticketId);
}
