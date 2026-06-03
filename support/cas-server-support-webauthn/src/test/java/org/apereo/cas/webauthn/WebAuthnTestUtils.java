package org.apereo.cas.webauthn;

import module java.base;
import com.yubico.core.WebAuthnServer;
import com.yubico.data.CredentialRegistration;
import com.yubico.data.RegistrationRequest;
import com.yubico.data.RegistrationResponse;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.TokenBindingStatus;
import com.yubico.webauthn.data.UserIdentity;
import lombok.experimental.UtilityClass;
import lombok.val;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class WebAuthnTestUtils {
    private static final String EXAMPLE_ATTESTATION = "a368617574684461746159012c49960de5880e8c687434170f6476605b8fe4aeb9a28632c7"
        + "995cf3ba831d976341000000000000000000000000000000000000000000a20008dce8bdc"
        + "3fc2c734a29a20ddb6509bceb721d7381859ab2548ae350fdb1962df68f1ebc08dbb5263c653b4"
        + "e855b45b7df85b4926ed4572f2af78da28028143d6a6de8c0afcc6c6fbb648ce0bac022ba0a2"
        + "303d2fced0d9772fcc0d32e281c8563082820e9bfd2e76241637ccbc36aebd85f398f6b6863d3d6755e3"
        + "98e05faf101e467c201219a83b2bf4269efc6e82f2c95dbfbc2a979ea2b78dea9b9fe467a2fa36361"
        + "6c6765455332353661785820c5df3292ce78ea68322b36073fd3b012a35cc9352cba7abd5ed2c287f6"
        + "112b5361795820a83b6a518319bee86dccd1c8d54b3acb4f590e2cf7d26616aad3e7aa49fc8b4c6366"
        + "6d74686669646f2d7532666761747453746d74a26378356381590136308201323081d9a0030201020"
        + "20500a5427a1d300a06082a8648ce3d0403023021311f301d0603550403131646697265666f782055"
        + "324620536f667420546f6b656e301e170d3137303833303134353130365a170d31373039303131343531"
        + "30365a3021311f301d0603550403131646697265666f782055324620536f667420546f6b656e30593013"
        + "06072a8648ce3d020106082a8648ce3d0301070342000409b9c8303e3a9f1cc0c4bb83c6d56a223699"
        + "137387ad27dd01ad9c8e0c80addce10e52e622197576f756e38d5965bf98d53ece5af4b0ec003ad08f932"
        + "bd84c1e300a06082a8648ce3d040302034800304502210083239a57e0fa99224b2c7989998cf833d5c1562"
        + "df38d285d46cab1d6cf46ae9e02204cfd5deb11de1fdafc4e899f8d03388164beaff2e4263a82210cc"
        + "c38906981236373696758463044022049c439848ec81672461cc0ea629f297cc7228450a6b0d0887"
        + "2ab969364ec6a6202200ea1acec627fd0e616d23da3e8bfa38a5527f2007cfe3fed63e5f3e2f7e25b11";

    public static ByteArray byteArray(final String value) {
        return new ByteArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public static CredentialRegistration credentialRegistration(final String username) {
        return CredentialRegistration.builder()
            .registrationTime(Instant.now(Clock.systemUTC()))
            .credential(RegisteredCredential.builder()
                .credentialId(byteArray(username + "-credential"))
                .userHandle(byteArray(username + "-handle"))
                .publicKeyCose(byteArray(username + "-public-key"))
                .build())
            .userIdentity(UserIdentity.builder()
                .name(username)
                .displayName("CAS")
                .id(byteArray(username + "-user"))
                .build())
            .build();
    }

    public static RegistrationRequest registrationRequest(final String username) {
        val userIdentity = UserIdentity.builder()
            .name(username)
            .displayName("CAS")
            .id(byteArray(username + "-user-" + UUID.randomUUID()))
            .build();
        val creationOptions = PublicKeyCredentialCreationOptions.builder()
            .rp(new RelyingPartyIdentity.RelyingPartyIdentityBuilder.MandatoryStages()
                .id("localhost")
                .name("CAS")
                .build())
            .user(userIdentity)
            .challenge(byteArray("challenge-" + UUID.randomUUID()))
            .pubKeyCredParams(List.of())
            .build();
        return new RegistrationRequest(
            username,
            Optional.of("device"),
            byteArray("request-" + UUID.randomUUID()),
            creationOptions,
            Optional.of(byteArray("session-" + UUID.randomUUID()))
        );
    }

    public static RegistrationResponse registrationResponse(final ByteArray requestId) {
        val tokenBindingStatus = TokenBindingStatus.PRESENT;
        val tokenBindingId = byteArray("binding-token");
        val challenge = byteArray("registration-challenge");
        val clientJson = '{'
            + "\"authenticatorExtensions\":{\"device\":\"cas\"},"
            + "\"challenge\":\"" + challenge.getBase64Url() + "\","
            + "\"origin\":\"https://localhost\","
            + "\"tokenBinding\":{\"status\":\"" + tokenBindingStatus.getValue() + "\",\"id\":\"" + tokenBindingId.getBase64Url() + "\"},"
            + "\"type\":\"webauthn.get\""
            + '}';
        val response = AuthenticatorAttestationResponse.builder()
            .attestationObject(ByteArray.fromHex(EXAMPLE_ATTESTATION))
            .clientDataJSON(new ByteArray(clientJson.getBytes(StandardCharsets.UTF_8)))
            .build();
        val publicKeyCredential = PublicKeyCredential.builder()
            .id(byteArray("credential-" + UUID.randomUUID()))
            .response(response)
            .clientExtensionResults(ClientRegistrationExtensionOutputs.builder().build())
            .build();
        return new RegistrationResponse(requestId,
            (PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>) publicKeyCredential,
            Optional.of(byteArray("session-token")));
    }

    public static WebAuthnServer.SuccessfulRegistrationResult successfulRegistrationResult(final RegistrationRequest request) {
        val registration = credentialRegistration(request.username());
        return new WebAuthnServer.SuccessfulRegistrationResult(
            request,
            registrationResponse(request.requestId()),
            registration,
            true,
            byteArray("session-token-" + request.username())
        );
    }
}
