package org.apereo.cas.unifiedgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Unified gateway application for demonstration purposes.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@SpringBootApplication
public class UnifiedGatewayApplication {

    public static void main(final String[] args) {
        SpringApplication.run(UnifiedGatewayApplication.class, args);
    }
}
