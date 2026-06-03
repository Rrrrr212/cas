package org.apereo.cas.gateway.annotation;

import org.apereo.cas.gateway.ProtocolType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoProtocol {

    ProtocolType defaultProtocol() default ProtocolType.OAUTH2;

    String parameterName() default "protocol";

    boolean fallbackToDefault() default true;
}