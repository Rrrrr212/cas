package org.apereo.cas.webauthn.web;

import module java.base;
import org.apereo.cas.mfa.DecentralizedIdCredential;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.RegistrationRequest;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController("biometricRegistrationEndpoint")
@RequiredArgsConstructor
@RequestMapping(BaseWebAuthnController.BASE_ENDPOINT_WEBAUTHN)
@ResponseBody
@Tag(name = "WebAuthN")
public class BiometricRegistrationEndpoint extends BaseWebAuthnController {
    public static final String WEBAUTHN_ENDPOINT_BIOMETRIC_REGISTER = "/biometric/register";

    private final WebAuthnServer server;

    private final WebAuthnCredentialRepository webAuthnCredentialRepository;

    private static ResponseEntity<Object> startResponse(final Object request) throws Exception {
        val json = writeJson(request);
        LOGGER.trace("Start: [{}]", json);
        return ResponseEntity.ok(json);
    }

    private static ResponseEntity<Object> messagesJson(final ResponseEntity.BodyBuilder response, final String message) {
        return response.body(Map.of("messages", List.of(message)));
    }

    @PostMapping(value = WEBAUTHN_ENDPOINT_BIOMETRIC_REGISTER, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Start biometric WebAuthn registration",
        parameters = {
            @Parameter(name = "displayName", in = ParameterIn.QUERY, required = true, description = "Display name"),
            @Parameter(name = "credentialNickname", in = ParameterIn.QUERY, required = false, description = "Credential nickname"),
            @Parameter(name = "requireResidentKey", in = ParameterIn.QUERY, required = false, description = "Require resident key"),
            @Parameter(name = "sessionToken", in = ParameterIn.QUERY, required = false, description = "Session token"),
            @Parameter(name = "didDocument", in = ParameterIn.QUERY, required = false, description = "DID document JSON")
        })
    public ResponseEntity<Object> startRegistration(
        @NonNull
        @RequestParam("displayName") final String displayName,
        @RequestParam(value = "credentialNickname", required = false, defaultValue = StringUtils.EMPTY) final String credentialNickname,
        @RequestParam(value = "requireResidentKey", required = false) final boolean requireResidentKey,
        @RequestParam(value = "sessionToken", required = false, defaultValue = StringUtils.EMPTY) final String sessionTokenBase64,
        @RequestParam(value = "didDocument", required = false, defaultValue = StringUtils.EMPTY) final String didDocument,
        final Principal authenticatedPrincipal,
        final HttpServletRequest request)
        throws Exception {

        val result = server.startRegistration(
            request,
            authenticatedPrincipal.getName(),
            Optional.of(displayName),
            Optional.ofNullable(credentialNickname).filter(StringUtils::isNotBlank),
            requireResidentKey
                ? ResidentKeyRequirement.REQUIRED
                : ResidentKeyRequirement.DISCOURAGED,
            Optional.ofNullable(sessionTokenBase64)
                .filter(StringUtils::isNotBlank)
                .map(Unchecked.function(ByteArray::fromBase64Url)));

        if (result.isRight()) {
            val username = authenticatedPrincipal.getName();
            val existingCredentialIds = webAuthnCredentialRepository.getCredentialIdsForUsername(username).stream()
                .map(descriptor -> descriptor.getId().getBase64Url())
                .collect(Collectors.toCollection(LinkedHashSet::new));
            val didCredentials = DecentralizedIdCredential.parse(didDocument);
            val credentialMappings = DecentralizedIdCredential.mapCredentialIds(didDocument);
            return startResponse(new BiometricStartRegistrationResponse(username,
                result.right().orElseThrow(), existingCredentialIds, didCredentials, credentialMappings));
        }
        return messagesJson(ResponseEntity.badRequest(), result.left().orElseThrow());
    }

    @RequiredArgsConstructor
    @Getter
    @SuppressWarnings("UnusedMethod")
    private static final class BiometricStartRegistrationResponse {
        private final boolean success = true;

        private final String username;

        private final RegistrationRequest request;

        private final Collection<String> existingCredentialIds;

        private final Set<DecentralizedIdCredential> decentralizedCredentials;

        private final Map<String, Set<String>> credentialIdMappings;

        private final StartRegistrationActions actions = new StartRegistrationActions();
    }

    @Getter
    @SuppressWarnings("UnusedMethod")
    private static final class StartRegistrationActions {
        private final String finish = BASE_ENDPOINT_WEBAUTHN + WebAuthnController.WEBAUTHN_ENDPOINT_REGISTER + "/finish";
    }
}
