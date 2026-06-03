package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.config.CasCoreAutoConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationAutoConfiguration;
import org.apereo.cas.config.CasCoreWebAutoConfiguration;
import org.apereo.cas.config.CasCoreWebflowAutoConfiguration;
import org.apereo.cas.config.CasRegisteredServicesTestConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.test.CasTestExtension;
import org.apereo.cas.util.spring.boot.SpringBootTestAutoConfigurations;
import org.apereo.cas.webauthn.web.WebAuthnController;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.CredentialRegistration;
import com.yubico.data.RegistrationRequest;
import com.yubico.data.RegistrationResponse;
import com.yubico.util.Either;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This is {@link WebAuthnRegistrationFlowIT}.
 *
 * @since 7.0.0
 */
@SpringBootTest(classes = WebAuthnRegistrationFlowIT.WebAuthnTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(CasTestExtension.class)
@Tag("MFAProvider")
class WebAuthnRegistrationFlowIT {

    @LocalServerPort
    private int port;

    @Autowired
    private WebAuthnController webAuthnController;

    @Autowired
    private WebAuthnServer webAuthnServer;

    @Autowired
    private RegistrationStorage registrationStorage;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Test
    void verifyStartRegistrationFlow() throws Throwable {
        val mockMvc = MockMvcBuilders.standaloneSetup(webAuthnController).build();
        
        val userIdentity = UserIdentity.builder()
            .name("casuser")
            .displayName("CAS User")
            .id(ByteArray.fromBase64Url("test-user-id"))
            .build();

        val publicKeyCredential = PublicKeyCredentialCreationOptions.builder()
            .rp(new RelyingPartyIdentity.RelyingPartyIdentityBuilder.MandatoryStages()
                .id("localhost")
                .name("CAS WebAuthn")
                .build())
            .user(userIdentity)
            .challenge(ByteArray.fromBase64Url("test-challenge"))
            .pubKeyCredParams(List.of())
            .build();

        val registrationRequest = new RegistrationRequest("casuser", Optional.empty(),
            ByteArray.fromBase64Url("test-session"),
            publicKeyCredential,
            Optional.of(ByteArray.fromBase64Url("test-request-id")));

        when(webAuthnServer.startRegistration(any(), anyString(), any(), any(), any(), any()))
            .thenReturn(Either.right(registrationRequest));

        val authenticatedPrincipal = new TestingAuthenticationToken("casuser", List.of());

        mockMvc.perform(post("/webauthn/registration/start")
                .principal(authenticatedPrincipal)
                .param("displayName", "CAS User")
                .param("nickName", "casuser")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void verifyFinishRegistrationFlow() throws Throwable {
        val mockMvc = MockMvcBuilders.standaloneSetup(webAuthnController).build();

        val userIdentity = UserIdentity.builder()
            .name("casuser")
            .displayName("CAS User")
            .id(ByteArray.fromBase64Url("test-user-id"))
            .build();

        val publicKeyCredential = PublicKeyCredentialCreationOptions.builder()
            .rp(new RelyingPartyIdentity.RelyingPartyIdentityBuilder.MandatoryStages()
                .id("localhost")
                .name("CAS WebAuthn")
                .build())
            .user(userIdentity)
            .challenge(ByteArray.fromBase64Url("test-challenge"))
            .pubKeyCredParams(List.of())
            .build();

        val registrationRequest = new RegistrationRequest("casuser", Optional.empty(),
            ByteArray.fromBase64Url("test-session"),
            publicKeyCredential,
            Optional.of(ByteArray.fromBase64Url("test-request-id")));

        val registeredCredential = RegisteredCredential.builder()
            .credentialId(ByteArray.fromBase64Url("test-credential-id"))
            .userHandle(ByteArray.fromBase64Url("test-user-id"))
            .publicKeyCose(ByteArray.fromBase64Url("test-public-key"))
            .build();

        val credentialRegistration = CredentialRegistration.builder()
            .registrationTime(Instant.now(Clock.systemUTC()))
            .credential(registeredCredential)
            .userIdentity(userIdentity)
            .build();

        val registrationResponse = new RegistrationResponse(
            ByteArray.fromBase64Url("test-response-id"),
            null,
            Optional.of(ByteArray.fromBase64Url("test-session-token")));

        val successResult = new WebAuthnServer.SuccessfulRegistrationResult(
            registrationRequest,
            registrationResponse,
            credentialRegistration,
            true,
            ByteArray.fromBase64Url("test-session-token"));

        when(webAuthnServer.finishRegistration(any(), anyString())).thenReturn(Either.right(successResult));

        mockMvc.perform(post("/webauthn/registration/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationData\":\"test-data\"}"))
            .andExpect(status().isOk());

        verify(webAuthnServer).finishRegistration(any(), anyString());
    }

    @SpringBootTestAutoConfigurations
    @ImportAutoConfiguration({
        CasCoreAutoConfiguration.class,
        CasCoreAuthenticationAutoConfiguration.class,
        CasCoreWebAutoConfiguration.class,
        CasCoreWebflowAutoConfiguration.class,
        CasRegisteredServicesTestConfiguration.class
    })
    @SpringBootConfiguration(proxyBeanMethods = false)
    public static class WebAuthnTestConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebAuthnServer webAuthnServer() {
            return mock(WebAuthnServer.class);
        }

        @Bean
        @ConditionalOnMissingBean
        public RegistrationStorage webAuthnCredentialRepository() {
            return mock(RegistrationStorage.class);
        }

        @Bean
        @ConditionalOnMissingBean
        public SessionManager webAuthnSessionManager() {
            return mock(SessionManager.class);
        }

        @Bean
        @ConditionalOnMissingBean
        public WebAuthnController webAuthnController(final WebAuthnServer webAuthnServer) {
            return new WebAuthnController(webAuthnServer);
        }
    }
}
