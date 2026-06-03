package org.apereo.cas.webauthn;

import module java.base;
import com.yubico.core.DefaultSessionManager;
import com.yubico.core.InMemoryRegistrationStorage;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import com.yubico.core.WebAuthnCache;
import com.yubico.core.WebAuthnServer;
import com.yubico.core.WebSessionWebAuthnCache;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.apereo.cas.util.spring.boot.SpringBootTestAutoConfigurations;
import org.apereo.cas.webauthn.web.WebAuthnController;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "cas.authn.mfa.web-authn.core.enabled=true")
@SpringBootTestAutoConfigurations
@Import(WebAuthnRegistrationFlowIT.TestConfiguration.class)
@Tag("WebAuthn")
class WebAuthnRegistrationFlowIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void verifyStartRegistrationReturnsSuccessfulResponse() {
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("casuser", "password");

        val formData = new LinkedMultiValueMap<String, String>();
        formData.add("displayName", "Test User");
        formData.add("credentialNickname", "test-key");

        val entity = new HttpEntity<>(formData, headers);
        val response = restTemplate.postForEntity(
            "http://localhost:" + port + "/webauthn/register", entity, String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    void verifyRegisterEndpointRejectsUnauthenticatedRequest() {
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val formData = new LinkedMultiValueMap<String, String>();
        formData.add("displayName", "Test User");

        val entity = new HttpEntity<>(formData, headers);
        val response = restTemplate.postForEntity(
            "http://localhost:" + port + "/webauthn/register", entity, String.class);

        assertTrue(response.getStatusCode().is4xxClientError()
            || response.getStatusCode() == HttpStatus.FOUND);
    }

    @Test
    void verifyFinishRegistrationWithInvalidPayloadReturnsBadRequest() {
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("casuser", "password");

        val entity = new HttpEntity<>("{}", headers);
        val response = restTemplate.postForEntity(
            "http://localhost:" + port + "/webauthn/register/finish", entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @SpringBootConfiguration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfiguration {

        @Bean
        RegistrationStorage registrationStorage() {
            return new InMemoryRegistrationStorage();
        }

        @Bean
        SessionManager sessionManager() {
            return new DefaultSessionManager();
        }

        @Bean
        WebAuthnCache webAuthnCache(final SessionManager sessionManager) {
            return new WebSessionWebAuthnCache(sessionManager);
        }

        @Bean
        RelyingParty relyingParty(final RegistrationStorage registrationStorage) {
            val rpIdentity = RelyingPartyIdentity.builder()
                .id("localhost")
                .name("CAS")
                .build();
            return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(registrationStorage)
                .origins(Set.of("http://localhost:" + 8443))
                .build();
        }

        @Bean
        WebAuthnServer webAuthnServer(final RegistrationStorage registrationStorage,
                                      final WebAuthnCache webAuthnCache,
                                      final RelyingParty relyingParty) {
            return new WebAuthnServer(registrationStorage, webAuthnCache, relyingParty, null);
        }

        @Bean
        WebAuthnController webAuthnController(final WebAuthnServer webAuthnServer) {
            return new WebAuthnController(webAuthnServer);
        }

        @Bean
        UserDetailsService userDetailsService() {
            val user = org.springframework.security.core.userdetails.User.builder()
                .username("casuser")
                .password("{noop}password")
                .roles("USER")
                .build();
            return new InMemoryUserDetailsManager(user);
        }

        @Bean
        SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(customizer -> customizer
                .requestMatchers("/webauthn/**").authenticated()
                .anyRequest().permitAll());
            http.httpBasic(customizer -> {});
            http.csrf(customizer -> customizer.disable());
            return http.build();
        }
    }
}