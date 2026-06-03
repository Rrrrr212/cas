package org.apereo.cas.support.oauth.authenticator;

import module java.base;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.authenticator.Authenticator;

/**
 * Strategy that selects the access token authenticator when the request
 * contains a bearer token for API authentication.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class AccessTokenAuthenticatorStrategy implements OAuth20AuthenticatorStrategy {

    private final Authenticator authenticator;

    @Override
    public boolean supports(final CallContext callContext) {
        val context = callContext.webContext();
        val authorizationHeader = context.getRequestHeader("Authorization");
        if (authorizationHeader.isPresent() && authorizationHeader.get().startsWith("Bearer ")) {
            LOGGER.debug("Request contains Bearer token, selecting access token authenticator");
            return true;
        }
        val tokenParam = context.getRequestParameter(OAuth20Constants.ACCESS_TOKEN);
        if (tokenParam.isPresent()) {
            LOGGER.debug("Request contains access_token parameter, selecting access token authenticator");
            return true;
        }
        return false;
    }

    @Override
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    @Override
    public String getMethodName() {
        return "access_token";
    }
}
