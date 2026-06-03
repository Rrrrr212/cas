package org.apereo.cas.mfa;

import module java.base;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("MFA")
class DecentralizedIdCredentialTests {

    @Test
    void verifyDidDocumentParsing() {
        val didDocument = """
            {
              "id": "did:example:casuser",
              "verificationMethod": [
                {
                  "id": "did:example:casuser#passkey-1",
                  "controller": "did:example:casuser",
                  "credentialId": "credential-one",
                  "publicKeyJwk": {
                    "kty": "EC",
                    "crv": "P-256"
                  }
                }
              ],
              "authentication": ["did:example:casuser#passkey-1"],
              "credentialIdMapping": {
                "did:example:casuser#passkey-2": ["credential-two", "credential-three"]
              }
            }
            """;

        val credentials = DecentralizedIdCredential.parse(didDocument);
        assertEquals(3, credentials.size());
        assertTrue(DecentralizedIdCredential.resolveCredentialIds(didDocument).containsAll(Set.of("credential-one", "credential-two", "credential-three")));
        assertEquals(Set.of("credential-two", "credential-three"), DecentralizedIdCredential.mapCredentialIds(didDocument).get("did:example:casuser#passkey-2"));
        assertTrue(credentials.stream().anyMatch(entry -> entry.getVerificationMethodId().equals("did:example:casuser#passkey-1")
            && entry.getCredentialId().equals("credential-one")
            && entry.getDid().equals("did:example:casuser")));
    }
}
