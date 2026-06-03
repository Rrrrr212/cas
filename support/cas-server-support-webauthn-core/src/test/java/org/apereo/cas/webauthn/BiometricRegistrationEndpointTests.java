package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.util.RandomUtils;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import org.apereo.cas.webauthn.web.BiometricRegistrationEndpoint;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.RegistrationRequest;
import com.yubico.util.Either;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("MFAProvider")
class BiometricRegistrationEndpointTests {

    @Test
    void verifyStartRegistration() throws Throwable {
        val server = mock(WebAuthnServer.class);
        val repository = mock(WebAuthnCredentialRepository.class);
        val endpoint = new BiometricRegistrationEndpoint(server, repository);

        val credentialId = ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8));
        when(repository.getCredentialIdsForUsername("casuser")).thenReturn(Set.of(PublicKeyCredentialDescriptor.builder().id(credentialId).build()));

        val publicKeyCredential = PublicKeyCredentialCreationOptions.builder()
            .rp(new RelyingPartyIdentity.RelyingPartyIdentityBuilder.MandatoryStages()
                .id(RandomUtils.randomAlphabetic(8))
                .name(RandomUtils.randomAlphabetic(8))
                .build())
            .user(new UserIdentity.UserIdentityBuilder.MandatoryStages()
                .name("casuser")
                .displayName("CAS User")
                .id(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)))
                .build())
            .challenge(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)))
            .pubKeyCredParams(List.of())
            .build();
        val registrationRequest = new RegistrationRequest("casuser", Optional.of("nick"),
            ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)),
            publicKeyCredential,
            Optional.of(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8))));

        when(server.startRegistration(any(), anyString(), any(), any(), any(), any())).thenReturn(Either.right(registrationRequest));

        val didDocument = """
            {
              "id": "did:example:casuser",
              "verificationMethod": [
                {
                  "id": "did:example:casuser#passkey-1",
                  "credentialId": "%s"
                }
              ]
            }
            """.formatted(credentialId.getBase64Url());

        val result = endpoint.startRegistration("CAS User", "nick", true, "",
            didDocument, new TestingAuthenticationToken("casuser", List.of()), new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        val body = result.getBody().toString();
        assertTrue(body.contains(credentialId.getBase64Url()));
        assertTrue(body.contains("did:example:casuser#passkey-1"));
        assertTrue(body.contains("/webauthn/register/finish"));

        when(server.startRegistration(any(), anyString(), any(), any(), any(), any())).thenReturn(Either.left("failed"));
        val failure = endpoint.startRegistration("CAS User", "nick", true, "",
            didDocument, new TestingAuthenticationToken("casuser", List.of()), new MockHttpServletRequest());
        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
    }
}
