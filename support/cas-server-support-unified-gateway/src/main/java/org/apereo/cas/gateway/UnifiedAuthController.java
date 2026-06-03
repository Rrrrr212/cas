package org.apereo.cas.gateway;

import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.ticket.registry.TicketRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Unified authentication controller that automatically adapts
 * between OAuth2 and SAML2 protocols.
 *
 * @since 7.0.0
 */
@RestController
@RequestMapping("/gateway/auth")
@RequiredArgsConstructor
@Slf4j
@AutoProtocol
public class UnifiedAuthController {

    private final ServicesManager servicesManager;
    private final TicketStorage ticketStorage;
    private final List<GatewayMfaProvider> mfaProviders;

    @GetMapping("/login")
    public ResponseEntity<String> login(final HttpServletRequest request,
                                        @RequestParam(value = "client_id", required = false) final String clientId,
                                        @RequestParam(value = "entityId", required = false) final String entityId) {
        
        // Protocol auto-adaptation based on parameters
        if (clientId != null) {
            LOGGER.info("Detected OAuth2 request for client: {}", clientId);
            // Reuse OAuthRegisteredService
            return ResponseEntity.ok("Handled via OAuth2 for client: " + clientId);
        } else if (entityId != null) {
            LOGGER.info("Detected SAML2 request for entity: {}", entityId);
            // Reuse SamlRegisteredService
            return ResponseEntity.ok("Handled via SAML2 for entity: " + entityId);
        }
        
        return ResponseEntity.badRequest().body("Unknown protocol. Missing client_id or entityId.");
    }
}
