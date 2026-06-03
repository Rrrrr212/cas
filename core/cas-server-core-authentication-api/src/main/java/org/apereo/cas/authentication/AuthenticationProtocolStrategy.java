package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;

public interface AuthenticationProtocolStrategy {

    boolean validateToken(String token, Service service) throws Throwable;

    Principal resolvePrincipal(String token, Service service) throws Throwable;

    Map<String, List<Object>> resolveAttributes(String token, Service service) throws Throwable;

    String getProtocolName();
}