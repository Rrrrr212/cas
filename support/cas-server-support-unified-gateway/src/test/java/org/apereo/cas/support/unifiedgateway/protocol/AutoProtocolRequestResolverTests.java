package org.apereo.cas.support.unifiedgateway.protocol;

import module java.base;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoProtocolRequestResolverTests {
    private final AutoProtocolRequestResolver resolver = new AutoProtocolRequestResolver();

    @Test
    void shouldResolveOAuthRequest() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/unified/gateway/auth");
        request.addParameter("client_id", "client");
        request.addParameter("response_type", "code");

        assertEquals(AutoProtocolRequestResolver.GatewayProtocol.OAUTH2, resolver.resolve(request).orElseThrow());
    }

    @Test
    void shouldResolveSamlRequest() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/unified/gateway/auth");
        request.addParameter("SAMLRequest", "request");

        assertEquals(AutoProtocolRequestResolver.GatewayProtocol.SAML2, resolver.resolve(request).orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenProtocolCannotBeResolved() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/unified/gateway/auth");

        assertTrue(resolver.resolve(request).isEmpty());
    }
}
