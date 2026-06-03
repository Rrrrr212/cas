package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationCloudConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor
public class SamlProtocolStrategy implements AuthenticationProtocolStrategy {

    private final AuthenticationCloudConfigProperties.SamlConfig samlConfig;

    private final PrincipalFactory principalFactory;

    @Override
    public boolean validateToken(final String token, final Service service) throws Throwable {
        LOGGER.debug("Validating SAML assertion at [{}]", samlConfig.getValidationUrl());
        val httpClient = java.net.http.HttpClient.newHttpClient();
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(samlConfig.getValidationUrl()))
            .header("Content-Type", "application/xml")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(token))
            .build();
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 && response.body().contains("saml:AuthnStatement");
    }

    @Override
    public Principal resolvePrincipal(final String token, final Service service) throws Throwable {
        LOGGER.debug("Resolving SAML principal from assertion");
        val attributes = resolveAttributes(token, service);
        val principalId = attributes.getOrDefault("NameID", List.of("unknown")).getFirst().toString();
        return principalFactory.createPrincipal(principalId, attributes);
    }

    @Override
    public Map<String, List<Object>> resolveAttributes(final String token, final Service service) throws Throwable {
        val attributes = new LinkedHashMap<String, List<Object>>();
        attributes.put("NameID", List.of(extractXmlValue(token, "NameID")));
        attributes.put("EmailAddress", List.of(extractXmlValue(token, "EmailAddress")));
        attributes.put("DisplayName", List.of(extractXmlValue(token, "DisplayName")));
        attributes.put("SessionIndex", List.of(extractXmlValue(token, "SessionIndex")));
        return attributes;
    }

    @Override
    public String getProtocolName() {
        return "SAML";
    }

    private static String extractXmlValue(final String xml, final String elementName) {
        val startTag = "<" + elementName + ">";
        val endTag = "</" + elementName + ">";
        val start = xml.indexOf(startTag);
        val end = xml.indexOf(endTag);
        if (start != -1 && end != -1) {
            return xml.substring(start + startTag.length(), end);
        }
        val attrPattern = elementName + "=\"([^\"]*)\"";
        val matcher = java.util.regex.Pattern.compile(attrPattern).matcher(xml);
        return matcher.find() ? matcher.group(1) : "";
    }
}