package org.apereo.cas.ticket.registry;

import module java.base;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

@RequiredArgsConstructor
public class HazelcastClusterHealthIndicator extends AbstractHealthIndicator {
    private final HazelcastInstance hazelcastInstance;

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        val cluster = hazelcastInstance.getCluster();
        val members = cluster.getMembers();
        val memberCount = members.size();

        val details = new HashMap<String, Object>();
        details.put("memberCount", memberCount);
        details.put("clusterName", hazelcastInstance.getConfig().getClusterName());
        details.put("instanceName", hazelcastInstance.getName());

        val memberDetails = members.stream()
            .map(member -> {
                val info = new HashMap<String, Object>();
                info.put("uuid", member.getUuid().toString());
                info.put("address", member.getAddress().toString());
                info.put("localMember", member.localMember());
                info.put("liteMember", member.isLiteMember());
                return info;
            })
            .toList();
        details.put("members", memberDetails);

        if (memberCount < 2) {
            builder.down()
                .withDetail("warning", "Cluster has fewer than 2 members; high availability is at risk");
        } else {
            builder.up();
        }
        builder.withDetails(details);
    }
}