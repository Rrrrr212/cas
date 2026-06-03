package org.apereo.cas.monitor;

import module java.base;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@RequiredArgsConstructor
public class HazelcastClusterHealthIndicator extends AbstractHealthIndicator {
    private static final int MINIMUM_CLUSTER_MEMBERS = 2;

    private final HazelcastInstance hazelcastInstance;

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        if (!hazelcastInstance.getLifecycleService().isRunning()) {
            builder.down()
                .withDetail("clusterName", hazelcastInstance.getConfig().getClusterName())
                .withDetail("minimumMembers", MINIMUM_CLUSTER_MEMBERS)
                .withDetail("members", 0);
            return;
        }
        val cluster = hazelcastInstance.getCluster();
        val members = cluster.getMembers();
        val localMember = cluster.getLocalMember();
        val memberAddresses = members.stream().map(member -> member.getAddress().toString()).toList();
        builder.withDetail("clusterName", hazelcastInstance.getConfig().getClusterName())
            .withDetail("minimumMembers", MINIMUM_CLUSTER_MEMBERS)
            .withDetail("members", members.size())
            .withDetail("localMemberUuid", localMember.getUuid())
            .withDetail("memberAddresses", memberAddresses)
            .withDetail("clusterSafe", hazelcastInstance.getPartitionService().isClusterSafe());
        if (members.size() < MINIMUM_CLUSTER_MEMBERS) {
            builder.status(Status.OUT_OF_SERVICE);
            return;
        }
        builder.up();
    }
}
