package org.apereo.cas.support.unifiedgateway.ticket;

import module java.base;
import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HazelcastTicketStorage implements TicketStorage {
    private final HazelcastInstance hazelcastInstance;

    private final String mapName;

    @Override
    public String getBackend() {
        return "hazelcast";
    }

    @Override
    public void save(final String ticketId, final Serializable ticketState, final Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            ticketMap().put(ticketId, ticketState, ttl.toSeconds(), TimeUnit.SECONDS);
            return;
        }
        ticketMap().set(ticketId, ticketState);
    }

    @Override
    public Optional<Serializable> get(final String ticketId) {
        return Optional.ofNullable(ticketMap().get(ticketId));
    }

    @Override
    public void delete(final String ticketId) {
        ticketMap().delete(ticketId);
    }

    private IMap<String, Serializable> ticketMap() {
        return hazelcastInstance.getMap(mapName);
    }
}
