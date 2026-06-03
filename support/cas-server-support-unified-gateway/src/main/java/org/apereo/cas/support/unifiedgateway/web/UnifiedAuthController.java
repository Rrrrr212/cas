package org.apereo.cas.support.unifiedgateway.web;

import module java.base;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apereo.cas.support.unifiedgateway.mfa.GatewayMfaProviderRegistry;
import org.apereo.cas.support.unifiedgateway.protocol.AutoProtocol;
import org.apereo.cas.support.unifiedgateway.ticket.TicketStorage;
import org.apereo.cas.web.support.WebUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/unified/gateway")
@RequiredArgsConstructor
public class UnifiedAuthController {
    private final GatewayMfaProviderRegistry gatewayMfaProviderRegistry;

    private final ObjectProvider<TicketStorage> ticketStorages;

    @AutoProtocol
    @RequestMapping("/auth")
    public ModelAndView authenticate(final HttpServletRequest request,
                                     final HttpServletResponse response) {
        return WebUtils.produceErrorView(new IllegalArgumentException(
            "Unified gateway could not determine whether the request targets OAuth2 or SAML2"));
    }

    @GetMapping("/mfa/{providerId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMfaProvider(@PathVariable final String providerId) {
        return gatewayMfaProviderRegistry.findProvider(providerId)
            .map(provider -> ResponseEntity.ok(Map.<String, Object>of(
                "id", provider.getId(),
                "family", provider.getFamily(),
                "available", isAvailable(providerId))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "MFA provider is not available through the unified gateway")));
    }

    @GetMapping("/mfa")
    @ResponseBody
    public List<Map<String, Object>> getMfaProviders() {
        return gatewayMfaProviderRegistry.getProviders().stream()
            .map(provider -> Map.<String, Object>of("id", provider.getId(), "family", provider.getFamily()))
            .toList();
    }

    @PostMapping("/tickets/{ticketId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveTicket(@PathVariable final String ticketId,
                                                          @RequestParam final String payload,
                                                          @RequestParam(defaultValue = "PT5M") final String ttl,
                                                          @RequestParam(required = false) final String backend) {
        return resolveStorage(backend)
            .map(storage -> {
                storage.save(ticketId, payload, Duration.parse(ttl));
                return ResponseEntity.ok(Map.<String, Object>of("ticketId", ticketId, "backend", storage.getBackend()));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No ticket storage backend is available")));
    }

    @GetMapping("/tickets/{ticketId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable final String ticketId,
                                                         @RequestParam(required = false) final String backend) {
        return resolveStorage(backend)
            .flatMap(storage -> storage.get(ticketId)
                .map(ticket -> ResponseEntity.ok(Map.<String, Object>of(
                    "ticketId", ticketId,
                    "backend", storage.getBackend(),
                    "payload", ticket))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Ticket or backend was not found")));
    }

    private boolean isAvailable(final String providerId) {
        return gatewayMfaProviderRegistry.findProvider(providerId)
            .map(provider -> {
                try {
                    return provider.isAvailable(null);
                } catch (final Exception e) {
                    return false;
                }
            })
            .orElse(false);
    }

    private Optional<TicketStorage> resolveStorage(final String backend) {
        return ticketStorages.orderedStream()
            .filter(storage -> StringUtils.isBlank(backend) || StringUtils.equalsIgnoreCase(storage.getBackend(), backend))
            .findFirst();
    }
}
