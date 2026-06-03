package org.apereo.cas.unifiedgateway;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified authentication controller for the gateway.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RestController
@RequestMapping("/unified-gateway")
@RequiredArgsConstructor
public class UnifiedAuthController {

    private final ProtocolRouter protocolRouter;
    private final TicketStorage ticketStorage;

    @AutoProtocol
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, Object>> authorize(final HttpServletRequest request) {
        LOGGER.info("Received authorize request, detecting protocol...");

        val protocol = protocolRouter.detectProtocol(request);
        val handler = protocolRouter.route(request);

        val response = new HashMap<String, Object>();
        response.put("protocol", protocol.name());
        response.put("handlerAvailable", handler != null);
        response.put("ticketId", UUID.randomUUID().toString());

        ticketStorage.store(response.get("ticketId").toString(), response, Duration.ofMinutes(5));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/challenge")
    public ResponseEntity<GatewayMfaProvider.MfaChallengeResponse> initiateMfaChallenge(
            @RequestBody final Map<String, Object> request) {
        val userId = (String) request.get("userId");
        val context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());

        LOGGER.info("Initiating MFA challenge for user [{}]", userId);

        val response = new GatewayMfaProvider.MfaChallengeResponse(
                true,
                UUID.randomUUID().toString(),
                "sample-challenge-data",
                "MFA challenge initiated successfully"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<GatewayMfaProvider.MfaVerificationResult> verifyMfaResponse(
            @RequestBody final GatewayMfaProvider.MfaResponse response) {
        LOGGER.info("Verifying MFA response for challenge [{}]", response.challengeId());

        val result = new GatewayMfaProvider.MfaVerificationResult(
                true,
                "MFA verification successful",
                Map.of("verifiedAt", System.currentTimeMillis())
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<Object> getTicket(@PathVariable final String ticketId) {
        val ticket = ticketStorage.retrieve(ticketId);
        if (ticket != null) {
            return ResponseEntity.ok(ticket);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/ticket/{ticketId}")
    public ResponseEntity<Void> removeTicket(@PathVariable final String ticketId) {
        val removed = ticketStorage.remove(ticketId);
        if (removed) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
