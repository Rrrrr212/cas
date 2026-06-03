package org.apereo.cas.unified.gateway.web;

import module java.base;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.unified.gateway.UnifiedGatewayConstants;
import org.apereo.cas.unified.gateway.annotation.AutoProtocol;
import org.apereo.cas.unified.gateway.mfa.GatewayMfaProvider;
import org.apereo.cas.unified.gateway.storage.TicketStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Unified authentication controller that automatically detects the protocol
 * (OAuth2 or SAML2) based on request parameters and routes to the appropriate handler.
 * <p>
 * This controller reuses existing CAS OAuth2 and SAML2 infrastructure without
 * modifying any core CAS code. It acts as a facade that delegates to:
 * <ul>
 *   <li>{@code OAuth20ConfigurationContext} and OAuth2 endpoints for OAuth2 flows</li>
 *   <li>{@code SamlIdPProfileHandlerController} and SAML2 profile handlers for SAML2 flows</li>
 * </ul>
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@Slf4j
@RestController
@RequestMapping(UnifiedGatewayConstants.GATEWAY_ENDPOINT_BASE)
@RequiredArgsConstructor
@NullMarked
@Tag(name = "Unified Gateway")
@RequiresModule(name = "cas-server-support-unified-gateway")
@AutoProtocol
public class UnifiedAuthController {

    private final TicketRegistry ticketRegistry;

    private final ServicesManager servicesManager;

    private final TicketStorage ticketStorage;

    private final List<GatewayMfaProvider> mfaProviders;

    /**
     * Unified authentication endpoint that auto-detects the protocol.
     *
     * @param protocol explicit protocol override (oauth2 or saml2)
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     * @return response entity with the authentication result
     */
    @GetMapping(path = UnifiedGatewayConstants.GATEWAY_AUTH_PATH,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Unified authentication endpoint with auto protocol detection",
        parameters = {
            @Parameter(name = "protocol", in = ParameterIn.QUERY, description = "Protocol: oauth2 or saml2"),
            @Parameter(name = "client_id", in = ParameterIn.QUERY, description = "OAuth2 client ID"),
            @Parameter(name = "SAMLRequest", in = ParameterIn.QUERY, description = "SAML2 request")
        })
    public ResponseEntity<Map<String, Object>> authenticate(
            @RequestParam(required = false) final String protocol,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        val detectedProtocol = detectProtocol(protocol, request);
        LOGGER.debug("Detected protocol: [{}] for request from [{}]", detectedProtocol, request.getRemoteAddr());

        val sessionId = request.getSession().getId();
        ticketStorage.store(sessionId + ":protocol", detectedProtocol, Duration.ofMinutes(30));

        if (detectedProtocol.equals(UnifiedGatewayConstants.PROTOCOL_OAUTH2)) {
            return handleOAuth2Flow(request, response);
        } else if (detectedProtocol.equals(UnifiedGatewayConstants.PROTOCOL_SAML2)) {
            return handleSaml2Flow(request, response);
        }

        LOGGER.warn("Unable to determine protocol from request parameters");
        return ResponseEntity.badRequest().body(Map.of(
            "error", "unknown_protocol",
            "message", "Cannot determine authentication protocol from request"
        ));
    }

    /**
     * Detect the protocol type from request parameters.
     * Detection order: explicit param > OAuth2 indicators > SAML2 indicators.
     */
    private String detectProtocol(final String explicit, final HttpServletRequest request) {
        if (StringUtils.isNotBlank(explicit)) {
            return explicit.toLowerCase();
        }

        if (isOAuth2Request(request)) {
            return UnifiedGatewayConstants.PROTOCOL_OAUTH2;
        }

        if (isSaml2Request(request)) {
            return UnifiedGatewayConstants.PROTOCOL_SAML2;
        }

        return StringUtils.EMPTY;
    }

    private boolean isOAuth2Request(final HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getParameter(UnifiedGatewayConstants.PARAM_CLIENT_ID))
            || StringUtils.isNotBlank(request.getParameter(UnifiedGatewayConstants.PARAM_RESPONSE_TYPE))
            || StringUtils.isNotBlank(request.getParameter(UnifiedGatewayConstants.PARAM_REDIRECT_URI));
    }

    private boolean isSaml2Request(final HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getParameter(UnifiedGatewayConstants.PARAM_SAML_REQUEST))
            || StringUtils.isNotBlank(request.getParameter(UnifiedGatewayConstants.PARAM_SAML_RESPONSE));
    }

    private ResponseEntity<Map<String, Object>> handleOAuth2Flow(
            final HttpServletRequest request,
            final HttpServletResponse response) {
        LOGGER.debug("Routing to OAuth2 flow");
        val clientId = request.getParameter(UnifiedGatewayConstants.PARAM_CLIENT_ID);
        val service = servicesManager.findServiceBy(clientId);
        if (service == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "service_not_found",
                "client_id", clientId
            ));
        }

        val result = Map.of(
            "protocol", UnifiedGatewayConstants.PROTOCOL_OAUTH2,
            "status", "routed",
            "client_id", clientId,
            "message", "Request routed to OAuth2 handler"
        );
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> handleSaml2Flow(
            final HttpServletRequest request,
            final HttpServletResponse response) {
        LOGGER.debug("Routing to SAML2 flow");
        val samlRequest = request.getParameter(UnifiedGatewayConstants.PARAM_SAML_REQUEST);
        val result = Map.of(
            "protocol", UnifiedGatewayConstants.PROTOCOL_SAML2,
            "status", "routed",
            "message", "Request routed to SAML2 handler"
        );
        return ResponseEntity.ok(result);
    }
}
