package org.apereo.cas.webauthn;

import com.yubico.core.WebAuthnServer;
import com.yubico.data.RegistrationRequest;
import com.yubico.util.Either;
import org.apereo.cas.webauthn.web.WebAuthnController;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.principal.Service;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = WebAuthnRegistrationFlowIT.WebAuthnTestConfiguration.class)
public class WebAuthnRegistrationFlowIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebAuthnServer webAuthnServer;

    @Autowired
    private WebAuthnAuthenticationHandler webAuthnAuthenticationHandler;

    @Test
    public void verifyRegistrationFlowSuccess() throws Throwable {
        when(webAuthnServer.startRegistration(any(), any(), any(), any(), any(), any()))
            .thenReturn(Either.right(mock(RegistrationRequest.class)));
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/webauthn/register?displayName=test&credentialNickname=test",
            null,
            String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());

        when(webAuthnServer.finishRegistration(any(), any()))
            .thenReturn(Either.right(null));

        ResponseEntity<String> finishResponse = restTemplate.postForEntity(
            "/webauthn/register/finish",
            "{}",
            String.class
        );
        assertEquals(HttpStatus.OK, finishResponse.getStatusCode());

        val result = webAuthnAuthenticationHandler.authenticate(WebAuthnTestUtils.getWebAuthnCredential(), mock(Service.class));
        assertNotNull(result);
        assertInstanceOf(AuthenticationHandlerExecutionResult.class, result);
    }

    @Test
    public void verifyRegistrationFlowFailure() {
        when(webAuthnServer.startRegistration(any(), any(), any(), any(), any(), any()))
            .thenThrow(new WebAuthnException("WebAuthnException", "Failed to start registration"));

        assertThrows(WebAuthnException.class, () -> {
            webAuthnServer.startRegistration(null, null, null, null, null, null);
        });
    }

    @Configuration
    static class WebAuthnTestConfiguration {
        @Bean
        public WebAuthnServer webAuthnServer() {
            return mock(WebAuthnServer.class);
        }

        @Bean
        public WebAuthnController webAuthnController(WebAuthnServer webAuthnServer) {
            return new WebAuthnController(webAuthnServer);
        }

        @Bean
        public WebAuthnAuthenticationHandler webAuthnAuthenticationHandler() throws Throwable {
            WebAuthnAuthenticationHandler handler = mock(WebAuthnAuthenticationHandler.class);
            when(handler.authenticate(any(), any())).thenReturn(mock(AuthenticationHandlerExecutionResult.class));
            return handler;
        }
    }
}
