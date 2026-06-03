package org.apereo.cas.support.unifiedgateway.protocol;

import module java.base;
import java.util.Arrays;
import org.apereo.cas.support.oauth.web.endpoints.OAuth20AuthorizeEndpointController;
import org.apereo.cas.support.saml.web.idp.profile.sso.SSOSamlIdPPostProfileHandlerController;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AutoProtocolRoutingAspect {
    private final AutoProtocolRequestResolver requestResolver;

    private final ObjectProvider<OAuth20AuthorizeEndpointController<?>> authorizeController;

    private final ObjectProvider<SSOSamlIdPPostProfileHandlerController> ssoPostProfileHandlerController;

    @Around("@annotation(org.apereo.cas.support.unifiedgateway.protocol.AutoProtocol)")
    public Object route(final ProceedingJoinPoint joinPoint) throws Throwable {
        val request = findArgument(joinPoint.getArgs(), HttpServletRequest.class);
        val response = findArgument(joinPoint.getArgs(), HttpServletResponse.class);
        if (request == null || response == null) {
            return joinPoint.proceed();
        }
        val routed = requestResolver.resolve(request)
            .map(protocol -> switch (protocol) {
                case OAUTH2 -> routeOAuthRequest(request, response);
                case SAML2 -> routeSamlRequest(request, response);
            })
            .orElse(null);
        return routed != null ? routed : joinPoint.proceed();
    }

    private @Nullable Object routeOAuthRequest(final HttpServletRequest request,
                                               final HttpServletResponse response) {
        val controller = authorizeController.getIfAvailable();
        if (controller == null) {
            return null;
        }
        return isPost(request)
            ? routeUnchecked(() -> controller.handleRequestPost(request, response))
            : routeUnchecked(() -> controller.handleRequest(request, response));
    }

    private @Nullable Object routeSamlRequest(final HttpServletRequest request,
                                              final HttpServletResponse response) {
        val controller = ssoPostProfileHandlerController.getIfAvailable();
        if (controller == null) {
            return null;
        }
        return isPost(request)
            ? controller.handleSaml2ProfileSsoPostRequest(response, request)
            : controller.handleSaml2ProfileSsoRedirectRequest(response, request);
    }

    private static boolean isPost(final HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod());
    }

    private static <T> @Nullable T findArgument(final Object[] args, final Class<T> type) {
        return Arrays.stream(args)
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst()
            .orElse(null);
    }

    private static Object routeUnchecked(final ThrowingSupplier supplier) {
        try {
            return supplier.get();
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }
}
