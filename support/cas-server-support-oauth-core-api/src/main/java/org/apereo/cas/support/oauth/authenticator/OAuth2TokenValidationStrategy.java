package org.apereo.cas.support.oauth.authenticator;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.token.TokenValidationStrategy;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20JwtAccessTokenEncoder;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.token.JwtBuilder;
import org.apereo.cas.token.authentication.TokenCredential;
import org.apereo.cas.util.function.FunctionUtils;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 implementation of token validation strategy.
 */
@RequiredArgsConstructor
public class OAuth2TokenValidationStrategy implements TokenValidationStrategy<OAuth20AccessToken> {

    private final TicketRegistry ticketRegistry;
    private final JwtBuilder accessTokenJwtBuilder;

    @Override
    public boolean supports(final Credential credential) {
        return credential instanceof TokenCredential;
    }

    @Override
    public OAuth20AccessToken validate(final Credential credential) throws Throwable {
        val tokenCredential = (TokenCredential) credential;
        val token = extractAccessTokenFrom(tokenCredential);
        return FunctionUtils.doAndHandle(() -> ticketRegistry.getTicket(token, OAuth20AccessToken.class));
    }

    @Override
    public boolean isExpired(final OAuth20AccessToken token) {
        return token.isExpired();
    }

    @Override
    public boolean hasRequiredScopes(final OAuth20AccessToken token, final Set<String> requiredScopes) {
        return token.getScopes().containsAll(requiredScopes);
    }

    @Override
    public Principal extractPrincipal(final OAuth20AccessToken token) {
        return token.getAuthentication().getPrincipal();
    }

    @Override
    public Map<String, Object> extractAttributes(final OAuth20AccessToken token) {
        val authentication = token.getAuthentication();
        val attributes = new HashMap<String, Object>(authentication.getAttributes());
        attributes.put(OAuth20Constants.CLIENT_ID, token.getClientId());
        return attributes;
    }

    protected String extractAccessTokenFrom(final TokenCredential tokenCredential) {
        return OAuth20JwtAccessTokenEncoder.toDecodableCipher(accessTokenJwtBuilder).decode(tokenCredential.getId());
    }
}
