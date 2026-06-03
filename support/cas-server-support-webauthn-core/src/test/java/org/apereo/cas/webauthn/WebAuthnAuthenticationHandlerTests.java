package org.apereo.cas.webauthn;

import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.util.AbstractCasMockitoTest;
import org.apereo.cas.web.support.WebUtils;

import com.yubico.core.RegistrationStorage;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.RequestContextHolder;
import org.springframework.webflow.test.MockRequestContext;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebAuthnAuthenticationHandlerTests extends AbstractCasMockitoTest {

    @Mock
    private RegistrationStorage webAuthnCredentialRepository;

    private WebAuthnAuthenticationHandler webAuthnAuthenticationHandler;

    @BeforeEach
    public void setup() {
        webAuthnAuthenticationHandler = WebAuthnTestUtils.getWebAuthnAuthenticationHandler(webAuthnCredentialRepository);
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        RequestContextHolder.setRequestContext(context);
        WebUtils.putAuthentication(CoreAuthenticationTestUtils.getAuthentication(), context);
    }

    @Test
    public void verifyAuthenticateSuccessful() throws Throwable {
        val credential = WebAuthnTestUtils.getWebAuthnCredential();
        val principal = CoreAuthenticationTestUtils.getAuthentication().getPrincipal();
        when(webAuthnCredentialRepository.getCredentialIdsForUsername(principal.getId()))
            .thenReturn(List.of(mock(com.yubico.webauthn.data.CredentialRegistration.class)));

        val result = webAuthnAuthenticationHandler.authenticate(credential, mock(Service.class));
        assertNotNull(result);
        assertEquals(principal.getId(), result.getPrincipal().getId());
    }

    @Test
    public void verifyAuthenticateAccountNotFound() throws Throwable {
        val credential = WebAuthnTestUtils.getWebAuthnCredential();
        val principal = CoreAuthenticationTestUtils.getAuthentication().getPrincipal();
        when(webAuthnCredentialRepository.getCredentialIdsForUsername(principal.getId()))
            .thenReturn(List.of());

        assertThrows(AccountNotFoundException.class, () -> webAuthnAuthenticationHandler.authenticate(credential, mock(Service.class)));
    }

    @Test
    public void verifyAuthenticateInvalid() throws Throwable {
        val credential = WebAuthnTestUtils.getWebAuthnCredential();
        val principal = CoreAuthenticationTestUtils.getAuthentication().getPrincipal();
        when(webAuthnCredentialRepository.getCredentialIdsForUsername(principal.getId()))
            .thenThrow(new WebAuthnException("WebAuthnException", "Invalid credential"));

        assertThrows(WebAuthnException.class, () -> webAuthnAuthenticationHandler.authenticate(credential, mock(Service.class)));
    }
}
