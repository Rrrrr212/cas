package org.apereo.cas.webauthn.web;

import module java.base;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.RegistrationRequest;
import com.yubico.util.Either;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This is {@link BiometricRegistrationEndpoint}.
 *
 * @author CAS
 * @since 7.2.0
 */
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseWebAuthnController.BASE_ENDPOINT_WEBAUTHN + "/biometric")
@ResponseBody
@Tag(name = "Biometric WebAuthN")
public class BiometricRegistrationEndpoint extends BaseWebAuthnController {

    private final WebAuthnServer server;

    private final WebAuthnCredentialRepository repository;

    /**
     * Gets registration options.
     *
     * @param displayName            the display name
     * @param credentialNickname     the credential nickname
     * @param requireResidentKey     the require resident key
     * @param sessionTokenBase64     the session token base 64
     * @param authenticatedPrincipal the authenticated principal
     * @param request                the request
     * @return the registration options
     * @throws Exception the exception
     */
    @GetMapping(value = "/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get biometric registration options",
        parameters = {
            @Parameter(name = "displayName", in = ParameterIn.QUERY, required = true, description = "Display name"),
            @Parameter(name = "credentialNickname", in = ParameterIn.QUERY, required = false, description = "Credential nickname"),
            @Parameter(name = "requireResidentKey", in = ParameterIn.QUERY, required = false, description = "Require resident key"),
            @Parameter(name = "sessionToken", in = ParameterIn.QUERY, required = false, description = "Session token")
        })
    public ResponseEntity<Object> getRegistrationOptions(
        @NonNull
        @RequestParam("displayName") final String displayName,
        @RequestParam(value = "credentialNickname", required = false, defaultValue = StringUtils.EMPTY) final String credentialNickname,
        @RequestParam(value = "requireResidentKey", required = false) final boolean requireResidentKey,
        @RequestParam(value = "sessionToken", required = false, defaultValue = StringUtils.EMPTY) final String sessionTokenBase64,
        final Principal authenticatedPrincipal,
        final HttpServletRequest request) throws Exception {

        val result = server.startRegistration(
            request,
            authenticatedPrincipal.getName(),
            Optional.of(displayName),
            Optional.ofNullable(credentialNickname),
            requireResidentKey
                ? com.yubico.webauthn.data.ResidentKeyRequirement.REQUIRED
                : com.yubico.webauthn.data.ResidentKeyRequirement.DISCOURAGED,
            Optional.ofNullable(sessionTokenBase64).map(Unchecked.function(com.yubico.webauthn.data.ByteArray::fromBase64Url)));

        if (result.isRight()) {
            return ResponseEntity.ok(new BiometricRegistrationResponse(result.right().orElseThrow()));
        }
        return ResponseEntity.badRequest().body(Map.of("messages", result.left().orElseThrow()));
    }

    /**
     * Finish biometric registration.
     *
     * @param responseJson the response json
     * @param request      the request
     * @return the response entity
     * @throws Exception the exception
     */
    @PostMapping(value = "/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Finish biometric registration",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Registration response JSON payload",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE
            )))
    public ResponseEntity<Object> finishBiometricRegistration(
        final HttpServletRequest request,
        @RequestBody final String responseJson) throws Exception {

        val result = server.finishRegistration(request, responseJson);
        if (result.isRight()) {
            return ResponseEntity.ok(Map.of("success", true, "registration", result.right().orElseThrow()));
        }
        return ResponseEntity.badRequest().body(Map.of("messages", result.left().orElseThrow()));
    }

    /**
     * Gets client JSON.
     *
     * @param request the request
     * @return the client JSON
     */
    @GetMapping(value = "/client", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get WebAuthN client JSON")
    public ResponseEntity<Object> getClientJson(final HttpServletRequest request) {
        return ResponseEntity.ok(Map.of(
            "baseUrl", getRequestBaseUrl(request),
            "registrationEndpoint", BASE_ENDPOINT_WEBAUTHN + "/biometric/registration",
            "authenticationEndpoint", BASE_ENDPOINT_WEBAUTHN + "/authenticate"
        ));
    }

    private String getRequestBaseUrl(final HttpServletRequest request) {
        val scheme = request.getScheme();
        val serverName = request.getServerName();
        val serverPort = request.getServerPort();
        val contextPath = request.getContextPath();
        if (("http".equals(scheme) && serverPort == 80) || ("https".equals(scheme) && serverPort == 443)) {
            return scheme + "://" + serverName + contextPath;
        }
        return scheme + "://" + serverName + ":" + serverPort + contextPath;
    }

    @RequiredArgsConstructor
    @Getter
    private static final class BiometricRegistrationResponse {
        private final boolean success = true;

        private final RegistrationRequest request;
    }
}
