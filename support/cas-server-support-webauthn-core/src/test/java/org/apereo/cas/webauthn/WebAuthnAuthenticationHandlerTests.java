package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.AuthenticationHolder;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("AuthenticationHandler")
class WebAuthnAuthenticationHandlerTests extends AbstractCasMockitoTest {

    private static final String HANDLER_NAME = "WebAuthnTestHandler";

    private static final String USERNAME = "casuser";

    @Test
    void verifyAuthenticateValidCredential() throws Throwable {
        val registrationStorage = WebAuthnTestUtils.mockRegistrationStorage(USERNAME);
        val sessionManager = mock(SessionManager.class);
        val principalFactory = PrincipalFactoryUtils.newPrincipalFactory();
        val provider = mock(MultifactorAuthenticationProvider.class);
        val multifactorProvider = mock(ObjectProvider.class);
        when(multifactorProvider.getObject()).thenReturn(provider);

        val handler = new WebAuthnAuthenticationHandler(
            HANDLER_NAME, principalFactory, registrationStorage,
            sessionManager, 0, multifactorProvider);

        val principal = RegisteredServiceTestUtils.getRegisteredService(USERNAME).getPrincipal();
        val authentication = RegisteredServiceTestUtils.getAuthentication(principal);
        AuthenticationHolder.setCurrentAuthentication(authentication);

        val credential = WebAuthnTestUtils.getWebAuthnCredential();
        val result = handler.authenticate(credential, null);

        assertNotNull(result);
        assertInstanceOf(AuthenticationHandlerExecutionResult.class, result);
        assertEquals(HANDLER_NAME, result.getHandlerName());
        assertEquals(USERNAME, result.getPrincipal().getId());

        verify(registrationStorage).getCredentialIdsForUsername(USERNAME);
    }

    @Test
    void verifyAuthenticateCredentialNotFound() {
        val registrationStorage = WebAuthnTestUtils.mockEmptyRegistrationStorage();
        val sessionManager = mock(SessionManager.class);
        val principalFactory = PrincipalFactoryUtils.newPrincipalFactory();
        val provider = mock(MultifactorAuthenticationProvider.class);
        val multifactorProvider = mock(ObjectProvider.class);
        when(multifactorProvider.getObject()).thenReturn(provider);

        val handler = new WebAuthnAuthenticationHandler(
            HANDLER_NAME, principalFactory, registrationStorage,
            sessionManager, 0, multifactorProvider);

        val principal = RegisteredServiceTestUtils.getRegisteredService(USERNAME).getPrincipal();
        val authentication = RegisteredServiceTestUtils.getAuthentication(principal);
        AuthenticationHolder.setCurrentAuthentication(authentication);

        val credential = WebAuthnTestUtils.getWebAuthnCredential();

        assertThrows(AccountNotFoundException.class,
            () -> handler.authenticate(credential, null));
    }

    @Test
    void verifyAuthenticateNoAuthentication() {
        val registrationStorage = WebAuthnTestUtils.mockRegistrationStorage(USERNAME);
        val sessionManager = mock(SessionManager.class);
        val principalFactory = PrincipalFactoryUtils.newPrincipalFactory();
        val provider = mock(MultifactorAuthenticationProvider.class);
        val multifactorProvider = mock(ObjectProvider.class);
        when(multifactorProvider.getObject()).thenReturn(provider);

        val handler = new WebAuthnAuthenticationHandler(
            HANDLER_NAME, principalFactory, registrationStorage,
            sessionManager, 0, multifactorProvider);

        val credential = WebAuthnTestUtils.getWebAuthnCredential();

        assertThrows(NullPointerException.class,
            () -> handler.authenticate(credential, null));
    }
}