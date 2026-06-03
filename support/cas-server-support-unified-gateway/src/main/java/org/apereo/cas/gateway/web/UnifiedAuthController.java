package org.apereo.cas.gateway.web;

import org.apereo.cas.gateway.ProtocolType;
import org.apereo.cas.gateway.annotation.AutoProtocol;
import org.apereo.cas.gateway.mfa.GatewayMfaService;
import org.apereo.cas.gateway.storage.TicketStorage;
import org.apereo.cas.authentication.principal.SimplePrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class UnifiedAuthController {

    private final GatewayMfaService gatewayMfaService;

    private final TicketStorage ticketStorage;

    @AutoProtocol(defaultProtocol = ProtocolType.OAUTH2)
    @GetMapping("/authorize")
    public void authorize() {
    }

    @GetMapping("/mfa/providers")
    public ResponseEntity<Map<String, Object>> listMfaProviders() {
        val providers = gatewayMfaService.getAvailableProviders();
        val result = providers.stream()
            .map(p -> Map.of(
                "id", p.getProviderId(),
                "name", p.getFriendlyName()
            ))
            .toList();
        return ResponseEntity.ok(Map.of("providers", result));
    }

    @PostMapping(value = "/mfa/authenticate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> mfaAuthenticate(
        @RequestParam("providerId") final String providerId,
        @RequestParam("principal") final String principalId) {

        try {
            val provider = gatewayMfaService.findProvider(providerId);
            if (provider.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unknown MFA provider: " + providerId));
            }
            val result = provider.get().authenticate(
                new SimplePrincipal(principalId, Map.of()),
                Map.of());
            return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "provider", providerId,
                "metadata", result.metadata() != null ? result.metadata() : Map.of()
            ));
        } catch (final Exception e) {
            LOGGER.error("MFA authentication failed for provider: {}", providerId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tickets/count")
    public ResponseEntity<Map<String, Object>> ticketCount() {
        return ResponseEntity.ok(Map.of("count", ticketStorage.countTickets()));
    }
}