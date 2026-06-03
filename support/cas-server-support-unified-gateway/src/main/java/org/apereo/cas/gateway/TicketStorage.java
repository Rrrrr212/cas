package org.apereo.cas.gateway;

import org.apereo.cas.ticket.Ticket;

/**
 * Unified ticket storage interface for the gateway.
 * Implementations should provide backend storage mechanisms like Redis or Hazelcast
 * by reusing existing CAS cluster components.
 *
 * @since 7.0.0
 */
public interface TicketStorage {
    
    /**
     * Save the ticket to the underlying storage.
     *
     * @param ticket the ticket to save
     */
    void save(Ticket ticket);
    
    /**
     * Retrieve the ticket by its identifier.
     *
     * @param ticketId the ticket id
     * @return the ticket, or null if not found
     */
    Ticket get(String ticketId);
    
    /**
     * Delete the ticket from storage.
     *
     * @param ticketId the ticket id
     * @return true if deleted successfully, false otherwise
     */
    boolean delete(String ticketId);
}
