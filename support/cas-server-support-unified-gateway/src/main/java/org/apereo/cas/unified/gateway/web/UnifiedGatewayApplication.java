package org.apereo.cas.unified.gateway.web;

import module java.base;
import org.apereo.cas.configuration.support.RequiresModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Demo application for the Unified Gateway module.
 * <p>
 * This class serves as an example entry point that demonstrates how to
 * bootstrap the unified gateway alongside the standard CAS web application.
 * In production, this module is loaded as part of the CAS WAR overlay
 * via the standard {@code cas-server-support-*} module mechanism.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.apereo.cas.unified.gateway")
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public class UnifiedGatewayApplication {

    /**
     * Main entry point for standalone demonstration.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(UnifiedGatewayApplication.class, args);
    }
}
