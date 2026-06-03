package org.apereo.cas.gateway.storage;

import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.jspecify.annotations.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor
public class RedisTicketStorage implements TicketStorage {

    private final TicketRegistry ticketRegistry;

    @Override
    public @Nullable Ticket getTicket(final String ticketId, final Class<? extends Ticket> clazz) {
        try {
            val ticket = ticketRegistry.getTicket(ticketId, clazz);
            if (ticket == null || ticket.isExpired()) {
                LOGGER.debug("Redis ticket not found or expired: {}", ticketId);
                return null;
            }
            return ticket;
        } catch (final Exception e) {
            LOGGER.error("Failed to retrieve ticket from Redis: {}", ticketId, e);
            return null;
        }
    }

    @Override
    public Ticket addTicket(final Ticket ticket) throws Exception {
        ticketRegistry.addTicket(ticket);
        LOGGER.debug("Added ticket to Redis: {}", ticket.getId());
        return ticket;
    }

    @Override
    public boolean deleteTicket(final String ticketId) {
        try {
            val count = ticketRegistry.deleteTicket(ticketId);
            return count > 0;
        } catch (final Exception e) {
            LOGGER.error("Failed to delete ticket from Redis: {}", ticketId, e);
            return false;
        }
    }

    @Override
    public long deleteAll() {
        return ticketRegistry.deleteAll();
    }

    @Override
    public long countTickets() {
        return ticketRegistry.countTickets();
    }
}