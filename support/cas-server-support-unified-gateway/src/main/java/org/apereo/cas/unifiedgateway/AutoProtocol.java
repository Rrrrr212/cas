package org.apereo.cas.unifiedgateway;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable automatic protocol detection and routing.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoProtocol {
    /**
     * Protocol types to support.
     *
     * @return the protocol types
     */
    ProtocolType[] value() default {ProtocolType.OAUTH2, ProtocolType.SAML2};

    enum ProtocolType {
        OAUTH2, SAML2
    }
}
