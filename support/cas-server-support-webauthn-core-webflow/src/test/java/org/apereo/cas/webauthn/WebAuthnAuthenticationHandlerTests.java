package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AccountNotFoundException;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.util.MockRequestContext;
import org.apereo.cas.web.support.WebUtils;
import com.yubico.core.SessionManager;
import com.yubico.data.CredentialRegistration;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("MFAProvider")
class WebAuthnAuthenticationHandlerTests {

    private WebAuthnAuthenticationHandler handler;

    private com.yubico.core.RegistrationStorage registrationStorage;

    @BeforeEach
    void setUp() {
        registrationStorage = mock(com.yubico.core.RegistrationStorage.class);
        val sessionManager = mock(SessionManager.class);
        val provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(mock(MultifactorAuthenticationProvider.class));

        handler = new WebAuthnAuthenticationHandler(
            "WebAuthnAuthenticationHandler",
            PrincipalFactoryUtils.newPrincipalFactory(),
            registrationStorage,
            sessionManager,
            0,
            provider);
    }

    @Test
    void verifySupportsCredential() {
        val credential = new WebAuthnCredential("test-token");
        assertTrue(handler.supports(credential));
    }

    @Test
    void verifySupportsClass() {
        assertTrue(handler.supports(WebAuthnCredential.class));
    }

    @Test
    void verifyDoesNotSupportOtherCredential() {
        val credential = mock(org.apereo.cas.authentication.Credential.class);
        assertFalse(handler.supports(credential));
    }

    @Test
    void verifyAuthenticateWithValidCredential() throws Throwable {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val credentialId = ByteArray.fromBase64Url("test-credential-id");
        when(registrationStorage.getCredentialIdsForUsername("casuser"))
            .thenReturn(Set.of(credentialId));

        val credential = new WebAuthnCredential("test-token");
        val result = handler.authenticate(credential, mock(Service.class));

        assertNotNull(result);
        assertEquals("casuser", result.getPrincipal().getId());
        assertEquals("WebAuthnAuthenticationHandler", result.getHandlerName());
    }

    @Test
    void verifyAuthenticateWithNoCredentials() {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        when(registrationStorage.getCredentialIdsForUsername("casuser"))
            .thenReturn(Set.of());

        val credential = new WebAuthnCredential("test-token");
        assertThrows(AccountNotFoundException.class,
            () -> handler.authenticate(credential, mock(Service.class)));
    }

    @Test
    void verifyAuthenticateWithEmptyCredentialsList() {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        when(registrationStorage.getCredentialIdsForUsername("casuser"))
            .thenReturn(List.of());

        val credential = new WebAuthnCredential("test-token");
        assertThrows(AccountNotFoundException.class,
            () -> handler.authenticate(credential, mock(Service.class)));
    }

    @Test
    void verifyAuthenticateWithMultipleCredentials() throws Throwable {
        val authentication = RegisteredServiceTestUtils.getAuthentication("casuser");
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val cred1 = ByteArray.fromBase64Url("cred-1");
        val cred2 = ByteArray.fromBase64Url("cred-2");
        when(registrationStorage.getCredentialIdsForUsername("casuser"))
            .thenReturn(Set.of(cred1, cred2));

        val credential = new WebAuthnCredential("test-token");
        val result = handler.authenticate(credential, mock(Service.class));

        assertNotNull(result);
        assertEquals("casuser", result.getPrincipal().getId());
    }

    @Test
    void verifyAuthenticateWithNoInProgressAuthentication() {
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(null, context);

        val credential = new WebAuthnCredential("test-token");
        assertThrows(NullPointerException.class,
            () -> handler.authenticate(credential, mock(Service.class)));
    }

    @Test
    void verifyGetWebAuthnCredentialRepository() {
        assertEquals(registrationStorage, handler.getWebAuthnCredentialRepository());
    }

    @Test
    void verifyRegistrationStorageOperations() {
        val username = "casuser";
        val credentialId = ByteArray.fromBase64Url("test-cred");
        val credential = RegisteredCredential.builder()
            .credentialId(credentialId)
            .userHandle(ByteArray.fromBase64Url("user-handle"))
            .publicKeyCose(ByteArray.fromBase64Url("public-key"))
            .build();
        val userIdentity = UserIdentity.builder()
            .name(username)
            .displayName("CAS User")
            .id(ByteArray.fromBase64Url("user-id"))
            .build();
        val reg = CredentialRegistration.builder()
            .registrationTime(java.time.Instant.now(Clock.systemUTC()))
            .credential(credential)
            .userIdentity(userIdentity)
            .build();

        when(registrationStorage.addRegistrationByUsername(username, reg)).thenReturn(true);
        when(registrationStorage.getRegistrationsByUsername(username)).thenReturn(List.of(reg));
        when(registrationStorage.userExists(username)).thenCallRealMethod();

        assertTrue(registrationStorage.addRegistrationByUsername(username, reg));
        assertFalse(registrationStorage.getRegistrationsByUsername(username).isEmpty());
        assertTrue(registrationStorage.userExists(username));
    }
}
