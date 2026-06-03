package org.apereo.cas.gateway.storage;

import org.apereo.cas.ticket.Ticket;
import org.jspecify.annotations.Nullable;

public interface TicketStorage {

    String BEAN_NAME = "gatewayTicketStorage";

    @Nullable Ticket getTicket(String ticketId, Class<? extends Ticket> clazz);

    Ticket addTicket(Ticket ticket) throws Exception;

    boolean deleteTicket(String ticketId);

    long deleteAll();

    long countTickets();
}