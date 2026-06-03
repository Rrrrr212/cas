package org.apereo.cas.mfa;

import module java.base;
import org.apereo.cas.authentication.credential.OneTimeTokenCredential;
import org.apereo.cas.util.function.FunctionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * This is {@link DecentralizedIdCredential}.
 * Extends the standard MFA credential model to integrate with
 * Decentralized Identity (DID) methods. Parses W3C DID documents
 * and maps verification method entries to WebAuthn credential IDs
 * for cross-device credential roaming.
 *
 * @author since 7.3.0
 */
@Slf4j
@ToString
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecentralizedIdCredential extends OneTimeTokenCredential {

    @Serial
    private static final long serialVersionUID = 8291034567890123456L;

    private static final ObjectMapper DID_MAPPER = new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String did;

    private String didMethod;

    private String didDocumentJson;

    public DecentralizedIdCredential(final String token) {
        super(token);
    }

    public DecentralizedIdCredential(final String token, final String did) {
        super(token);
        this.did = did;
        this.didMethod = parseDidMethod(did);
    }

    /**
     * Parse the DID method from a DID identifier.
     * DID format: {@code did:method:method-specific-id}
     *
     * @param did the DID identifier
     * @return the DID method, or empty string if unparseable
     */
    public static String parseDidMethod(final String did) {
        if (did == null || did.isBlank()) {
            return "";
        }
        val parts = did.split(":");
        if (parts.length >= 2 && "did".equalsIgnoreCase(parts[0])) {
            return parts[1];
        }
        return "";
    }

    /**
     * Parse the DID document JSON and extract the first
     * verification method's credential ID as a base64url string.
     * The DID document is expected to follow W3C DID Core
     * specification with a {@code verificationMethod} array
     * where each entry may contain a base64url-encoded
     * public key or credential identifier.
     *
     * @param didDocumentJson the DID document as JSON string
     * @return the credential ID extracted from the first
     *         verification method, or empty if not found
     */
    public static Optional<String> extractCredentialIdFromDidDocument(final String didDocumentJson) {
        return FunctionUtils.doAndHandle(() -> {
            val doc = DID_MAPPER.readTree(didDocumentJson);
            val verificationMethods = doc.get("verificationMethod");
            if (verificationMethods != null && verificationMethods.isArray()) {
                for (val vm : verificationMethods) {
                    val idNode = vm.get("id");
                    if (idNode != null) {
                        val id = idNode.asText();
                        LOGGER.debug("Extracted verification method id [{}] from DID document", id);
                        return Optional.of(id);
                    }
                }
            }
            LOGGER.warn("No verification method found in DID document");
            return Optional.<String>empty();
        }, e -> {
            LOGGER.error("Failed to parse DID document or extract credentialId", e);
            return Optional.<String>empty();
        }).get();
    }

    /**
     * Map the DID document's verification methods to credential IDs.
     * Returns all credential IDs found in the DID document's
     * verification method entries as base64url strings.
     *
     * @param didDocumentJson the DID document as JSON string
     * @return collection of credential IDs from the DID document
     */
    public static Collection<String> mapCredentialIdsFromDidDocument(final String didDocumentJson) {
        return FunctionUtils.doAndHandle(() -> {
            val doc = DID_MAPPER.readTree(didDocumentJson);
            val result = new ArrayList<String>();
            val verificationMethods = doc.get("verificationMethod");
            if (verificationMethods != null && verificationMethods.isArray()) {
                for (val vm : verificationMethods) {
                    val idNode = vm.get("id");
                    if (idNode != null) {
                        result.add(idNode.asText());
                    }
                }
            }
            return result;
        }, e -> {
            LOGGER.error("Failed to map credentialIds from DID document", e);
            return Collections.<String>emptyList();
        }).get();
    }

    /**
     * Resolve credential ID from this credential's DID document.
     *
     * @return the first credential ID found as base64url, or empty
     */
    public Optional<String> resolveCredentialId() {
        if (didDocumentJson == null || didDocumentJson.isBlank()) {
            return Optional.empty();
        }
        return extractCredentialIdFromDidDocument(didDocumentJson);
    }
}
