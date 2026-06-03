package org.apereo.cas.ticket.registry;

import module java.base;
import org.apereo.cas.config.CasHazelcastTicketRegistryAutoConfiguration;
import org.apereo.cas.test.CasTestExtension;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CasHazelcastTicketRegistryAutoConfiguration.class,
    properties = {
        "cas.ticket.registry.hazelcast.cluster.core.instance-name=testclusterhealth",
        "management.endpoint.health.group.cluster.include=clusterHealthIndicator"
    })
@Tag("Hazelcast")
@ExtendWith(CasTestExtension.class)
class HazelcastClusterHealthIndicatorTests {

    @Autowired
    @Qualifier("clusterHealthIndicator")
    private HealthIndicator clusterHealthIndicator;

    @Test
    void verifyClusterHealthReportsDownForSingleNode() {
        val health = clusterHealthIndicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        val details = health.getDetails();
        assertTrue(details.containsKey("memberCount"));
        assertTrue(details.containsKey("clusterState"));
        assertTrue(details.containsKey("members"));
        assertTrue(details.containsKey("partitionCount"));
        assertTrue(details.containsKey("reason"));
    }
}
