package org.apereo.cas.authentication.principal;

import java.util.HashMap;
import java.util.Map;

/**
 * Default principal factory implementation.
 */
public class DefaultPrincipalFactory implements PrincipalFactory {

    @Override
    public Principal createPrincipal(String id) {
        return createPrincipal(id, new HashMap<>());
    }

    @Override
    public Principal createPrincipal(String id, Map<String, Object> attributes) {
        return new SimplePrincipal(id, attributes);
    }

    public static class SimplePrincipal implements Principal {
        private final String id;
        private final Map<String, Object> attributes;

        public SimplePrincipal(String id, Map<String, Object> attributes) {
            this.id = id;
            this.attributes = attributes;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
