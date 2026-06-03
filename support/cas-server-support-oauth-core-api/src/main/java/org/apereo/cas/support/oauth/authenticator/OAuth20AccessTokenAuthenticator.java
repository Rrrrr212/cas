package org.apereo.cas.support.oauth.authenticator;

import module java.base;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20JwtAccessTokenEncoder;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.token.JwtBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This is {@link OAuth20AccessTokenAuthenticator}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class OAuth20AccessTokenAuthenticator extends AbstractOAuth20Authenticator {

    private final JwtBuilder accessTokenJwtBuilder;

    private Set<String> requiredScopes = new LinkedHashSet<>();

    public OAuth20AccessTokenAuthenticator(final TicketRegistry ticketRegistry,
                                           final JwtBuilder accessTokenJwtBuilder,
                                           final OAuth20ProfileScopeToAttributesFilter profileScopeToAttributesFilter,
                                           final ConfigurableApplicationContext applicationContext) {
        super(ticketRegistry, profileScopeToAttributesFilter, applicationContext);
        this.accessTokenJwtBuilder = accessTokenJwtBuilder;
    }

    protected String extractAccessTokenFrom(final TokenCredentials tokenCredentials) {
        return OAuth20JwtAccessTokenEncoder.toDecodableCipher(accessTokenJwtBuilder).decode(tokenCredentials.getToken());
    }

    @Override
    public Optional<Credentials> validate(final CallContext callContext, final Credentials credentials) {
        val tokenCredentials = (TokenCredentials) credentials;
        val token = extractAccessTokenFrom(tokenCredentials);
        LOGGER.trace("Received access token [{}] for authentication", token);

        val accessToken = getValidTicket(token, OAuth20AccessToken.class);
        if (accessToken == null) {
            LOGGER.error("Provided access token [{}] is either not found in the ticket registry or has expired", token);
            return Optional.empty();
        }

        if (!requiredScopes.isEmpty() && !accessToken.getScopes().containsAll(requiredScopes)) {
            LOGGER.error("Unable to authenticate access token without required scopes [{}]", requiredScopes);
            return Optional.empty();
        }

        val profile = buildUserProfile(tokenCredentials, callContext, accessToken);
        if (profile != null) {
            LOGGER.trace("Final user profile based on access token [{}] is [{}]", accessToken, profile);
            tokenCredentials.setUserProfile(profile);
            return Optional.of(tokenCredentials);
        }
        return Optional.empty();
    }

    protected CommonProfile buildUserProfile(final TokenCredentials tokenCredentials,
                                             final CallContext callContext,
                                             final OAuth20AccessToken accessToken) {
        val authentication = accessToken.getAuthentication();
        val principal = authentication.getPrincipal();
        val profile = buildProfileFromPrincipal(principal, accessToken.getClientId());
        val attributes = new HashMap<String, Object>(authentication.getAttributes());
        profile.addAttributes(attributes);
        LOGGER.trace("Built user profile based on access token [{}] is [{}]", accessToken, profile);
        return profile;
    }

    @Override
    protected OAuth20RequestParameterResolver requestParameterResolver() {
        throw new UnsupportedOperationException("Not used for access token authentication");
    }

    @Override
    protected OAuth20AccessTokenFactory accessTokenFactory() {
        throw new UnsupportedOperationException("Not used for access token authentication");
    }
}
