package org.apereo.cas.webauthn.web;

import module java.base;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.BaseCasRestActuatorEndpoint;
import org.apereo.cas.webauthn.WebAuthnUtils;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import com.yubico.data.CredentialRegistration;
import com.yubico.webauthn.data.ByteArray;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * This is {@link BiometricRegistrationEndpoint}.
 * Exposes a REST actuator endpoint that returns WebAuthn client JSON
 * for biometric registration ceremonies. Reuses the existing
 * {@link WebAuthnCredentialRepository} to look up registered
 * credentials and produce client-consumable registration data.
 *
 * @author since 7.3.0
 */
@Endpoint(id = "biometricRegistration", defaultAccess = Access.NONE)
@Slf4j
public class BiometricRegistrationEndpoint extends BaseCasRestActuatorEndpoint {

    private static final String BASE_ENDPOINT_BIOMETRIC = "/biometric";

    private final ObjectProvider<WebAuthnCredentialRepository> registrationStorage;

    public BiometricRegistrationEndpoint(final CasConfigurationProperties casProperties,
                                         final ConfigurableApplicationContext applicationContext,
                                         final ObjectProvider<WebAuthnCredentialRepository> registrationStorage) {
        super(casProperties, applicationContext);
        this.registrationStorage = registrationStorage;
    }

    /**
     * Produce WebAuthn client JSON for the given username.
     * Returns all registered credentials for the user formatted
     * as JSON suitable for WebAuthn client-side consumption.
     *
     * @param username the username to look up
     * @return response entity containing WebAuthn client JSON
     */
    @Operation(summary = "Fetch biometric registration data for username",
        parameters = @Parameter(name = "username", required = true, description = "The username to look up"))
    @GetMapping(path = BASE_ENDPOINT_BIOMETRIC + "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchRegistration(
        @PathVariable final String username) throws Exception {
        val repository = registrationStorage.getObject();
        val registrations = repository.getRegistrationsByUsername(username);
        if (registrations.isEmpty()) {
            LOGGER.debug("No biometric registrations found for [{}]", username);
            return ResponseEntity.notFound().build();
        }
        val clientJson = buildClientJson(registrations);
        LOGGER.debug("Returning biometric registration JSON for [{}] with [{}] credential(s)",
            username, registrations.size());
        return ResponseEntity.ok(clientJson);
    }

    /**
     * Produce WebAuthn client JSON for a specific credential.
     *
     * @param username     the username to look up
     * @param credentialId the credential ID in base64url encoding
     * @return response entity containing WebAuthn client JSON
     */
    @Operation(summary = "Fetch biometric registration data for a specific credential",
        parameters = {
            @Parameter(name = "username", required = true, description = "The username to look up"),
            @Parameter(name = "credentialId", required = true, description = "The credential ID in base64url")
        })
    @GetMapping(path = BASE_ENDPOINT_BIOMETRIC + "/{username}/{credentialId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchRegistrationByCredentialId(
        @PathVariable final String username,
        @PathVariable final String credentialId) throws Exception {
        val repository = registrationStorage.getObject();
        val byteArray = ByteArray.fromBase64Url(credentialId);
        val registration = repository.getRegistrationByUsernameAndCredentialId(username, byteArray);
        if (registration.isEmpty()) {
            LOGGER.debug("No biometric registration found for [{}] with credentialId [{}]", username, credentialId);
            return ResponseEntity.notFound().build();
        }
        val clientJson = buildClientJson(List.of(registration.get()));
        return ResponseEntity.ok(clientJson);
    }

    private String buildClientJson(final Collection<? extends CredentialRegistration> registrations) throws Exception {
        val mapper = WebAuthnUtils.getObjectMapper();
        val credentials = registrations.stream()
            .map(reg -> {
                val cred = reg.getCredential();
                return Map.of(
                    "credentialId", cred.getCredentialId().getBase64Url(),
                    "publicKeyCose", cred.getPublicKeyCose().getBase64Url(),
                    "signatureCount", String.valueOf(cred.getSignatureCount()),
                    "username", reg.getUsername(),
                    "nickname", reg.getCredentialNickname() != null ? reg.getCredentialNickname() : ""
                );
            })
            .toList();
        return mapper.writeValueAsString(Map.of("credentials", credentials));
    }
}
