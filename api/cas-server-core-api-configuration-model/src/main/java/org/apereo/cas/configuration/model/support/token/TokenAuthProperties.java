package org.apereo.cas.configuration.model.support.token;

import org.apereo.cas.configuration.support.RequiresModule;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Token authentication properties.
 */
@RequiresModule(name = "cas-server-support-token-auth")
@ConfigurationProperties(value = "cas.auth.token", ignoreUnknownFields = false)
@Getter
@Setter
@Accessors(chain = true)
public class TokenAuthProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 8116541604106596102L;

    /**
     * Name of the authentication handler.
     */
    private String name = "TokenAuthenticationHandler";

    /**
     * Order of the authentication handler.
     */
    private int order = 0;

    /**
     * Required scopes for token validation.
     */
    private Set<String> requiredScopes = new LinkedHashSet<>();
}
