package org.apereo.cas.monitor;

import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

/**
 * This is {@link HazelcastClusterHealthIndicator}.
 *
 * @author CAS Contributors
 * @since 7.0.0
 */
@Slf4j
public class HazelcastClusterHealthIndicator extends AbstractHealthIndicator {
    private final ObjectProvider<HazelcastInstance> hazelcastInstance;

    public HazelcastClusterHealthIndicator(final ObjectProvider<HazelcastInstance> hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        val instance = hazelcastInstance.getIfAvailable();
        if (instance == null) {
            builder.down().withDetail("error", "Hazelcast instance not available");
            return;
        }
        val cluster = instance.getCluster();
        val members = cluster.getMembers();
        val memberCount = members.size();
        
        builder.up()
            .withDetail("clusterSize", memberCount)
            .withDetail("localMember", cluster.getLocalMember())
            .withDetail("members", members);
        
        if (memberCount < 2) {
            LOGGER.warn("Hazelcast cluster size is {}, consider adding more nodes for high availability", memberCount);
        }
    }
}
