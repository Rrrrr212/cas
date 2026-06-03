package org.apereo.cas.webauthn;

import module java.base;
import com.yubico.core.RegistrationStorage;
import com.yubico.data.CredentialRegistration;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;
import org.apereo.cas.util.RandomUtils;
import lombok.experimental.UtilityClass;
import lombok.val;

import static org.mockito.Mockito.*;

@UtilityClass
public class WebAuthnTestUtils {

    public static final String TEST_USERNAME = "casuser";

    public static CredentialRegistration getCredentialRegistration(final String username) {
        val id = ByteArray.fromBase64Url(username);
        return CredentialRegistration.builder()
            .registrationTime(Instant.now(Clock.systemUTC()))
            .credential(RegisteredCredential.builder()
                .credentialId(id)
                .userHandle(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)))
                .publicKeyCose(ByteArray.fromBase64Url(RandomUtils.randomAlphabetic(8)))
                .build())
            .userIdentity(UserIdentity.builder()
                .name(username)
                .displayName("CAS")
                .id(id)
                .build())
            .build();
    }

    public static WebAuthnCredential getWebAuthnCredential() {
        return new WebAuthnCredential(UUID.randomUUID().toString());
    }

    public static WebAuthnCredential getWebAuthnCredential(final String token) {
        return new WebAuthnCredential(token);
    }

    public static RegistrationStorage mockRegistrationStorage(final String username) {
        val storage = mock(RegistrationStorage.class);
        val registration = getCredentialRegistration(username);
        val credentialId = registration.getCredential().getCredentialId();
        when(storage.getCredentialIdsForUsername(username)).thenReturn(Set.of(credentialId));
        when(storage.getRegistrationsByUsername(username)).thenReturn(List.of(registration));
        when(storage.getRegistrationByUsernameAndCredentialId(username, credentialId))
            .thenReturn(Optional.of(registration));
        when(storage.getUserHandleForUsername(username))
            .thenReturn(Optional.of(registration.getUserIdentity().getId()));
        when(storage.getUsernameForUserHandle(registration.getUserIdentity().getId()))
            .thenReturn(Optional.of(username));
        when(storage.getRegistrationsByUserHandle(registration.getUserIdentity().getId()))
            .thenReturn(List.of(registration));
        when(storage.lookup(credentialId, registration.getUserIdentity().getId()))
            .thenReturn(Optional.of(registration));
        when(storage.lookupAll(registration.getUserIdentity().getId()))
            .thenReturn(Set.of(registration));
        return storage;
    }

    public static RegistrationStorage mockEmptyRegistrationStorage() {
        val storage = mock(RegistrationStorage.class);
        when(storage.getCredentialIdsForUsername(anyString())).thenReturn(Set.of());
        when(storage.getRegistrationsByUsername(anyString())).thenReturn(List.of());
        when(storage.getUserHandleForUsername(anyString())).thenReturn(Optional.empty());
        when(storage.getUsernameForUserHandle(any())).thenReturn(Optional.empty());
        when(storage.getRegistrationsByUserHandle(any())).thenReturn(List.of());
        when(storage.lookup(any(), any())).thenReturn(Optional.empty());
        when(storage.lookupAll(any())).thenReturn(Set.of());
        return storage;
    }
}