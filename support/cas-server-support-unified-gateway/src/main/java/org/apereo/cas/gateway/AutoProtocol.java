package org.apereo.cas.gateway;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controllers or methods for automatic protocol adaptation
 * between OAuth2 and SAML2.
 * 
 * @since 7.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoProtocol {
    
    /**
     * Optional explicit protocol specification (e.g., "oauth2", "saml2").
     *
     * @return protocol name
     */
    String value() default "";
}
