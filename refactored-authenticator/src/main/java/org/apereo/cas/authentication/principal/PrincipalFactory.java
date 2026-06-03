package org.apereo.cas.authentication.principal;

import java.util.Map;

/**
 * Factory interface for creating principals.
 */
public interface PrincipalFactory {

    Principal createPrincipal(String id);

    Principal createPrincipal(String id, Map<String, Object> attributes);
}
