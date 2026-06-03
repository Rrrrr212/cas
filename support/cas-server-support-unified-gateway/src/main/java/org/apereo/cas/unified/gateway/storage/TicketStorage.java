package org.apereo.cas.unified.gateway.storage;

import module java.base;
import org.apereo.cas.configuration.support.RequiresModule;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.io.Serializable;
import java.time.Duration;

/**
 * Interface for storing and retrieving gateway session state across clustered CAS nodes.
 * <p>
 * This abstraction allows the unified gateway to persist authentication state,
 * MFA challenges, and protocol context information in a distributed manner.
 * Implementations are provided for Redis and Hazelcast.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public interface TicketStorage {

    /**
     * Store a value with the given key and time-to-live.
     *
     * @param key the storage key
     * @param value the value to store
     * @param ttl the time-to-live duration
     */
    void store(String key, Serializable value, Duration ttl);

    /**
     * Retrieve a value by key.
     *
     * @param key the storage key
     * @return the stored value, or null if not found or expired
     */
    @Nullable Serializable retrieve(String key);

    /**
     * Delete a value by key.
     *
     * @param key the storage key
     * @return true if the key was found and deleted
     */
    boolean delete(String key);

    /**
     * Check if a key exists in the storage.
     *
     * @param key the storage key
     * @return true if the key exists and is not expired
     */
    boolean contains(String key);
}
