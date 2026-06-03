package org.apereo.cas.gateway.routing;

import org.apereo.cas.gateway.ProtocolType;
import org.apereo.cas.gateway.annotation.AutoProtocol;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class AutoProtocolRoutingHandler implements HandlerInterceptor {

    private final GatewayRoutingProperties routingProperties;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        val autoProtocol = handlerMethod.getMethodAnnotation(AutoProtocol.class);
        if (autoProtocol == null) {
            return true;
        }

        val paramName = autoProtocol.parameterName();
        val protocolParam = request.getParameter(paramName);
        val protocolType = ProtocolType.fromParameter(protocolParam);

        if (protocolType == null) {
            if (autoProtocol.fallbackToDefault()) {
                LOGGER.debug("No protocol parameter '{}' found, using default: {}",
                    paramName, autoProtocol.defaultProtocol());
                routeToEndpoint(request, response, autoProtocol.defaultProtocol());
                return false;
            }
            LOGGER.warn("No protocol parameter '{}' found and fallback disabled", paramName);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Missing required protocol parameter: " + paramName);
            return false;
        }

        LOGGER.debug("Auto-routing to protocol: {}", protocolType.getProtocolName());
        routeToEndpoint(request, response, protocolType);
        return false;
    }

    private void routeToEndpoint(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final ProtocolType protocolType) throws Exception {

        val endpoint = routingProperties.getEndpointOverrides()
            .getOrDefault(protocolType.getProtocolName(), protocolType.getDefaultEndpoint());

        val queryString = request.getQueryString();
        val targetUrl = new StringBuilder(request.getContextPath())
            .append(endpoint);
        if (queryString != null && !queryString.isBlank()) {
            targetUrl.append('?').append(queryString);
        }

        LOGGER.info("Forwarding request to: {}", targetUrl);
        request.getRequestDispatcher(targetUrl.toString()).forward(request, response);
    }
}