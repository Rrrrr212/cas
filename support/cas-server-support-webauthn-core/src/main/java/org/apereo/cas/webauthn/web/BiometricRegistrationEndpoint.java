package org.apereo.cas.webauthn.web;

import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * This is {@link BiometricRegistrationEndpoint}.
 *
 * @author CAS Agent
 * @since 7.0.0
 */
@RestController("biometricRegistrationEndpoint")
@RequestMapping("/webauthn/biometric")
@RequiredArgsConstructor
@Slf4j
public class BiometricRegistrationEndpoint {

    private final ObjectProvider<WebAuthnCredentialRepository> registrationStorage;

    @Operation(summary = "Fetch biometric registration devices for username as JSON", 
               parameters = @Parameter(name = "username", required = true, description = "The username to look up"))
    @GetMapping(path = "/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<? extends com.yubico.data.CredentialRegistration> fetchRegistration(
        @PathVariable final String username) {
        return registrationStorage.getObject().getRegistrationsByUsername(username);
    }
}
