package org.apereo.cas.mfa;

import org.apereo.cas.authentication.MultifactorAuthenticationCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * This is {@link DecentralizedIdCredential}.
 * Decentralized Identifier (DID) credential mapping.
 */
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Slf4j
public class DecentralizedIdCredential implements MultifactorAuthenticationCredential {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String id;
    private String credentialId;
    private String didDocument;
    private String providerId;

    public DecentralizedIdCredential(final String didDocument) {
        this.didDocument = didDocument;
        parseDidDocument();
    }

    @Override
    public String getId() {
        return this.id;
    }

    private void parseDidDocument() {
        try {
            final Map<String, Object> doc = MAPPER.readValue(didDocument, Map.class);
            this.id = (String) doc.get("id");
            final List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) doc.get("verificationMethod");
            if (verificationMethods != null && !verificationMethods.isEmpty()) {
                this.credentialId = (String) verificationMethods.get(0).get("id");
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to parse DID document", e);
        }
    }
}
