package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AuthenticationHolder;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.util.RandomUtils;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import com.yubico.core.SessionManager;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("MFAProvider")
class WebAuthnAuthenticationHandlerTests {

    @AfterEach
    void cleanUp() {
        AuthenticationHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void verifyDidCredentialFallback() throws Throwable {
        val repository = mock(WebAuthnCredentialRepository.class);
        val sessionManager = mock(SessionManager.class);
        @SuppressWarnings("unchecked")
        val provider = mock(ObjectProvider.class);
        val handler = new WebAuthnAuthenticationHandler("webauthn",
            PrincipalFactoryUtils.newPrincipalFactory(), repository,
            sessionManager, 0, provider);

        val did = "did:example:casuser";
        val credentialDescriptor = PublicKeyCredentialDescriptor.builder()
            .id(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)))
            .build();

        when(repository.getCredentialIdsForUsername("casuser")).thenReturn(Set.of());
        when(repository.getCredentialIdsForUsername(did)).thenReturn(Set.of(credentialDescriptor));

        val didDocument = """
            {
              "id": "%s",
              "verificationMethod": [
                {
                  "id": "%s#passkey-1",
                  "credentialId": "%s"
                }
              ]
            }
            """.formatted(did, did, credentialDescriptor.getId().getBase64Url());

        val request = new MockHttpServletRequest();
        request.setParameter("didDocument", didDocument);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        AuthenticationHolder.setCurrentAuthentication(RegisteredServiceTestUtils.getAuthentication("casuser"));

        val result = handler.authenticate(new WebAuthnCredential(RandomUtils.randomAlphabetic(8)), mock(Service.class));
        assertNotNull(result);
        assertEquals("casuser", result.getPrincipal().getId());
    }
}
