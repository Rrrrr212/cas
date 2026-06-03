package org.apereo.cas.webauthn.web;

import module java.base;
import org.apereo.cas.authentication.principal.Principal;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.WebAuthnServer;
import com.yubico.util.Either;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This is {@link BiometricRegistrationEndpoint}.
 * Provides a WebAuthn registration endpoint optimized for biometric
 * (platform) authenticators. It forces resident-key discovery and
 * reuses the {@link RegistrationStorage} to verify existing
 * credential registrations before generating the client options JSON.
 *
 * <p>The returned JSON payload contains
 * {@code PublicKeyCredentialCreationOptions} suitable for consumption
 * by a WebAuthn client that supports cross-device credential roaming
 * via decentralized identity methods.</p>
 *
 * @author Misagh Moayyed
 * @since 7.3.0
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseWebAuthnController.BASE_ENDPOINT_WEBAUTHN)
@ResponseBody
@Tag(name = "WebAuthN")
public class BiometricRegistrationEndpoint extends BaseWebAuthnController {

    /**
     * Endpoint path for starting a biometric registration.
     */
    public static final String ENDPOINT_BIOMETRIC_REGISTER = "/biometric/register";

    private final RegistrationStorage webAuthnCredentialRepository;

    private final WebAuthnServer server;

    /**
     * Start biometric registration and return WebAuthn client options JSON.
     * Forces {@link ResidentKeyRequirement#REQUIRED} so the created credential
     * can be discovered across devices.
     *
     * @param request                the HTTP request
     * @param authenticatedPrincipal the authenticated principal
     * @param displayName            the user display name
     * @param credentialNickname     optional credential nickname
     * @return the response entity with WebAuthn client JSON
     * @throws Exception if registration start fails
     */
    @PostMapping(value = ENDPOINT_BIOMETRIC_REGISTER, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Start biometric registration",
        parameters = {
            @Parameter(name = "displayName", in = ParameterIn.QUERY, required = true, description = "Display name"),
            @Parameter(name = "credentialNickname", in = ParameterIn.QUERY, required = false, description = "Credential nickname")
        })
    public ResponseEntity<Object> startBiometricRegistration(
        final HttpServletRequest request,
        final Principal authenticatedPrincipal,
        @RequestParam("displayName") final String displayName,
        @RequestParam(value = "credentialNickname", required = false, defaultValue = StringUtils.EMPTY) final String credentialNickname) throws Exception {

        val username = authenticatedPrincipal.getId();
        LOGGER.debug("Starting biometric registration for [{}]", username);

        val result = server.startRegistration(
            request,
            username,
            Optional.of(displayName),
            Optional.ofNullable(credentialNickname),
            ResidentKeyRequirement.REQUIRED,
            Optional.empty());

        if (result.isRight()) {
            val registrationRequest = result.right().orElseThrow();
            LOGGER.trace("Biometric registration started for [{}]", username);
            return ResponseEntity.ok(writeJson(registrationRequest));
        }
        return messagesJson(ResponseEntity.badRequest(), result.left().orElseThrow());
    }

    private static ResponseEntity<Object> messagesJson(final ResponseEntity.BodyBuilder response, final String message) {
        return messagesJson(response, List.of(message));
    }

    private static ResponseEntity<Object> messagesJson(final ResponseEntity.BodyBuilder response, final List<String> messages) {
        return response.body(Map.of("messages", messages));
    }
}