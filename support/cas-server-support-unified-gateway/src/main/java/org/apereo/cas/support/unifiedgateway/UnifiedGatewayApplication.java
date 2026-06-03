package org.apereo.cas.support.unifiedgateway;

import module java.base;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class UnifiedGatewayApplication {
    public static void main(final String[] args) {
        SpringApplication.run(UnifiedGatewayApplication.class, args);
    }
}
