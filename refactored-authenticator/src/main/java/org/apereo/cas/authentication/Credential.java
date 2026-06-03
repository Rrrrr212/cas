package org.apereo.cas.authentication;

import java.io.Serializable;

/**
 * Basic credential interface.
 */
public interface Credential extends Serializable {

    String getId();
}
