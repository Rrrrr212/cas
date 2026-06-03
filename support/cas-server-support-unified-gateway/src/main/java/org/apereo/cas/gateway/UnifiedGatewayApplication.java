package org.apereo.cas.gateway;

import org.apereo.cas.gateway.annotation.AutoProtocol;
import org.apereo.cas.gateway.mfa.GatewayMfaProvider;
import org.apereo.cas.gateway.mfa.GatewayMfaService;
import org.apereo.cas.gateway.storage.TicketStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Map;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class UnifiedGatewayApplication implements CommandLineRunner {

    private final GatewayMfaService gatewayMfaService;

    private final TicketStorage ticketStorage;

    public static void main(final String[] args) {
        new SpringApplicationBuilder(UnifiedGatewayApplication.class)
            .run(args);
    }

    @Override
    public void run(final String... args) {
        val availableProviders = gatewayMfaService.getAvailableProviders();
        LOGGER.info("Unified Gateway started with {} MFA providers:", availableProviders.size());
        availableProviders.forEach(p ->
            LOGGER.info("  - {} ({})", p.getProviderId(), p.getFriendlyName()));

        LOGGER.info("Ticket storage backend: {}", ticketStorage.getClass().getSimpleName());
        LOGGER.info("Current ticket count: {}", ticketStorage.countTickets());
    }

    @AutoProtocol(defaultProtocol = ProtocolType.OAUTH2)
    public void demoAuthorize() {
        LOGGER.info("Demo: This method has @AutoProtocol annotation");
    }
}