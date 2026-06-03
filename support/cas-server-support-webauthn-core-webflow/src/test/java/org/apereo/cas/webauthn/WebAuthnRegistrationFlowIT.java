package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.config.CasCoreEnvironmentBootstrapAutoConfiguration;
import org.apereo.cas.config.CasCoreMultitenancyAutoConfiguration;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.test.CasTestExtension;
import org.apereo.cas.util.MockRequestContext;
import org.apereo.cas.util.RandomUtils;
import org.apereo.cas.util.spring.DirectObjectProvider;
import org.apereo.cas.util.spring.boot.SpringBootTestAutoConfigurations;
import org.apereo.cas.web.support.WebUtils;
import com.yubico.core.InMemoryRegistrationStorage;
import com.yubico.core.SessionManager;
import com.yubico.data.CredentialRegistration;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("MFAProvider")
@SpringBootTest(classes = {
    CasCoreEnvironmentBootstrapAutoConfiguration.class,
    CasCoreMultitenancyAutoConfiguration.class
})
@SpringBootTestAutoConfigurations
@ExtendWith(CasTestExtension.class)
class WebAuthnRegistrationFlowIT {

    private WebAuthnAuthenticationHandler handler;

    private InMemoryRegistrationStorage registrationStorage;

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        registrationStorage = new InMemoryRegistrationStorage();
        sessionManager = mock(SessionManager.class);
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
    void verifyFullRegistrationFlow() throws Throwable {
        val username = "casuser";

        val credentialId = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val userHandle = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val publicKeyCose = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(32));

        val credential = RegisteredCredential.builder()
            .credentialId(credentialId)
            .userHandle(userHandle)
            .publicKeyCose(publicKeyCose)
            .build();

        val userIdentity = UserIdentity.builder()
            .name(username)
            .displayName("CAS User")
            .id(userHandle)
            .build();

        val registration = CredentialRegistration.builder()
            .registrationTime(java.time.Instant.now(Clock.systemUTC()))
            .credential(credential)
            .userIdentity(userIdentity)
            .build();

        registrationStorage.addRegistrationByUsername(username, registration);

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val credentials = registrationStorage.getCredentialIdsForUsername(username);
        assertFalse(credentials.isEmpty());

        val webAuthnCredential = new WebAuthnCredential("registration-token");
        val result = handler.authenticate(webAuthnCredential, mock(Service.class));

        assertNotNull(result);
        assertEquals(username, result.getPrincipal().getId());
        assertEquals("WebAuthnAuthenticationHandler", result.getHandlerName());
        assertNotNull(result.getCredential());
    }

    @Test
    void verifyRegistrationFlowWithNoCredentials() {
        val username = "newuser";

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val credentials = registrationStorage.getCredentialIdsForUsername(username);
        assertTrue(credentials.isEmpty());

        val webAuthnCredential = new WebAuthnCredential("registration-token");
        assertThrows(org.apereo.cas.authentication.AccountNotFoundException.class,
            () -> handler.authenticate(webAuthnCredential, mock(Service.class)));
    }

    @Test
    void verifyRegistrationFlowWithMultipleDevices() throws Throwable {
        val username = "casuser";

        for (var i = 0; i < 3; i++) {
            val credentialId = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
            val userHandle = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
            val publicKeyCose = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(32));

            val credential = RegisteredCredential.builder()
                .credentialId(credentialId)
                .userHandle(userHandle)
                .publicKeyCose(publicKeyCose)
                .build();

            val userIdentity = UserIdentity.builder()
                .name(username)
                .displayName("CAS User")
                .id(userHandle)
                .build();

            val registration = CredentialRegistration.builder()
                .registrationTime(java.time.Instant.now(Clock.systemUTC()))
                .credential(credential)
                .userIdentity(userIdentity)
                .build();

            registrationStorage.addRegistrationByUsername(username, registration);
        }

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val credentials = registrationStorage.getCredentialIdsForUsername(username);
        assertEquals(3, credentials.size());

        val webAuthnCredential = new WebAuthnCredential("registration-token");
        val result = handler.authenticate(webAuthnCredential, mock(Service.class));

        assertNotNull(result);
        assertEquals(username, result.getPrincipal().getId());
    }

    @Test
    void verifyAuthenticationHandlerExecutionResultOnSuccess() throws Throwable {
        val username = "testuser";

        val credentialId = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val userHandle = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val publicKeyCose = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(32));

        val credential = RegisteredCredential.builder()
            .credentialId(credentialId)
            .userHandle(userHandle)
            .publicKeyCose(publicKeyCose)
            .build();

        val userIdentity = UserIdentity.builder()
            .name(username)
            .displayName("Test User")
            .id(userHandle)
            .build();

        val registration = CredentialRegistration.builder()
            .registrationTime(java.time.Instant.now(Clock.systemUTC()))
            .credential(credential)
            .userIdentity(userIdentity)
            .build();

        registrationStorage.addRegistrationByUsername(username, registration);

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val webAuthnCredential = new WebAuthnCredential("test-token");
        val result = handler.authenticate(webAuthnCredential, mock(Service.class));

        assertNotNull(result);
        assertInstanceOf(AuthenticationHandlerExecutionResult.class, result);
        assertEquals(username, result.getPrincipal().getId());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void verifyAccountNotFoundExceptionOnFailure() {
        val username = "nonexistent";

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val webAuthnCredential = new WebAuthnCredential("test-token");
        val exception = assertThrows(org.apereo.cas.authentication.AccountNotFoundException.class,
            () -> handler.authenticate(webAuthnCredential, mock(Service.class)));

        assertTrue(exception.getMessage().contains(username));
    }

    @Test
    void verifyRemoveRegistrationFlow() throws Throwable {
        val username = "casuser";

        val credentialId = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val userHandle = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(16));
        val publicKeyCose = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(32));

        val credential = RegisteredCredential.builder()
            .credentialId(credentialId)
            .userHandle(userHandle)
            .publicKeyCose(publicKeyCose)
            .build();

        val userIdentity = UserIdentity.builder()
            .name(username)
            .displayName("CAS User")
            .id(userHandle)
            .build();

        val registration = CredentialRegistration.builder()
            .registrationTime(java.time.Instant.now(Clock.systemUTC()))
            .credential(credential)
            .userIdentity(userIdentity)
            .build();

        registrationStorage.addRegistrationByUsername(username, registration);
        assertFalse(registrationStorage.getCredentialIdsForUsername(username).isEmpty());

        registrationStorage.removeRegistrationByUsername(username, registration);
        assertTrue(registrationStorage.getCredentialIdsForUsername(username).isEmpty());

        val authentication = RegisteredServiceTestUtils.getAuthentication(username);
        val context = MockRequestContext.create();
        WebUtils.putAuthentication(authentication, context);

        val webAuthnCredential = new WebAuthnCredential("test-token");
        assertThrows(org.apereo.cas.authentication.AccountNotFoundException.class,
            () -> handler.authenticate(webAuthnCredential, mock(Service.class)));
    }
}
