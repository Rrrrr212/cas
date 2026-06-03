package org.apereo.cas.support.oauth.authenticator;

import module java.base;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredServiceUsernameProviderContext;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.profile.OAuth20ProfileScopeToAttributesFilter;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.ticket.OAuth20Token;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessTokenFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.function.FunctionUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.Nullable;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;

/**
 * Abstract base class for OAuth20 authenticators that provides shared
 * token validation, profile building, and principal resolution logic.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public abstract class AbstractOAuth20Authenticator implements Authenticator {

    private final ServicesManager servicesManager;

    private final TicketRegistry ticketRegistry;

    private final OAuth20ProfileScopeToAttributesFilter profileScopeToAttributesFilter;

    private final ConfigurableApplicationContext applicationContext;

    /**
     * Retrieve a ticket from the registry and verify it is not expired.
     *
     * @param token        the ticket identifier
     * @param ticketClass  the expected ticket type
     * @return the ticket instance, or null if not found or expired
     */
    protected <T extends OAuth20Token> @Nullable T getValidTicket(final String token, final Class<T> ticketClass) {
        LOGGER.trace("Looking up ticket [{}] of type [{}] in registry", token, ticketClass.getSimpleName());
        return FunctionUtils.doAndHandle(() -> {
            val ticket = ticketRegistry.getTicket(token, ticketClass);
            if (ticket == null) {
                LOGGER.error("Ticket [{}] not found in registry", token);
                return null;
            }
            if (ticket.isExpired()) {
                LOGGER.error("Ticket [{}] has expired", token);
                return null;
            }
            return ticket;
        });
    }

    /**
     * Build a CommonProfile from a resolved principal, merging principal
     * attributes and authentication attributes, and adding the CLIENT_ID.
     *
     * @param principal    the resolved principal
     * @param clientId     the OAuth client identifier
     * @return the built profile
     */
    protected CommonProfile buildProfileFromPrincipal(final Principal principal, final String clientId) {
        val profile = new CommonProfile(true);
        profile.setId(principal.getId());
        val attributes = new HashMap<String, Object>(principal.getAttributes());
        profile.addAttributes(attributes);
        profile.addAttribute(OAuth20Constants.CLIENT_ID, clientId);
        LOGGER.trace("Built profile [{}] for principal [{}] and client [{}]", profile.getId(), principal.getId(), clientId);
        return profile;
    }

    /**
     * Resolve the final principal after applying the scope-to-attributes filter.
     *
     * @param authentication    the authentication result
     * @param registeredService the OAuth registered service
     * @param service           the CAS service
     * @param callContext       the current call context
     * @return the filtered principal
     */
    protected Principal resolveFinalPrincipal(final Authentication authentication,
                                              final OAuthRegisteredService registeredService,
                                              final Service service,
                                              final CallContext callContext) throws Throwable {
        val scopes = requestParameterResolver().resolveRequestedScopes(callContext.webContext());
        val responseType = requestParameterResolver().resolveResponseType(callContext.webContext());
        val grantType = requestParameterResolver().resolveGrantType(callContext.webContext());
        val accessToken = accessTokenFactory().create(service, authentication, scopes,
            registeredService.getClientId(), responseType, grantType);
        return profileScopeToAttributesFilter.filter(service, authentication.getPrincipal(), registeredService, accessToken);
    }

    /**
     * Resolve the display username for a principal using the registered service's username provider.
     *
     * @param principal         the principal
     * @param registeredService the registered service
     * @param service           the CAS service
     * @return the resolved username
     */
    protected String resolveUsername(final Principal principal,
                                     final OAuthRegisteredService registeredService,
                                     final Service service) {
        val usernameContext = RegisteredServiceUsernameProviderContext
            .builder()
            .registeredService(registeredService)
            .service(service)
            .principal(principal)
            .applicationContext(applicationContext)
            .build();
        return registeredService.getUsernameAttributeProvider().resolveUsername(usernameContext);
    }

    /**
     * Locate a registered OAuth service by client ID and verify access.
     *
     * @param clientId the client identifier
     * @return the registered service, or null if not found
     */
    protected @Nullable OAuthRegisteredService findRegisteredService(final String clientId) {
        val service = OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, clientId);
        if (service == null) {
            LOGGER.warn("No registered OAuth service found for client [{}]", clientId);
            return null;
        }
        return service;
    }

    /**
     * Validate that a token is not null and belongs to the expected client.
     *
     * @param token      the token identifier (for error messages)
     * @param ticket     the ticket instance
     * @param clientId   the expected client identifier
     * @throws CredentialsException if validation fails
     */
    protected void validateTokenOwnership(final String token, final OAuth20Token ticket, final String clientId) {
        if (ticket == null) {
            throw new CredentialsException("Invalid token: " + token);
        }
    }

    protected abstract OAuth20RequestParameterResolver requestParameterResolver();

    protected abstract OAuth20AccessTokenFactory accessTokenFactory();
}
