package org.apereo.cas.unifiedgateway;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.util.function.FunctionUtils;
import org.springframework.context.ApplicationContext;

/**
 * Protocol router to automatically detect and route to appropriate protocol handler.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
public class ProtocolRouter {

    private final ApplicationContext applicationContext;

    public ProtocolRouter(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public AutoProtocol.ProtocolType detectProtocol(final HttpServletRequest request) {
        val requestUri = request.getRequestURI();
        val queryString = request.getQueryString();

        if (containsProtocolIndicator(requestUri, queryString, "oauth", "client_id", "response_type")) {
            return AutoProtocol.ProtocolType.OAUTH2;
        }
        if (containsProtocolIndicator(requestUri, queryString, "saml", "SAMLRequest", "entityID")) {
            return AutoProtocol.ProtocolType.SAML2;
        }

        LOGGER.debug("No specific protocol detected, defaulting to OAuth2");
        return AutoProtocol.ProtocolType.OAUTH2;
    }

    public Object route(final HttpServletRequest request) {
        val protocol = detectProtocol(request);
        return switch (protocol) {
            case OAUTH2 -> getOAuth2Handler();
            case SAML2 -> getSaml2Handler();
        };
    }

    private Object getOAuth2Handler() {
        return FunctionUtils.doUnchecked(() -> {
            val clazz = Class.forName("org.apereo.cas.support.oauth.services.OAuth20Service");
            return applicationContext.getBeanProvider(clazz).getIfAvailable();
        });
    }

    private Object getSaml2Handler() {
        return FunctionUtils.doUnchecked(() -> {
            val clazz = Class.forName("org.apereo.cas.support.saml.services.SamlIdPService");
            return applicationContext.getBeanProvider(clazz).getIfAvailable();
        });
    }

    private boolean containsProtocolIndicator(final String uri, final String query, final String... indicators) {
        for (val indicator : indicators) {
            if ((uri != null && uri.toLowerCase().contains(indicator.toLowerCase())) ||
                (query != null && query.toLowerCase().contains(indicator.toLowerCase()))) {
                return true;
            }
        }
        return false;
    }
}
