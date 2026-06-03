package org.apereo.cas.unified.gateway.annotation;

import module java.base;
import org.apereo.cas.unified.gateway.UnifiedGatewayConstants;
import org.apereo.cas.configuration.support.RequiresModule;
import org.jspecify.annotations.NullMarked;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a controller or handler for automatic protocol detection.
 * When applied, the request parameters are inspected to determine whether
 * OAuth2 or SAML2 processing should be used, and the request is routed accordingly.
 *
 * @author CAS Contributor
 * @since 7.4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@NullMarked
@RequiresModule(name = "cas-server-support-unified-gateway")
public @interface AutoProtocol {

    /**
     * The parameter name used to detect the protocol type.
     * Default is {@code protocol}.
     *
     * @return parameter name
     */
    String protocolParam() default UnifiedGatewayConstants.PARAM_PROTOCOL;

    /**
     * Whether to allow automatic fallback based on request parameters
     * when the protocol parameter is not explicitly set.
     *
     * @return true if auto-detection is enabled
     */
    boolean autoDetect() default true;
}
