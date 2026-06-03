package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AuthenticationHolder;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("MFAProvider")
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class WebAuthnAuthenticationHandlerTests {
    @Mock
    private RegistrationStorage webAuthnCredentialRepository;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProvider;

    @Mock
    private WebAuthnCredential webAuthnCredential;

    private WebAuthnAuthenticationHandler handler;

    @BeforeEach
    void initialize() {
        handler = spy(new WebAuthnAuthenticationHandler("WebAuthnAuthenticationHandler",
            PrincipalFactoryUtils.newPrincipalFactory(),
            webAuthnCredentialRepository,
            sessionManager,
            0,
            multifactorAuthenticationProvider));
    }

    @AfterEach
    void cleanup() {
        AuthenticationHolder.clear();
    }

    @Test
    void verifyAuthenticateReturnsHandlerExecutionResultForValidCredential() throws Throwable {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        AuthenticationHolder.setCurrentAuthentication(authentication);
        when(webAuthnCredentialRepository.getCredentialIdsForUsername("casuser"))
            .thenReturn(Set.of(WebAuthnTestUtils.byteArray("registered-credential")));

        val result = handler.authenticate(webAuthnCredential, mock(Service.class));

        assertNotNull(result);
        assertEquals("casuser", result.getPrincipal().getId());
        assertSame(webAuthnCredential, result.getCredential());
    }

    @Test
    void verifyAuthenticateFailsForInvalidCredential() {
        doReturn(false).when(handler).preAuthenticate(webAuthnCredential);

        assertThrows(FailedLoginException.class, () -> handler.authenticate(webAuthnCredential, mock(Service.class)));
        verifyNoInteractions(webAuthnCredentialRepository);
    }

    @Test
    void verifyAuthenticateFailsWhenCredentialRegistrationDoesNotExist() {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        AuthenticationHolder.setCurrentAuthentication(authentication);
        when(webAuthnCredentialRepository.getCredentialIdsForUsername("casuser")).thenReturn(Set.of());

        assertThrows(AccountNotFoundException.class, () -> handler.authenticate(webAuthnCredential, mock(Service.class)));
    }
}
