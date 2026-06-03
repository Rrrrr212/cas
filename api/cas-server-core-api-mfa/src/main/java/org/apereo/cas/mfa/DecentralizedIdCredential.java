package org.apereo.cas.mfa;

import module java.base;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.val;

@Value
@Builder
public class DecentralizedIdCredential implements Serializable {
    @Serial
    private static final long serialVersionUID = 7338853836353081162L;

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    String did;

    String verificationMethodId;

    String controller;

    String credentialId;

    String publicKeyJwk;

    String publicKeyMultibase;

    public static Set<DecentralizedIdCredential> parse(final String didDocument) {
        if (didDocument == null || didDocument.isBlank()) {
            return Set.of();
        }
        try {
            val root = MAPPER.readTree(didDocument);
            if (root == null || !root.isObject()) {
                return Set.of();
            }

            val did = textValue(root.get("id"));
            val credentials = new LinkedHashMap<String, DecentralizedIdCredential>();
            val verificationMethods = new LinkedHashMap<String, JsonNode>();

            collectVerificationMethods(root.get("verificationMethod"), did, credentials, verificationMethods);
            collectRelationshipCredentials(root.get("authentication"), did, credentials, verificationMethods);
            collectRelationshipCredentials(root.get("assertionMethod"), did, credentials, verificationMethods);
            collectCredentialMappings(root.get("credentialIdMapping"), did, credentials);
            return new LinkedHashSet<>(credentials.values());
        } catch (final Exception e) {
            return Set.of();
        }
    }

    public static Set<String> resolveCredentialIds(final String didDocument) {
        return parse(didDocument).stream()
            .map(DecentralizedIdCredential::getCredentialId)
            .filter(Objects::nonNull)
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Map<String, Set<String>> mapCredentialIds(final String didDocument) {
        return parse(didDocument).stream()
            .collect(Collectors.groupingBy(DecentralizedIdCredential::getVerificationMethodId,
                LinkedHashMap::new,
                Collectors.mapping(DecentralizedIdCredential::getCredentialId,
                    Collectors.toCollection(LinkedHashSet::new))));
    }

    private static void collectVerificationMethods(final JsonNode verificationMethodNode,
                                                   final String did,
                                                   final Map<String, DecentralizedIdCredential> credentials,
                                                   final Map<String, JsonNode> verificationMethods) {
        if (verificationMethodNode == null || !verificationMethodNode.isArray()) {
            return;
        }
        verificationMethodNode.forEach(node -> {
            if (node != null && node.isObject()) {
                val verificationMethodId = textValue(node.get("id"));
                if (!verificationMethodId.isBlank()) {
                    verificationMethods.putIfAbsent(verificationMethodId, node);
                }
                collectCredentialNode(node, did, credentials);
            }
        });
    }

    private static void collectRelationshipCredentials(final JsonNode relationshipNode,
                                                       final String did,
                                                       final Map<String, DecentralizedIdCredential> credentials,
                                                       final Map<String, JsonNode> verificationMethods) {
        if (relationshipNode == null) {
            return;
        }
        if (relationshipNode.isArray()) {
            relationshipNode.forEach(node -> collectRelationshipCredentials(node, did, credentials, verificationMethods));
            return;
        }
        if (relationshipNode.isTextual()) {
            val verificationMethod = verificationMethods.get(relationshipNode.asText());
            if (verificationMethod != null) {
                collectCredentialNode(verificationMethod, did, credentials);
            }
            return;
        }
        if (relationshipNode.isObject()) {
            collectCredentialNode(relationshipNode, did, credentials);
        }
    }

    private static void collectCredentialMappings(final JsonNode mappingNode,
                                                  final String did,
                                                  final Map<String, DecentralizedIdCredential> credentials) {
        if (mappingNode == null || !mappingNode.isObject()) {
            return;
        }
        mappingNode.fields().forEachRemaining(entry -> extractCredentialIds(entry.getValue()).forEach(credentialId -> {
            val key = entry.getKey() + '|' + credentialId;
            credentials.putIfAbsent(key, DecentralizedIdCredential.builder()
                .did(did)
                .verificationMethodId(entry.getKey())
                .credentialId(credentialId)
                .build());
        }));
    }

    private static void collectCredentialNode(final JsonNode node,
                                              final String did,
                                              final Map<String, DecentralizedIdCredential> credentials) {
        val credentialIds = extractCredentialIds(node);
        if (credentialIds.isEmpty()) {
            return;
        }

        val verificationMethodId = textValue(node.get("id"));
        val controller = textValue(node.get("controller"));
        val publicKeyMultibase = textValue(node.get("publicKeyMultibase"));
        val publicKeyJwk = writeValue(node.get("publicKeyJwk"));

        credentialIds.forEach(credentialId -> {
            val key = verificationMethodId + '|' + credentialId;
            credentials.putIfAbsent(key, DecentralizedIdCredential.builder()
                .did(did)
                .verificationMethodId(verificationMethodId)
                .controller(controller)
                .credentialId(credentialId)
                .publicKeyJwk(publicKeyJwk)
                .publicKeyMultibase(publicKeyMultibase)
                .build());
        });
    }

    private static Set<String> extractCredentialIds(final JsonNode node) {
        val credentialIds = new LinkedHashSet<String>();
        if (node == null || !node.isObject()) {
            return credentialIds;
        }

        addCredentialIdValue(node.get("credentialId"), credentialIds);

        val credentialIdsNode = node.get("credentialIds");
        if (credentialIdsNode != null && credentialIdsNode.isArray()) {
            credentialIdsNode.forEach(value -> addCredentialIdValue(value, credentialIds));
        }
        return credentialIds;
    }

    private static void addCredentialIdValue(final JsonNode value, final Set<String> credentialIds) {
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            credentialIds.add(value.asText());
        }
    }

    private static String textValue(final JsonNode node) {
        if (node == null || node.isNull() || !node.isValueNode()) {
            return "";
        }
        return node.asText("");
    }

    private static String writeValue(final JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (final Exception e) {
            return "";
        }
    }
}
