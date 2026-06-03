package org.apereo.cas.ticket.registry;

import module java.base;
import com.hazelcast.cluster.ClusterState;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

@Slf4j
@RequiredArgsConstructor
public class HazelcastClusterHealthIndicator extends AbstractHealthIndicator {

    private static final int MIN_CLUSTER_MEMBERS = 2;

    private final HazelcastInstance hazelcastInstance;

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        val cluster = hazelcastInstance.getCluster();
        val members = cluster.getMembers();
        val memberCount = members.size();
        val clusterState = cluster.getClusterState();

        builder.withDetail("clusterState", clusterState.name());
        builder.withDetail("memberCount", memberCount);

        val memberAddresses = members.stream()
            .map(Member::getAddress)
            .map(Object::toString)
            .toList();
        builder.withDetail("members", memberAddresses);

        val partitionService = hazelcastInstance.getPartitionService();
        builder.withDetail("partitionCount", partitionService.getPartitions().size());

        if (memberCount < MIN_CLUSTER_MEMBERS) {
            builder.down().withDetail("reason",
                "Cluster member count %d is below minimum threshold %d".formatted(memberCount, MIN_CLUSTER_MEMBERS));
        } else if (clusterState != ClusterState.ACTIVE) {
            builder.down().withDetail("reason",
                "Cluster state is %s, expected ACTIVE".formatted(clusterState.name()));
        } else {
            builder.up();
        }
    }
}
