package org.apereo.cas.webauthn.web;

import module java.base;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.test.CasTestExtension;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.webauthn.WebAuthnTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.core.InMemoryRegistrationStorage;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.RegistrationRequest;
import com.yubico.util.Either;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@Tag("MFAProvider")
@ExtendWith(CasTestExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = WebAuthnRegistrationFlowIT.WebAuthnRegistrationFlowConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebAuthnRegistrationFlowIT {
    private static final String USERNAME = "casuser";

    private static final String PASSWORD = "Mellon";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationStorage registrationStorage;

    @Autowired
    private Map<String, RegistrationRequest> pendingRegistrations;

    @BeforeEach
    void cleanup() {
        pendingRegistrations.clear();
        registrationStorage.removeAllRegistrations(USERNAME);
    }

    @Test
    void verifyRegistrationFlowStoresCredential() throws Exception {
        val startResponse = startRegistration();
        assertEquals(HttpStatus.OK, startResponse.getStatusCode());

        val startBody = objectMapper.readTree(startResponse.getBody());
        assertTrue(startBody.path("success").asBoolean());
        assertEquals("/webauthn/register/finish", startBody.path("actions").path("finish").asText());
        assertTrue(registrationStorage.getRegistrationsByUsername(USERNAME).isEmpty());

        val requestId = startBody.path("request").path("requestId").asText();
        assertFalse(requestId.isBlank());

        val finishResponse = restTemplate.withBasicAuth(USERNAME, PASSWORD)
            .postForEntity(startBody.path("actions").path("finish").asText(), jsonEntity(objectMapper.writeValueAsString(Map.of("requestId", requestId))), String.class);

        assertEquals(HttpStatus.OK, finishResponse.getStatusCode());
        val finishBody = objectMapper.readTree(finishResponse.getBody());
        assertTrue(finishBody.path("success").asBoolean());
        assertEquals(USERNAME, finishBody.path("username").asText());
        assertFalse(registrationStorage.getRegistrationsByUsername(USERNAME).isEmpty());
    }

    @Test
    void verifyRegistrationFlowFailsValidationWhenRequestCannotBeResolved() throws Exception {
        val startResponse = startRegistration();
        assertEquals(HttpStatus.OK, startResponse.getStatusCode());
        assertTrue(registrationStorage.getRegistrationsByUsername(USERNAME).isEmpty());

        val finishResponse = restTemplate.withBasicAuth(USERNAME, PASSWORD)
            .exchange("/webauthn/register/finish", HttpMethod.POST,
                jsonEntity(objectMapper.writeValueAsString(Map.of("requestId", "missing-request"))), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, finishResponse.getStatusCode());
        val finishBody = objectMapper.readTree(finishResponse.getBody());
        assertEquals("Registration failed", finishBody.path("messages").get(0).asText());
        assertEquals("No such registration in progress.", finishBody.path("messages").get(1).asText());
        assertTrue(registrationStorage.getRegistrationsByUsername(USERNAME).isEmpty());
    }

    private ResponseEntity<String> startRegistration() {
        val endpoint = UriComponentsBuilder.fromPath("/webauthn/register")
            .queryParam("displayName", "CAS")
            .queryParam("credentialNickname", "device")
            .queryParam("requireResidentKey", false)
            .build()
            .toUriString();
        return restTemplate.withBasicAuth(USERNAME, PASSWORD)
            .postForEntity(endpoint, jsonEntity("{}"), String.class);
    }

    private static HttpEntity<String> jsonEntity(final String body) {
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class WebAuthnRegistrationFlowConfiguration {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public Map<String, RegistrationRequest> pendingRegistrations() {
            return new ConcurrentHashMap<>();
        }

        @Bean
        public RegistrationStorage registrationStorage() {
            return new InMemoryRegistrationStorage(new CasConfigurationProperties(), CipherExecutor.noOpOfStringToString());
        }

        @Bean
        public WebAuthnServer webAuthnServer(final Map<String, RegistrationRequest> pendingRegistrations,
                                             final RegistrationStorage registrationStorage,
                                             final ObjectMapper objectMapper) throws Exception {
            val server = mock(WebAuthnServer.class);
            doAnswer(invocation -> {
                val username = invocation.getArgument(1, String.class);
                val request = WebAuthnTestUtils.registrationRequest(username);
                pendingRegistrations.put(request.requestId().getBase64Url(), request);
                return Either.right(request);
            }).when(server).startRegistration(any(), anyString(), any(), any(), any(), any());

            doAnswer(invocation -> {
                val responseJson = invocation.getArgument(1, String.class);
                val requestId = objectMapper.readTree(responseJson).path("requestId").asText();
                val request = pendingRegistrations.remove(requestId);
                if (request == null) {
                    return Either.left(List.of("Registration failed", "No such registration in progress."));
                }
                val result = WebAuthnTestUtils.successfulRegistrationResult(request);
                registrationStorage.addRegistrationByUsername(request.username(), result.getRegistration());
                return Either.right(result);
            }).when(server).finishRegistration(any(), anyString());
            return server;
        }

        @Bean
        public WebAuthnController webAuthnController(final WebAuthnServer webAuthnServer) {
            return new WebAuthnController(webAuthnServer);
        }

        @Bean
        public SecurityFilterChain webAuthnSecurityFilterChain(final HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/webauthn/register/**").hasRole("USER")
                    .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
            return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername(USERNAME)
                .password("{noop}" + PASSWORD)
                .roles("USER")
                .build());
        }
    }
}
