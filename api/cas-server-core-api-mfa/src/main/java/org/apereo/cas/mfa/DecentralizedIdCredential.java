package org.apereo.cas.mfa;

import module java.base;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.CredentialMetadata;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * This is {@link DecentralizedIdCredential}.
 * Represents a credential backed by a Decentralized Identifier (DID)
 * and its associated DID document. The credential maps the DID to a
 * {@code credentialId} extracted from the document's verification
 * methods or identifier, enabling cross-device credential roaming
 * scenarios in WebAuthn MFA flows.
 *
 * @author Misagh Moayyed
 * @since 7.3.0
 */
@ToString
@Getter
@Setter
@EqualsAndHashCode(of = "did")
public class DecentralizedIdCredential implements Credential {

    @Serial
    private static final long serialVersionUID = -5861029922029639625L;

    private String did;

    private Map<String, Object> didDocument;

    private String credentialId;

    private CredentialMetadata credentialMetadata;

    public DecentralizedIdCredential() {
    }

    public DecentralizedIdCredential(final String did, final Map<String, Object> didDocument) {
        this.did = did;
        this.didDocument = didDocument;
        this.credentialId = resolveCredentialId(didDocument);
    }

    @Override
    public String getId() {
        return this.did != null ? this.did : UNKNOWN_ID;
    }

    @Override
    public CredentialMetadata getCredentialMetadata() {
        return this.credentialMetadata;
    }

    /**
     * Sets the DID document and re-resolves the {@code credentialId}.
     *
     * @param didDocument the DID document as a map
     */
    public void setDidDocument(final Map<String, Object> didDocument) {
        this.didDocument = didDocument;
        this.credentialId = resolveCredentialId(didDocument);
    }

    /**
     * Resolves the {@code credentialId} from a DID document by inspecting
     * the document's {@code id} field first, then falling back to the first
     * entry in the {@code verificationMethod} array.
     *
     * @param didDocument the DID document represented as a map
     * @return the resolved credential identifier, or {@code null}
     */
    public static String resolveCredentialId(final Map<String, Object> didDocument) {
        if (didDocument == null || didDocument.isEmpty()) {
            return null;
        }
        val id = didDocument.get("id");
        if (id != null) {
            return id.toString();
        }
        val verificationMethod = didDocument.get("verificationMethod");
        if (verificationMethod instanceof List<?> list && !list.isEmpty()) {
            val first = (Map<String, Object>) list.getFirst();
            val vmId = first.get("id");
            if (vmId != null) {
                return vmId.toString();
            }
        }
        return null;
    }
}