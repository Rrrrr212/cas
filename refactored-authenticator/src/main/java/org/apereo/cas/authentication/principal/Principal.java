package org.apereo.cas.authentication.principal;

import java.io.Serializable;
import java.util.Map;

/**
 * Principal interface representing an authenticated entity.
 */
public interface Principal extends Serializable {

    String getId();

    Map<String, Object> getAttributes();
}
