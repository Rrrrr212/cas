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
public class OAuth2ProtocolStrategy implements AuthenticationProtocolStrategy {

    private final AuthenticationCloudConfigProperties.OAuth2Config oauth2Config;

    private final PrincipalFactory principalFactory;

    @Override
    public boolean validateToken(final String token, final Service service) throws Throwable {
        LOGGER.debug("Validating OAuth2 token via introspection endpoint [{}]", oauth2Config.getIntrospectionUrl());
        val httpClient = java.net.http.HttpClient.newHttpClient();
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(oauth2Config.getIntrospectionUrl()))
            .header("Authorization", "Bearer " + oauth2Config.getClientSecret())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("token=" + token))
            .build();
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 && response.body().contains("\"active\":true");
    }

    @Override
    public Principal resolvePrincipal(final String token, final Service service) throws Throwable {
        LOGGER.debug("Resolving OAuth2 principal from token");
        val attributes = resolveAttributes(token, service);
        val principalId = attributes.getOrDefault("sub", List.of("unknown")).getFirst().toString();
        return principalFactory.createPrincipal(principalId, attributes);
    }

    @Override
    public Map<String, List<Object>> resolveAttributes(final String token, final Service service) throws Throwable {
        val attributes = new LinkedHashMap<String, List<Object>>();
        val httpClient = java.net.http.HttpClient.newHttpClient();
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(oauth2Config.getUserInfoUrl()))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            val body = response.body();
            attributes.put("sub", List.of(extractJsonValue(body, "sub")));
            attributes.put("email", List.of(extractJsonValue(body, "email")));
            attributes.put("name", List.of(extractJsonValue(body, "name")));
        }
        return attributes;
    }

    @Override
    public String getProtocolName() {
        return "OAUTH2";
    }

    private static String extractJsonValue(final String json, final String key) {
        val pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        val matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}