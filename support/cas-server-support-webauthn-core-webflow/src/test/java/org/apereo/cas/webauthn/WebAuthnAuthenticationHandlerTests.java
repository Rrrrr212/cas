package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.config.CasCoreEnvironmentBootstrapAutoConfiguration;
import org.apereo.cas.config.CasCoreMultitenancyAutoConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.test.CasTestExtension;
import org.apereo.cas.util.MockRequestContext;
import org.apereo.cas.util.spring.DirectObjectProvider;
import org.apereo.cas.util.spring.boot.SpringBootTestAutoConfigurations;
import org.apereo.cas.web.support.WebUtils;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link WebAuthnAuthenticationHandlerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
@Tag("MFAProvider")
@SpringBootTest(classes = {
    CasCoreEnvironmentBootstrapAutoConfiguration.class,
    CasCoreMultitenancyAutoConfiguration.class
})
@EnableConfigurationProperties(CasConfigurationProperties.class)
@SpringBootTestAutoConfigurations
@ExtendWith(CasTestExtension.class)
class WebAuthnAuthenticationHandlerTests {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier(TenantExtractor.BEAN_NAME)
    private TenantExtractor tenantExtractor;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private WebAuthnAuthenticationHandler handler;

    private RegistrationStorage webAuthnCredentialRepository;

    private SessionManager sessionManager;

    @BeforeEach
    void initialize() {
        webAuthnCredentialRepository = mock(RegistrationStorage.class);
        sessionManager = mock(SessionManager.class);
        val provider = new DirectObjectProvider<>(mock(MultifactorAuthenticationProvider.class));
        handler = new WebAuthnAuthenticationHandler("webauthn",
            PrincipalFactoryUtils.newPrincipalFactory(),
            webAuthnCredentialRepository, sessionManager, 0, provider);
    }

    @Test
    void verifySupportsCredential() {
        val credential = new WebAuthnCredential("token");
        assertTrue(handler.supports(credential));
        assertTrue(handler.supports(WebAuthnCredential.class));
    }

    @Test
    void verifyAuthenticationWithValidCredential() throws Throwable {
        val credential = new WebAuthnCredential("token");
        val service = mock(Service.class);

        val context = MockRequestContext.create(applicationContext).setClientInfo();
        WebUtils.putAuthentication(RegisteredServiceTestUtils.getAuthentication("casuser"), context);

        when(webAuthnCredentialRepository.getCredentialIdsForUsername("casuser"))
            .thenReturn(List.of("credential1"));

        val result = handler.authenticate(credential, service);
        assertNotNull(result);
        assertEquals("casuser", result.getPrincipal().getId());
        verify(webAuthnCredentialRepository).getCredentialIdsForUsername("casuser");
    }

    @Test
    void verifyAuthenticationWithNoCredentials() {
        val credential = new WebAuthnCredential("token");
        val service = mock(Service.class);

        val context = MockRequestContext.create(applicationContext).setClientInfo();
        WebUtils.putAuthentication(RegisteredServiceTestUtils.getAuthentication("casuser"), context);

        when(webAuthnCredentialRepository.getCredentialIdsForUsername("casuser"))
            .thenReturn(List.of());

        assertThrows(Exception.class, () -> handler.authenticate(credential, service));
    }

    @Test
    void verifyAuthenticationWithMissingAuthentication() {
        val credential = new WebAuthnCredential("token");
        val service = mock(Service.class);

        val context = MockRequestContext.create(applicationContext).setClientInfo();

        assertThrows(Exception.class, () -> handler.authenticate(credential, service));
    }
}
